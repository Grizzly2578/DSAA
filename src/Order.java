import java.util.List;

public record Order(List<CartItem> items, String customerAlias, double totalPrice) {
	public Order {
		items = List.copyOf(items);
		if (items.isEmpty()) {
			throw new IllegalArgumentException("An order must contain at least one item");
		}
		if (!Double.isFinite(totalPrice) || totalPrice < 0) {
			throw new IllegalArgumentException("Total price must be a valid positive amount");
		}
	}
}