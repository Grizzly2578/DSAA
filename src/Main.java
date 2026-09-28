import java.io.IOException;
import java.util.InputMismatchException;
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
        IO.println("3. Add Menu Item");
        IO.println("4. Remove Menu Item");
        IO.println("5. Edit Menu Item");
        IO.println("6. Display Queue (Queue)");
        IO.println("7. Place Order (Enqueue)");
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
        IO.print("Enter item price: ");
        double price = readDouble(scanner);

        inventory.addItem(new MenuItem(id, name, price));
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
        IO.print("Enter Item ID to order: ");
        int id = readInt(scanner);

        if (!inventory.itemExists(id)) {
            IO.println("Item Not Found.");
            return;
        }

        IO.print("Enter Order Quantity: ");
        int quantity = readInt(scanner);

        IO.print("Enter Customer Alias: ");
        String customerAlias = readStr(scanner);

        orderQueue.enqueue(new Order(id, quantity, customerAlias));
        IO.println("Order placed and added to the Order Queue.");
    }

    private static void fulfillOrder(Inventory inventory, OrderQueue orderQueue) {
        if (orderQueue.isEmpty()) {
            IO.println("No pending orders.");
            return;
        }

        Order order = orderQueue.dequeue();
        MenuItem item = inventory.getItemById(order.itemId());
        String itemName = (item != null) ? item.getName() : "Unknown Item";
        System.out.printf("Fulfilled order: %d x %s (ID: %d)%n",
                order.quantity(), itemName, order.itemId());
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
