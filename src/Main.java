import java.io.IOException;
import java.util.InputMismatchException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static void main() {
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
            IO.println("Failed to start HTTP server: " + e.getMessage());
        }

        IO.println("==========================================");
        IO.println("     WELCOME TO JAVA DSA CAFE SYSTEM      ");
        IO.println("==========================================");

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
                    IO.println("Exiting... Goodbye!");
                    running = false;
                }
                default -> IO.println("Invalid choice.");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        IO.println("\nSelect an option:");
        IO.println("1. Search Item by ID (Binary Search)");
        IO.println("2. Display Menu (Ordered Array)");
        IO.println("3. Add Drink or Pastry");
        IO.println("4. Remove Menu Item");
        IO.println("5. Edit Menu Item");
        IO.println("6. Display Queue (Queue)");
        IO.println("7. Build Shopping Cart and Place Order");
        IO.println("8. Fulfill Next Order (Dequeue)");
        IO.println("9. Exit");
        IO.print("Choice: ");
    }

    private static void displayQueue(OrderQueue orderQueue){
        IO.println(orderQueue.toString());
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
        IO.print("Enter Item ID to search: ");
        int id = readInt(scanner);
        if (inventory.itemExists(id)) {
            IO.println("Item Exists: " + inventory.getItemById(id));
        } else {
            IO.println("Item does not exist.");
        }
    }

    private static void addMenuItem(Scanner scanner, Inventory inventory) {
        IO.print("Enter new Item ID: ");
        int id = readInt(scanner);

        if (inventory.itemExists(id)) {
            IO.println("Item Already Exists.");
            return;
        }

        IO.print("Enter item name: ");
        String name = scanner.nextLine();
        IO.print("Enter item type (Drink/Pastry): ");
        String type = scanner.nextLine();

        MenuItem item;
        if (type.equalsIgnoreCase("Pastry")) {
            IO.print("Enter pastry price: ");
            item = new Pastry(id, name, readDouble(scanner));
        } else {
            IO.print("Enter Small price: ");
            double small = readDouble(scanner);
            IO.print("Enter Medium price: ");
            double medium = readDouble(scanner);
            IO.print("Enter Large price: ");
            item = new Drink(id, name, small, medium, readDouble(scanner));
        }

        inventory.addItem(item);
        IO.println("Item created and added to the menu.");
    }

    private static void removeMenuItem(Scanner scanner, Inventory inventory) {
        IO.print("Enter Item ID to remove: ");
        int id = readInt(scanner);

        if (inventory.removeItem(id)) {
            IO.println("Item removed.");
        } else {
            IO.println("Item does not exist.");
        }
    }

    private static void editMenuItem(Scanner scanner, Inventory inventory) {
        IO.print("Enter Item ID to edit: ");
        int id = readInt(scanner);

        if (!inventory.itemExists(id)) {
            IO.println("Item Not Found.");
            return;
        }

        IO.print("Enter new name: ");
        String name = scanner.nextLine();
        IO.print("Enter new price: ");
        double price = readDouble(scanner);

        inventory.editItem(id, name, price);
        IO.println("Item updated.");
    }

    private static void placeOrder(Scanner scanner, Inventory inventory, OrderQueue orderQueue) {
        List<CartItem> cart = new ArrayList<>();
        String addAnother = "y";
        do {
            IO.print("Enter Item ID to add to cart: ");
            int id = readInt(scanner);
            MenuItem item = inventory.getItemById(id);
            if (item == null) {
                IO.println("Item Not Found.");
                continue;
            }
            Size size = chooseSize(scanner, item);
            IO.print("Enter quantity: ");
            int quantity = readInt(scanner);
            if (quantity > 0) cart.add(new CartItem(id, size, quantity));
            IO.print("Add another item? (y/n): ");
            addAnother = scanner.nextLine();
            System.out.printf("Current cart total: ₱%.2f%n", inventory.calculateCartTotal(cart));
        } while (addAnother.equalsIgnoreCase("y"));

        if (cart.isEmpty()) return;
        IO.print("Enter Customer Alias: ");
        String customerAlias = readStr(scanner);

        double totalPrice = inventory.calculateCartTotal(cart);
        orderQueue.enqueue(new Order(cart, customerAlias, totalPrice));
        System.out.printf("Order placed and added to the Order Queue. Total: ₱%.2f%n", totalPrice);
    }

    private static Size chooseSize(Scanner scanner, MenuItem item) {
        if (item instanceof Pastry) return Size.STANDARD;
        IO.print("Choose size (Small/Medium/Large): ");
        try {
            return Size.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            IO.println("Invalid size; using Medium.");
            return Size.MEDIUM;
        }
    }

    private static void fulfillOrder(Inventory inventory, OrderQueue orderQueue) {
        if (orderQueue.isEmpty()) {
            IO.println("No pending orders.");
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
            IO.println("Invalid number, defaulting to -1.");
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
            IO.println("Invalid number, defaulting to 0.0.");
            return 0.0;
        }
    }
}
