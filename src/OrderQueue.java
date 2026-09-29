import java.util.ArrayDeque;
import java.util.Queue;

/**
 * FIFO order queue (Thread-safe for HTTP server & CLI).
 */
public class OrderQueue {

    // ArrayDeque is generally more memory-efficient than LinkedList for queues
    private final Queue<Order> queue = new ArrayDeque<>();

    public synchronized void enqueue(Order order) {
        queue.add(order);
    }

    public synchronized Order dequeue() {
        return queue.poll();
    }

    public synchronized Order peek() {
        return queue.peek();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int size() {
        return queue.size();
    }

    /**
     * Serializes the current pending orders to a JSON array string.
     * Can be directly returned by the HTTPServer GET /api/orders endpoint.
     */
    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        int total = queue.size();

        for (Order order : queue) {
            sb.append("{\"items\":[");
            for (int i = 0; i < order.items().size(); i++) {
                CartItem item = order.items().get(i);
                sb.append(String.format("{\"itemId\":%d,\"size\":\"%s\",\"quantity\":%d}",
                        item.itemId(), item.size(), item.quantity()));
                if (i < order.items().size() - 1) sb.append(",");
            }
            sb.append(String.format("],\"customerAlias\":\"%s\",\"totalPrice\":%.2f}",
                    escapeJson(order.customerAlias()), order.totalPrice()));
            if (++count < total) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public synchronized String toString() {
        if (queue.isEmpty()) {
            return "Order Queue is empty.";
        }
        StringBuilder sb = new StringBuilder("Current Queue:\n");
        int index = 1;
        for (Order order : queue) {
            sb.append(index++).append(". ");
            for (CartItem item : order.items()) {
                sb.append(item.quantity()).append(" x item ").append(item.itemId())
                        .append(" (").append(item.size()).append(") | ");
            }
            sb.append("Alias: ").append(order.customerAlias()).append("\n");
        }
        return sb.toString().trim();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}