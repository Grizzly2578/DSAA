
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Inventory inventory = new Inventory();
        OrderQueue orderQueue = new OrderQueue();
        Users userSystem = new Users();
        SalesManager salesManager = new SalesManager();

        inventory.addItem(new Beverage(101, "Espresso", 3.50, "Medium", true, 0));
        inventory.addItem(new Beverage(102, "Iced Latte", 4.75, "Large", false, 50));
        inventory.addItem(new Pastry(201, "Croissant", 3.25, true, true));
        inventory.addItem(new Pastry(202, "Blueberry Muffin", 3.80, false, true));

        System.out.println("==========================================");
        System.out.println("   WELCOME TO JAVA DSA CAFE SYSTEM");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. View Menu (Ordered Array)");
            System.out.println("2. Search Item by ID (Binary Search)");
            System.out.println("3. Place New Order (Enqueue - Online Alg)");
            System.out.println("4. Fulfill Next Order (Dequeue)");
            System.out.println("5. Register Member / Apply Voucher (Sets)");
            System.out.println("6. Run EOD Sales Report (Insertion Sort)");
            System.out.println("7. Calculate Change (Greedy Algorithm)");
            System.out.println("8. Exit");
            System.out.print("Choice: ");

            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    inventory.displayCatalog();
                    break;
                case 2:
                    System.out.print("Enter Item ID to search: ");
                    int searchId = scanner.nextInt();
                    MenuItem item = inventory.binarySearchById(searchId);
                    if (item != null) {
                        System.out.println("Found: " + item);
                    } else {
                        System.out.println("Item not found!");
                    }
                    break;
                case 3:
                    System.out.print("Enter Customer Name: ");
                    String name = scanner.next();
                    Order newOrder = new Order((int)(Math.random() * 9000) + 1000, name);
                    newOrder.addItem(inventory.binarySearchById(101)); // Example adding Espresso
                    orderQueue.enqueueOrder(newOrder);
                    break;
                case 4:
                    Order fulfilled = orderQueue.dequeueOrder();
                    if (fulfilled != null) {
                        salesManager.recordCompletedOrder(fulfilled);
                    }
                    break;
                case 5:
                    System.out.print("Enter Member ID to register: ");
                    String memberId = scanner.next();
                    userSystem.registerMember(memberId);
                    break;
                case 6:
                    salesManager.generateEODReportInsertionSort();
                    break;
                case 7:
                    System.out.print("Empty Choice ");

                    break;
                case 8:
                    running = false;
                    System.out.println("Thank you for using Java Cafe System!");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        scanner.close();
    }
}

