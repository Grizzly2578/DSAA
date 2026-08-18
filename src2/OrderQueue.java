import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Type;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Real-time Order Processing Queue.
 *
 * DSA Concepts Utilized:
 * - Queue Data Structure (FIFO - First-In-First-Out)
 * - Online Algorithm paradigm
 * - JSON Persistence
 */
public class OrderQueue {

    private static final String FILE_NAME = "json_files/orders.json";

    private static final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private Queue<Order> pendingOrders;

    public OrderQueue() { load(); }

    /**
     * Enqueue Operation:
     * Adds a new order to the back of the queue.
     */
    public void enqueueOrder(Order order) {
        pendingOrders.add(order);

        // Save the new state of the queue
        save();

        System.out.println(
            "-> Order #" + order.getOrderId() + " added to the queue."
        );
    }

    /**
     * Dequeue Operation:
     * Barista processes and removes the front order.
     */
    public Order dequeueOrder() {

        if (pendingOrders.isEmpty()) {
            System.out.println("No pending orders to process.");
            return null;
        }

        Order processedOrder = pendingOrders.poll();

        processedOrder.setProcessed(true);

        // Save the new state after removing the order
        save();

        System.out.println(
            "<- Barista fulfilled Order #"
            + processedOrder.getOrderId()
        );

        return processedOrder;
    }

    /**
     * Displays all currently pending orders.
     */
    public void displayPendingOrders() {

        System.out.println("=== PENDING ORDERS QUEUE ===");

        if (pendingOrders.isEmpty()) {
            System.out.println("Queue is currently empty.");
            return;
        }

        for (Order order : pendingOrders) System.out.println(order);
    }

    /**
     * Saves the queue to orders.json.
     */
    private void save() {

        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            // Convert Queue -> List for JSON
            List<Order> orders = new LinkedList<>(pendingOrders);
            gson.toJson(orders, writer);

        } catch (IOException e) {
            System.err.println("Failed to save orders.");
            e.printStackTrace();
        }
    }

    /**
     * Loads the queue from orders.json.
     */
    private void load() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            pendingOrders = new LinkedList<>();
            return;
        }

        try (FileReader reader = new FileReader(file)) {

            Type listType = new TypeToken<List<Order>>() {}.getType();

            List<Order> orders = gson.fromJson(reader, listType);

            if (orders == null) pendingOrders = new LinkedList<>();
            else pendingOrders = new LinkedList<>(orders);

        } catch (IOException e) {

            System.err.println("Failed to load orders.");
            e.printStackTrace();

            pendingOrders = new LinkedList<>();
        }
    }

    /**
     * Manually save the queue.
     */
    public void saveOrders() { save(); }
}
