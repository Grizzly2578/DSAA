public record CartItem(int itemId, Size size, int quantity) {
    public CartItem {
        if (quantity <= 0 || quantity > 999) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (size == null) {
            throw new IllegalArgumentException("Size is required");
        }
    }
}