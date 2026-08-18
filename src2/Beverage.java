/**
 * Subclass representing drink items in the Cafe.
 */
public class Beverage extends MenuItem {
    private String size;       // Small, Medium, Large
    private boolean isHot;     // True for Hot, False for Iced
    private int sugarLevel;    // Percentage (0%, 25%, 50%, 100%)

    public Beverage(int itemId, String name, double price, String size, boolean isHot, int sugarLevel) {
        super(itemId, name, price, "Beverage");
        this.size = size;
        this.isHot = isHot;
        this.sugarLevel = sugarLevel;
    }

    // Beverage-specific getters and setters
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public boolean isHot() { return isHot; }
    public void setHot(boolean hot) { isHot = hot; }

    public int getSugarLevel() { return sugarLevel; }
    public void setSugarLevel(int sugarLevel) { this.sugarLevel = sugarLevel; }

    @Override
    public String toString() {
        String temp = isHot ? "Hot" : "Iced";
        return super.toString() + String.format(" [%s, %s, Sugar: %d%%]", size, temp, sugarLevel);
    }
}

