import java.util.HashMap;

public class Drink extends MenuItem {
    public Drink(int id, String name, double smallPrice, double mediumPrice, double largePrice) {
        super(id, name, prices(smallPrice, mediumPrice, largePrice));
    }

    private static HashMap<Size, Double> prices(double small, double medium, double large) {
        HashMap<Size, Double> prices = new HashMap<>();
        prices.put(Size.SMALL, small);
        prices.put(Size.MEDIUM, medium);
        prices.put(Size.LARGE, large);
        return prices;
    }

    @Override
    public String getType() {
        return "Drink";
    }
}