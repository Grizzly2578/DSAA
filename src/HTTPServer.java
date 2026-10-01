import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPServer {

    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";
    private static final int MAX_REQUEST_BYTES = 32 * 1024;
    private static final int MAX_STATIC_FILE_BYTES = 5 * 1024 * 1024;
    private static final long SESSION_TTL_MILLIS = 30 * 60 * 1000L;
    private static final long LOGIN_WINDOW_MILLIS = 60 * 1000L;
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // References to system core classes
    private final Inventory inventory;
    private final OrderQueue orderQueue;

    // AUTH - User Sessions
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private record Session(String username, String role, long expiresAt) {
    }

    private static final class LoginAttempt {
        private long windowStartedAt;
        private int count;
    }

    public HTTPServer(Inventory inventory, OrderQueue orderQueue) {
        this.inventory = inventory;
        this.orderQueue = orderQueue;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // 1. Static Content Handler (Serves HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        // 2. REST API Handlers
        server.createContext("/api/login", new LoginApiHandler());
        server.createContext("/api/logout", new LogoutApiHandler());
        server.createContext("/api/menu", new MenuApiHandler());
        server.createContext("/api/orders", new OrdersApiHandler());
        server.createContext("/api/summary", new SummaryApiHandler());

        // Multithreaded executor for handling concurrent requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Cafe Server running at: http://localhost:" + PORT + "/");
    }
    // ==========================================
    // AUTH HANDLER
    // ==========================================

    private Session getSession(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            Session session = sessions.get(token);
            if (session == null) return null;
            if (session.expiresAt() <= System.currentTimeMillis()) {
                sessions.remove(token, session);
                return null;
            }
            return session;
        }
        return null;
    }

    private String getRole(HttpExchange exchange) {
        Session session = getSession(exchange);
        return session == null ? null : session.role();
    }

    private boolean isAuthorized(HttpExchange exchange, String... allowedRoles) throws IOException {
        String role = getRole(exchange);
        if (role == null) {
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized. Please log in.\"}", "application/json");
            return true;
        }
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) return false;
        }
        sendResponse(exchange, 403, "{\"error\":\"Forbidden. Insufficient permissions.\"}", "application/json");
        return true;
    }

    private class LoginApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String client = exchange.getRemoteAddress().getAddress().getHostAddress();
                if (isLoginRateLimited(client)) {
                    sendResponse(exchange, 429, "{\"error\":\"Too many login attempts\"}", "application/json");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    String role = getRole(body);

                    if (role != null) {
                        loginAttempts.remove(client);
                        String token = UUID.randomUUID().toString();
                        String username = extractString(body, "username");
                        sessions.put(token, new Session(username, role, System.currentTimeMillis() + SESSION_TTL_MILLIS));
                        sendResponse(exchange, 200, "{\"token\":\"" + token + "\", \"role\":\"" + role + "\"}", "application/json");
                    } else {
                        sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Malformed JSON\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
            }
        }

        private static String getRole(String body) {
            String username = body.replaceAll("(?s).*\"username\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            String password = body.replaceAll("(?s).*\"password\"\\s*:\\s*\"([^\"]+)\".*", "$1");


            String role = null;
//            if ("admin".equals(username) && passwordMatches("CAFE_ADMIN_PASSWORD", password)) role = "MANAGER";
//            else if ("barista".equals(username) && passwordMatches("CAFE_BARISTA_PASSWORD", password)) role = "BARISTA";

//            Hard Coded Implementation of Login Credentials for Demonstration Purposes
            if ("admin".equals(username) && password.equals("admin123")) role = "MANAGER";
            else if ("barista".equals(username) && password.equals("coffee123")) role = "BARISTA";
            return role;
        }

        private static boolean passwordMatches(String environmentVariable, String password) {
            String configuredPassword = System.getenv(environmentVariable);
            return configuredPassword != null && configuredPassword.equals(password);
        }
    }

    private class LogoutApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
                return;
            }
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                sessions.remove(authorization.substring(7).trim());
            }
            sendResponse(exchange, 204, "", "application/json");
            }
    }

    // ==========================================
    // STATIC FILE HANDLER
    // ==========================================
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Default root to index.html
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Strips leading slash to avoid root path resolution issues on Windows
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            Path rootPath = Paths.get(WEB_ROOT).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(path).normalize();

            if (!filePath.startsWith(rootPath) || !Files.exists(filePath)) {
                sendResponse(exchange, 403, "Access Denied", "text/plain");
                return;
            }

            Path realRoot = rootPath.toRealPath();
            Path realFile = filePath.toRealPath();
            if (!realFile.startsWith(realRoot) || Files.isDirectory(realFile)) {
                sendResponse(exchange, 403, "Access Denied", "text/plain");
                return;
            }
            if (Files.size(realFile) > MAX_STATIC_FILE_BYTES) {
                sendResponse(exchange, 413, "File too large", "text/plain");
                return;
            }

            String contentType = determineContentType(realFile.toString());
            byte[] bytes = Files.readAllBytes(realFile);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String determineContentType(String path) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    // ==========================================
    // API: /api/menu
    // ==========================================
    private class MenuApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    try {
                        int searchId = Integer.parseInt(query.substring(3));
                        MenuItem item = inventory.getItemById(searchId); // Uses Inventory's Binary Search

                        if (item != null) {
                                sendResponse(exchange, 200, "[" + itemToJson(item) + "]", "application/json");
                        } else {
                            sendResponse(exchange, 200, "[]", "application/json"); // Return empty array if not found
                        }
                    } catch (NumberFormatException e) {
                        sendResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}", "application/json");
                    }
                } else {
                    // Determine which list to serialize (Sorted vs Default)
                    java.util.List<MenuItem> itemsToSerialize = inventory.getItemsSnapshot(); // Default (Ordered by ID)

                    if (query != null && query.contains("sortBy=price")) {
                        boolean ascending = !query.contains("desc=true");
                        itemsToSerialize = inventory.getMenuSortedByPrice(ascending); // Trigger Merge Sort
                    }

                    // Construct JSON representation
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < itemsToSerialize.size(); i++) {
                        MenuItem item = itemsToSerialize.get(i);
                        json.append(itemToJson(item));
                        if (i < itemsToSerialize.size() - 1) json.append(",");
                    }
                    json.append("]");

                    sendResponse(exchange, 200, json.toString(), "application/json");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER")) return;

                String body = readRequestBody(exchange);
                try {
                        int id = Integer.parseInt(extractString(body, "id"));
                        String name = extractString(body, "name");
                        String type = extractString(body, "type");
                        Map<Size, Double> prices = parsePrices(body, type);

                        MenuItem item = "Pastry".equalsIgnoreCase(type)
                            ? new Pastry(id, name, prices.get(Size.STANDARD))
                            : new Drink(id, name, prices.get(Size.SMALL), prices.get(Size.MEDIUM), prices.get(Size.LARGE));
                        if (inventory.addItem(item)) {
                        sendResponse(exchange, 201, "{\"status\":\"Item added\"}", "application/json");
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Item ID already exists\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid item data format\"}", "application/json");
                }

            } else if ("PUT".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER")) return;

                String body = readRequestBody(exchange);
                try {
                    int id = Integer.parseInt(extractString(body, "id"));
                    String name = extractString(body, "name");
                    Map<Size, Double> prices = parsePrices(body, extractString(body, "type"));

                    if (inventory.editItem(id, name, prices)) {
                        sendResponse(exchange, 200, "{\"status\":\"Item updated\"}", "application/json");
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Item ID not found\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid item data format\"}", "application/json");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER")) return;

                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    try {
                        int id = Integer.parseInt(query.substring(3));
                        if (inventory.removeItem(id)) {
                            sendResponse(exchange, 200, "{\"status\":\"Item removed\"}", "application/json");
                        } else {
                            sendResponse(exchange, 404, "{\"error\":\"Item not found\"}", "application/json");
                        }
                    } catch (NumberFormatException e) {
                        sendResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}", "application/json");
                    }
                } else {
                    sendResponse(exchange, 400, "{\"error\":\"Missing id query parameter\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
            }
        }
    }

    // ==========================================
    // API: /api/orders
    // ==========================================
    private class OrdersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER", "BARISTA")) return;

                String jsonResponse = orderQueue.toJson();
                sendResponse(exchange, 200, jsonResponse, "application/json");

            } else if ("POST".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER", "BARISTA")) return;

                String body = readRequestBody(exchange);

                try {
                    List<CartItem> cartItems = parseCartItems(body);
                    String customerAlias = "Guest";
                    if (body.contains("\"customerAlias\"")) {
                        customerAlias = extractString(body, "customerAlias");
                    }
                    if (customerAlias.length() > 100) {
                        throw new IllegalArgumentException("Customer alias is too long");
                    }

                    for (CartItem cartItem : cartItems) {
                        MenuItem menuItem = inventory.getItemById(cartItem.itemId());
                        if (menuItem == null || !menuItem.supportsSize(cartItem.size())) {
                            sendResponse(exchange, 404, "{\"error\":\"Item or size not found\"}", "application/json");
                            return;
                        }
                    }
                    double totalPrice = inventory.calculateCartTotal(cartItems);
                    orderQueue.enqueue(new Order(cartItems, customerAlias, totalPrice));
                    sendResponse(exchange, 201, "{\"status\":\"Order queued\"}", "application/json");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Malformed JSON request\"}", "application/json");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER", "BARISTA")) return;

                Session session = getSession(exchange);
                CompletedSale sale = orderQueue.fulfillNext(session.username());
                if (sale == null) {
                    sendResponse(exchange, 400, "{\"error\":\"No pending orders to fulfill\"}", "application/json");
                } else {
                    sendResponse(exchange, 200, "{\"status\":\"Order fulfilled\",\"sale\":"
                            + saleToJson(sale) + "}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
            }
        }
    }

    private class SummaryApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
                return;
            }
            if (isAuthorized(exchange, "MANAGER")) return;

            List<CompletedSale> sales = orderQueue.getCompletedSalesSnapshot();
            StringBuilder json = new StringBuilder(String.format(
                    "{\"metrics\":{\"menuItems\":%d,\"pendingOrders\":%d,\"completedSales\":%d,\"itemsSold\":%d,\"totalRevenue\":%.2f},\"sales\":[",
                    inventory.size(), orderQueue.size(), sales.size(), orderQueue.getCompletedItemCount(), orderQueue.getTotalRevenue()));
            for (int i = 0; i < sales.size(); i++) {
                json.append(saleToJson(sales.get(i)));
                if (i < sales.size() - 1) json.append(",");
            }
            json.append("]}");
            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    private static void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if ("http://localhost:8080".equals(origin) || "http://127.0.0.1:8080".equals(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private boolean isLoginRateLimited(String client) {
        long now = System.currentTimeMillis();
        LoginAttempt attempt = loginAttempts.computeIfAbsent(client, ignored -> new LoginAttempt());
        synchronized (attempt) {
            if (now - attempt.windowStartedAt >= LOGIN_WINDOW_MILLIS) {
                attempt.windowStartedAt = now;
                attempt.count = 0;
            }
            attempt.count++;
            return attempt.count > MAX_LOGIN_ATTEMPTS;
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null && Long.parseLong(contentLength) > MAX_REQUEST_BYTES) {
            throw new IOException("Request body too large");
        }

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = exchange.getRequestBody().read(buffer)) != -1) {
            total += read;
            if (total > MAX_REQUEST_BYTES) throw new IOException("Request body too large");
            body.write(buffer, 0, read);
        }
        return body.toString(StandardCharsets.UTF_8);
    }

    private static String itemToJson(MenuItem item) {
        StringBuilder json = new StringBuilder(String.format("{\"id\":%d,\"name\":\"%s\",\"type\":\"%s\",\"prices\":{",
                item.getId(), escapeJson(item.getName()), item.getType()));
        int index = 0;
        for (Map.Entry<Size, Double> price : item.getPrices().entrySet()) {
            json.append(String.format("\"%s\":%.2f", price.getKey(), price.getValue()));
            if (++index < item.getPrices().size()) json.append(",");
        }
        return json.append("}}").toString();
    }

    private static String orderItemsToJson(Order order) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < order.items().size(); i++) {
            CartItem item = order.items().get(i);
            json.append(String.format("{\"itemId\":%d,\"size\":\"%s\",\"quantity\":%d}",
                    item.itemId(), item.size(), item.quantity()));
            if (i < order.items().size() - 1) json.append(",");
        }
        return json.append("]").toString();
    }

    private static String saleToJson(CompletedSale sale) {
        Order order = sale.order();
        return String.format("{\"completedAt\":\"%s\",\"completedBy\":\"%s\",\"customerAlias\":\"%s\",\"totalPrice\":%.2f,\"items\":%s}",
                sale.completedAt(), escapeJson(sale.completedBy()), escapeJson(order.customerAlias()),
                order.totalPrice(), orderItemsToJson(order));
    }

    private static List<CartItem> parseCartItems(String body) {
        List<CartItem> items = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{\\s*\\\"itemId\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"size\\\"\\s*:\\s*\\\"([A-Za-z]+)\\\"\\s*,\\s*\\\"quantity\\\"\\s*:\\s*(\\d+)\\s*\\}").matcher(body);
        while (matcher.find()) {
            if (items.size() >= 50) throw new IllegalArgumentException("Too many cart items");
            items.add(new CartItem(Integer.parseInt(matcher.group(1)),
                    Size.valueOf(matcher.group(2).toUpperCase()), Integer.parseInt(matcher.group(3))));
        }
        if (items.isEmpty()) throw new IllegalArgumentException("No cart items");
        return items;
    }

    private static Map<Size, Double> parsePrices(String body, String type) {
        HashMap<Size, Double> prices = new HashMap<>();
        if ("Pastry".equalsIgnoreCase(type)) {
            prices.put(Size.STANDARD, Double.parseDouble(extractString(body, "price")));
        } else {
            prices.put(Size.SMALL, Double.parseDouble(extractString(body, "smallPrice")));
            prices.put(Size.MEDIUM, Double.parseDouble(extractString(body, "mediumPrice")));
            prices.put(Size.LARGE, Double.parseDouble(extractString(body, "largePrice")));
        }
        return prices;
    }

    private static String extractString(String body, String key) {
        Matcher matcher = Pattern.compile("(?s).*\\\"" + key + "\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([-+]?[0-9]*\\.?[0-9]+)).*").matcher(body);
        if (!matcher.matches()) throw new IllegalArgumentException("Missing " + key);
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}