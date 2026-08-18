import java.util.ArrayList;
import java.util.List;

/**
 * Represents an individual customer order.
 */
public class Order {
    private int orderId;
    private String customerName;
    private List<MenuItem> items;
    private double totalAmount;
    private boolean isProcessed;

    public Order(int orderId, String customerName) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.isProcessed = false;
    }

    public void addItem(MenuItem item) {
        items.add(item);
        totalAmount += item.getPrice();
    }

    public int getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public List<MenuItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public boolean isProcessed() { return isProcessed; }
    public void setProcessed(boolean processed) { isProcessed = processed; }

    @Override
    public String toString() {
        return String.format("Order #%d | Customer: %s | Items: %d | Total: $%.2f",
                orderId, customerName, items.size(), totalAmount);
    }
}

