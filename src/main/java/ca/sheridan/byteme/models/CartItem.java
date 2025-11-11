package ca.sheridan.byteme.models;

// Using a Java Record for a simple, immutable data object
public record CartItem(
    String id,
    String name,
    String message,
    String icingColorName,
    String icingColorHex,
    double price
) {
    // You can add validation or helper methods here if needed
}