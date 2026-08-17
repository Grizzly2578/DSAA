
public class Pastry extends MenuItem {
    private boolean isWarmed;
    private boolean containsGluten;

    public Pastry(int itemId, String name, double price, boolean isWarmed, boolean containsGluten) {
        super(itemId, name, price, "Pastry");
        this.isWarmed = isWarmed;
        this.containsGluten = containsGluten;
    }

    // Pastry-specific getters and setters
    public boolean isWarmed() { return isWarmed; }
    public void setWarmed(boolean warmed) { isWarmed = warmed; }

    public boolean isContainsGluten() { return containsGluten; }
    public void setContainsGluten(boolean containsGluten) { this.containsGluten = containsGluten; }

    @Override
    public String toString() {
        return super.toString() + String.format(" [Warmed: %b, Gluten-Free: %b]", isWarmed, !containsGluten);
    }
}

