/**
 * Base Class for all items in the Cafe Menu.
 * Demonstrates Object-Oriented Programming (OOP) foundation for Data Structures.
 */
public abstract class MenuItem implements Comparable<MenuItem> {
    private int itemId;
    private String name;
    private double price;
    private String category;

    public MenuItem(int itemId, String name, double price, String category) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    // Getters and Setters
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    /**
     * Comparable implementation to support Ordered Arrays & Binary Search.
     * Items are compared strictly by itemId (or price).
     */
    @Override
    public int compareTo(MenuItem other) {
        return Integer.compare(this.itemId, other.itemId);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - $%.2f (%s)", itemId, name, price, category);
    }
}