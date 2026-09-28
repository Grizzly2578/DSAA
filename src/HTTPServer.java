import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class HTTPServer {

    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";

    // References to system core classes
    private final Inventory inventory;
    private final OrderQueue orderQueue;

    // AUTH - User Sessions
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public HTTPServer(Inventory inventory, OrderQueue orderQueue) {
        this.inventory = inventory;
        this.orderQueue = orderQueue;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 1. Static Content Handler (Serves HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        // 2. REST API Handlers
        server.createContext("/api/login", new LoginApiHandler());
        server.createContext("/api/menu", new MenuApiHandler());
        server.createContext("/api/orders", new OrdersApiHandler());

        // Multithreaded executor for handling concurrent requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Cafe Server running at: http://localhost:" + PORT + "/");
    }
    // ==========================================
    // AUTH HANDLER
    // ==========================================

    private String getRole(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return sessions.get(authHeader.substring(7));
        }
        return null;
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
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    String role = getRole(body);

                    if (role != null) {
                        String token = UUID.randomUUID().toString();
                        sessions.put(token, role);
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


            // Hard coded login credentials for demo purposes
            String role = null;
            if ("admin".equals(username) && "admin1234".equals(password)) role = "MANAGER";
            else if ("barista".equals(username) && "coffee1234".equals(password)) role = "BARISTA";
            return role;
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

            Path filePath = Paths.get(WEB_ROOT, path);

            // Security check: prevent directory traversal (e.g., ../)
            Path _path = Paths.get(WEB_ROOT);

            if (!filePath.normalize().startsWith(_path.toAbsolutePath().normalize())
                    && !filePath.toAbsolutePath().normalize().startsWith(_path.toAbsolutePath().normalize())) {
                sendResponse(exchange, 403, "Access Denied", "text/plain");
                return;
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = determineContentType(filePath.toString());
                byte[] bytes = Files.readAllBytes(filePath);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                sendResponse(exchange, 404, "404 Not Found: " + path, "text/plain");
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
                            String json = String.format("[{\"id\":%d,\"name\":\"%s\",\"price\":%.2f}]",
                                    item.getId(), item.getName(), item.getPrice());
                            sendResponse(exchange, 200, json, "application/json");
                        } else {
                            sendResponse(exchange, 200, "[]", "application/json"); // Return empty array if not found
                        }
                    } catch (NumberFormatException e) {
                        sendResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}", "application/json");
                    }
                } else {
                    // Construct JSON representation of menu items from inventory
                    StringBuilder json = new StringBuilder("[");

                    for (int i = 0; i < inventory.size(); i++) {
                        MenuItem item = inventory.get(i);
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"price\":%.2f}",
                                item.getId(), item.getName(), item.getPrice()));
                        if (i < inventory.size() - 1) json.append(",");
                    }

                    json.append("]");

                    sendResponse(exchange, 200, json.toString(), "application/json");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                if (!isAuthorized(exchange, "MANAGER")) return;

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                try {
                    int id = Integer.parseInt(body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
                    String name = body.replaceAll("(?s).*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    double price = Double.parseDouble(body.replaceAll("(?s).*\"price\"\\s*:\\s*([\\d\\.]+).*", "$1"));

                    if (inventory.addItem(new MenuItem(id, name, price))) {
                        sendResponse(exchange, 201, "{\"status\":\"Item added\"}", "application/json");
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Item ID already exists\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid item data format\"}", "application/json");
                }

            } else if ("PUT".equalsIgnoreCase(method)) {
                if (!isAuthorized(exchange, "MANAGER")) return;

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                try {
                    int id = Integer.parseInt(body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
                    String name = body.replaceAll("(?s).*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    double price = Double.parseDouble(body.replaceAll("(?s).*\"price\"\\s*:\\s*([\\d\\.]+).*", "$1"));

                    if (inventory.editItem(id, name, price)) {
                        sendResponse(exchange, 200, "{\"status\":\"Item updated\"}", "application/json");
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Item ID not found\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid item data format\"}", "application/json");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (!isAuthorized(exchange, "MANAGER")) return;

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

                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                try {
                    int itemId = Integer.parseInt(body.replaceAll(".*\"itemId\"\\s*:\\s*(\\d+).*", "$1"));
                    int quantity = Integer.parseInt(body.replaceAll(".*\"quantity\"\\s*:\\s*(\\d+).*", "$1"));

                    String customerAlias = "Guest";
                    if (body.contains("\"customerAlias\"")) {
                        customerAlias = body.replaceAll(".*\"customerAlias\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    }

                    if (inventory.itemExists(itemId)) {
                        orderQueue.enqueue(new Order(itemId, quantity, customerAlias));
                        sendResponse(exchange, 201, "{\"status\":\"Order queued\"}", "application/json");
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Item ID not found\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Malformed JSON request\"}", "application/json");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (isAuthorized(exchange, "MANAGER", "BARISTA")) return;

                if (orderQueue.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"No pending orders to fulfill\"}", "application/json");
                } else {
                    Order fulfilled = orderQueue.dequeue();
                    sendResponse(exchange, 200, "{\"status\":\"Order fulfilled\", \"itemId\": " + fulfilled.itemId() + "}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
            }
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}