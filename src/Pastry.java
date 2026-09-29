import java.util.HashMap;

public class Pastry extends MenuItem {
    public Pastry(int id, String name, double price) {
        super(id, name, prices(price));
    }

    private static HashMap<Size, Double> prices(double price) {
        HashMap<Size, Double> prices = new HashMap<>();
        prices.put(Size.STANDARD, price);
        return prices;
    }

    @Override
    public String getType() {
        return "Pastry";
    }
}