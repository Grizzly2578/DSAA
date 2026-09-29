import java.time.Instant;

public record CompletedSale(Order order, Instant completedAt, String completedBy) {
}