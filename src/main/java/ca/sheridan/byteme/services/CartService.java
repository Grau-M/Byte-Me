package ca.sheridan.byteme.services;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.ShippingAddress;
import ca.sheridan.byteme.models.CartItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A session-scoped service to manage the shopping cart.
 * Each user gets their own instance of this bean for their session.
 */
@Service
public class CartService {

    // A list to hold the items in the cart
    private final List<CartItem> items = new ArrayList<>();
    private final OrderService orderService;

    @Autowired
    public CartService(OrderService orderService) {
        this.orderService = orderService;
    }

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
    public List<CartItem> getCartItems() {
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
     * Calculates the tax on the subtotal.
     * @return The tax as a double.
     */
    public double getTax() {
        return getSubtotal() * 0.13;
    }

    /**
     * Calculates the total of the cart.
     * @return The total as a double.
     */
    public double getTotal() {
        return getSubtotal() + getTax();
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

    public Optional<CartItem> getItemById(String itemId) {
        return items.stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst();
    }

    public boolean updateOrderShipping(Long orderId, String shippingName, String shippingAddressLine1,
                                   String shippingAddressLine2, String shippingCity, String shippingProvince,
                                   String shippingPostalCode, String shippingCountry) {
        if (orderId == null) return false;

        // Get the order
        Optional<Order> orderOpt = orderService.getOrderByIdOptional(orderId.toString());
        if (orderOpt.isEmpty()) {
            return false; // order not found
        }

        Order order = orderOpt.get();

        // Update shipping information
        ShippingAddress shipping = new ShippingAddress(
            shippingName,
            shippingAddressLine1,
            shippingAddressLine2,
            shippingCity,
            shippingProvince,
            shippingPostalCode,
            shippingCountry
        );

        // Assign it to the order
        order.setShippingAddress(shipping);

        // Persist the changes
        orderService.updateOrder(order);

        return true;
        }
}