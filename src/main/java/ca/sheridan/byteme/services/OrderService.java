package ca.sheridan.byteme.services;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    private static final double TAX_RATE = 0.13; // Example: 13% HST

    // -----------------------------
    // CRUD Operations
    // -----------------------------
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getOrdersForUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        return orderOpt.orElse(null); // return null if not found
    }

    public void updateOrder(Order order) {
        if (order.getId() == null) {
            throw new IllegalArgumentException("Order ID cannot be null for update");
        }
        orderRepository.save(order);
    }

    // -----------------------------
    // Order Operations
    // -----------------------------
    public boolean cancelOrder(Order order) {
        if (order.getStatus() != Status.Pending) {
            return false;
        }
        order.setStatus(Status.Canceled);
        updateOrder(order);
        return true;
    }

    public void removeItemFromOrder(Order order, String itemId) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        order.getItems().removeIf(item -> item.id().equals(itemId));
        updateOrder(order);
    }

    // -----------------------------
    // Calculation Methods
    // -----------------------------
    public double calculateSubtotal(Order order) {
        if (order.getItems() == null) return 0.0;
        return order.getItems().stream()
                .mapToDouble(CartItem::price) // multiply by quantity if you add it
                .sum();
    }

    public double calculateTax(Order order) {
        return calculateSubtotal(order) * TAX_RATE;
    }

    public double calculateTotal(Order order) {
        return calculateSubtotal(order) + calculateTax(order);
    }

    public Optional<Order> getOrderByIdOptional(String editOrderId) {
    if (editOrderId == null || editOrderId.isEmpty()) {
        return Optional.empty();
    }
    return orderRepository.findById(editOrderId);
    }
}