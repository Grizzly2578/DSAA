import java.io.IOException;
import java.util.InputMismatchException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    void run() {
        Scanner scanner = new Scanner(System.in);
        Inventory inventory = new Inventory();
        OrderQueue orderQueue = new OrderQueue();

        try {
            HTTPServer server = new HTTPServer(inventory, orderQueue);
            server.start();
        } catch (IOException e) {
            System.out.println("Failed to start HTTP server: " + e.getMessage());
        }

        System.out.println("==========================================");
        System.out.println("     WELCOME TO JAVA DSA CAFE SYSTEM      ");
        System.out.println("==========================================");

        boolean running = true;
        while (running) { // <-- "Go To Main Input" loop point
            printMenu();
            int choice = readChoice(scanner);

            switch (choice) {
                case 1 -> searchItemId(scanner, inventory);
                case 2 -> inventory.displayCatalog();
                case 3 -> addMenuItem(scanner, inventory);
                case 4 -> removeMenuItem(scanner, inventory);
                case 5 -> editMenuItem(scanner, inventory);
                case 6 -> displayQueue(orderQueue);
                case 7 -> placeOrder(scanner, inventory, orderQueue);
                case 8 -> fulfillOrder(inventory, orderQueue);
                case 9 -> {
                    System.out.println("Exiting... Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice.");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\nSelect an option:");
        System.out.println("1. Search Item by ID (Binary Search)");
        System.out.println("2. Display Menu (Ordered Array)");
        System.out.println("3. Add Drink or Pastry");
        System.out.println("4. Remove Menu Item");
        System.out.println("5. Edit Menu Item");
        System.out.println("6. Display Queue (Queue)");
        System.out.println("7. Build Shopping Cart and Place Order");
        System.out.println("8. Fulfill Next Order (Dequeue)");
        System.out.println("9. Exit");
        System.out.print("Choice: ");
    }

    private static void displayQueue(OrderQueue orderQueue){
        System.out.println(orderQueue.toString());
    }


    private static int readChoice(Scanner scanner) {
        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            return choice;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // discard the invalid token
            return -1; // falls into "Invalid choice."
        }
    }

    private static void searchItemId(Scanner scanner, Inventory inventory) {
        System.out.print("Enter Item ID to search: ");
        int id = readInt(scanner);
        if (inventory.itemExists(id)) {
            System.out.println("Item Exists: " + inventory.getItemById(id));
        } else {
            System.out.println("Item does not exist.");
        }
    }

    private static void addMenuItem(Scanner scanner, Inventory inventory) {
        System.out.print("Enter new Item ID: ");
        int id = readInt(scanner);

        if (inventory.itemExists(id)) {
            System.out.println("Item Already Exists.");
            return;
        }

        System.out.print("Enter item name: ");
        String name = scanner.nextLine();
        System.out.print("Enter item type (Drink/Pastry): ");
        String type = scanner.nextLine();

        MenuItem item;
        if (type.equalsIgnoreCase("Pastry")) {
            System.out.print("Enter pastry price: ");
            item = new Pastry(id, name, readDouble(scanner));
        } else {
            System.out.print("Enter Small price: ");
            double small = readDouble(scanner);
            System.out.print("Enter Medium price: ");
            double medium = readDouble(scanner);
            System.out.print("Enter Large price: ");
            item = new Drink(id, name, small, medium, readDouble(scanner));
        }

        inventory.addItem(item);
        System.out.println("Item created and added to the menu.");
    }

    private static void removeMenuItem(Scanner scanner, Inventory inventory) {
        System.out.print("Enter Item ID to remove: ");
        int id = readInt(scanner);

        if (inventory.removeItem(id)) {
            System.out.println("Item removed.");
        } else {
            System.out.println("Item does not exist.");
        }
    }

    private static void editMenuItem(Scanner scanner, Inventory inventory) {
        System.out.print("Enter Item ID to edit: ");
        int id = readInt(scanner);

        if (!inventory.itemExists(id)) {
            System.out.println("Item Not Found.");
            return;
        }

        System.out.print("Enter new name: ");
        String name = scanner.nextLine();
        System.out.print("Enter new price: ");
        double price = readDouble(scanner);

        inventory.editItem(id, name, price);
        System.out.println("Item updated.");
    }

    private static void placeOrder(Scanner scanner, Inventory inventory, OrderQueue orderQueue) {
        List<CartItem> cart = new ArrayList<>();
        String addAnother = "y";
        do {
            System.out.print("Enter Item ID to add to cart: ");
            int id = readInt(scanner);
            MenuItem item = inventory.getItemById(id);
            if (item == null) {
                System.out.println("Item Not Found.");
                continue;
            }
            Size size = chooseSize(scanner, item);
            System.out.print("Enter quantity: ");
            int quantity = readInt(scanner);
            if (quantity > 0) cart.add(new CartItem(id, size, quantity));
            System.out.print("Add another item? (y/n): ");
            addAnother = scanner.nextLine();
            System.out.printf("Current cart total: ₱%.2f%n", inventory.calculateCartTotal(cart));
        } while (addAnother.equalsIgnoreCase("y"));

        if (cart.isEmpty()) return;
        System.out.print("Enter Customer Alias: ");
        String customerAlias = readStr(scanner);

        double totalPrice = inventory.calculateCartTotal(cart);
        orderQueue.enqueue(new Order(cart, customerAlias, totalPrice));
        System.out.printf("Order placed and added to the Order Queue. Total: ₱%.2f%n", totalPrice);
    }

    private static Size chooseSize(Scanner scanner, MenuItem item) {
        if (item instanceof Pastry) return Size.STANDARD;
        System.out.print("Choose size (Small/Medium/Large): ");
        try {
            return Size.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid size; using Medium.");
            return Size.MEDIUM;
        }
    }

    private static void fulfillOrder(Inventory inventory, OrderQueue orderQueue) {
        if (orderQueue.isEmpty()) {
            System.out.println("No pending orders.");
            return;
        }

        CompletedSale sale = orderQueue.fulfillNext("CLI");
        Order order = sale.order();
        System.out.printf("Fulfilled order for %s (Total: ₱%.2f):%n", order.customerAlias(), order.totalPrice());
        for (CartItem cartItem : order.items()) {
            MenuItem item = inventory.getItemById(cartItem.itemId());
            String itemName = (item != null) ? item.getName() : "Unknown Item";
            System.out.printf("%d x %s (%s)%n", cartItem.quantity(), itemName, cartItem.size());
        }
    }

    private static int readInt(Scanner scanner) {
        try {
            int value = scanner.nextInt();
            scanner.nextLine();
            return value;
        } catch (InputMismatchException e) {
            scanner.nextLine();
            System.out.println("Invalid number, defaulting to -1.");
            return -1;
        }
    }

    private static String readStr(Scanner scanner){
        return scanner.nextLine();
    }

    private static double readDouble(Scanner scanner) {
        try {
            double value = scanner.nextDouble();
            scanner.nextLine();
            return value;
        } catch (InputMismatchException e) {
            scanner.nextLine();
            System.out.println("Invalid number, defaulting to 0.0.");
            return 0.0;
        }
    }
}
