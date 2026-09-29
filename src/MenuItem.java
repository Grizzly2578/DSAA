import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MenuItem {
    private final int id;
    private String name;
    private final HashMap<Size, Double> prices;

    public MenuItem(int id, String name, Map<Size, Double> prices) {
        if (id <= 0 || name == null || name.isBlank() || name.length() > 100 || prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Invalid menu item");
        }
        this.id = id;
        this.name = name;
        this.prices = new HashMap<>(prices);
        this.prices.forEach((size, price) -> validatePrice(size, price));
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<Size, Double> getPrices() {
        return Collections.unmodifiableMap(prices);
    }

    public double getPrice(Size size) {
        Double price = prices.get(size);
        if (price == null) {
            throw new IllegalArgumentException("This item does not support size " + size);
        }
        return price;
    }

    public double getPrice() {
        if (prices.containsKey(Size.MEDIUM)) return prices.get(Size.MEDIUM);
        if (prices.containsKey(Size.STANDARD)) return prices.get(Size.STANDARD);
        return prices.values().iterator().next();
    }

    public boolean supportsSize(Size size) {
        return prices.containsKey(size);
    }

    public void setName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Invalid item name");
        }
        this.name = name;
    }

    public void setPrice(Size size, double price) {
        validatePrice(size, price);
        prices.put(size, price);
    }

    public void setPrice(double price) {
        setPrice(prices.containsKey(Size.STANDARD) ? Size.STANDARD : Size.MEDIUM, price);
    }

    public String getType() {
        return "Menu Item";
    }

    private static void validatePrice(Size size, double price) {
        if (size == null || !Double.isFinite(price) || price < 0 || price > 1_000_000) {
            throw new IllegalArgumentException("Invalid item price");
        }
    }

    @Override
    public String toString() {
        return String.format("[ID: %-3d] %-20s %s %s", id, name, getType(), prices);
    }
}