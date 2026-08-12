
import java.util.LinkedList;
import java.util.Queue;

/**
 * Real-time Order Processing Queue.
 * DSA Concepts Utilized:
 * - Queue Data Structure (FIFO - First-In-First-Out)
 * - Online Algorithm paradigm (Processing sequential stream of orders without future knowledge)
 */
public class OrderQueue {
    private Queue<Order> pendingOrders;

    public OrderQueue() {
        this.pendingOrders = new LinkedList<>();
    }

    /**
     * Enqueue Operation: Adds a new order to the back of the queue.
     */
    public void enqueueOrder(Order order) {
        pendingOrders.add(order);
        System.out.println("-> Order #" + order.getOrderId() + " added to the queue.");
    }

    /**
     * Dequeue Operation: Barista processes and removes the front order.
     */
    public Order dequeueOrder() {
        if (pendingOrders.isEmpty()) {
            System.out.println("No pending orders to process.");
            return null;
        }
        Order processedOrder = pendingOrders.poll();
        processedOrder.setProcessed(true);
        System.out.println("<- Barista fulfilled Order #" + processedOrder.getOrderId());
        return processedOrder;
    }

    public void displayPendingOrders() {
        System.out.println("=== PENDING ORDERS QUEUE ===");
        if (pendingOrders.isEmpty()) {
            System.out.println("Queue is currently empty.");
            return;
        }
        for (Order order : pendingOrders) {
            System.out.println(order);
        }
    }
}

