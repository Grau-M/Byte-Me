package ca.sheridan.byteme.services;

import ca.sheridan.byteme.models.CartItem;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A session-scoped service to manage the shopping cart.
 * Each user gets their own instance of this bean for their session.
 */
@Service
@SessionScope
public class CartService implements Serializable {

    private static final long serialVersionUID = 1L;

    // A list to hold the items in the cart
    private final List<CartItem> items = new ArrayList<>();

    /**
     * Adds an item to the cart.
     * If an item with the same ID already exists, it will be replaced (for this app, that's fine).
     * @param item The CartItem to add.
     */
    public void addItem(CartItem item) {
        // For simplicity, we'll remove any existing item with the same ID first
        // This makes "adding" an item effectively an "update" if it's already there.
        items.removeIf(existing -> existing.id().equals(item.id()));
        items.add(item);
    }

    /**
     * Removes an item from the cart by its ID.
     * @param itemId The ID of the item to remove.
     */
    public void removeItem(String itemId) {
        items.removeIf(item -> item.id().equals(itemId));
    }

    /**
     * Gets all items currently in the cart.
     * @return A list of CartItems.
     */
    public List<CartItem> getItems() {
        return new ArrayList<>(items); // Return a copy to prevent external modification
    }

    /**
     * Calculates the subtotal of all items in the cart.
     * @return The total price as a double.
     */
    public double getSubtotal() {
        return items.stream()
                .mapToDouble(CartItem::price)
                .sum();
    }

    /**
     * Clears all items from the cart.
     */
    public void clearCart() {
        items.clear();
    }

    /**
     * ***** THIS IS THE MISSING METHOD *****
     * Gets the total number of items in the cart.
     * @return The size of the items list.
     */
    public int getCartCount() {
        return items.size();
    }
}