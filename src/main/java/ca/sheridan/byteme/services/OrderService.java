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
    @Autowired
    private ShippingService shippingService;

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
        // Customer-facing cancel: only allow when pending
        if (order.getStatus() != Status.Pending) {
            return false;
        }
        order.setStatusBeforeCancel(order.getStatus());
        order.setStatus(Status.Canceled);
        updateOrder(order);
        return true;
    }

    public void removeItemFromOrder(Order order, String itemId) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        order.getItems().removeIf(item -> item.id().equals(itemId));

        // Recalculate totals
        order.setSubtotal(calculateSubtotal(order));
        order.setTax(calculateTax(order));
        order.setTotal(calculateTotal(order));
        if (order.getItems().isEmpty()) {
            order.setShippingCost(0.0);
        }

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
        double subtotal = calculateSubtotal(order);
        double tax = calculateTax(order);
        double shippingCost = 0.0;

        if (order.getShippingAddress() != null && !order.getItems().isEmpty()) {
            shippingCost = shippingService.getShippingCost(order.getShippingAddress()).orElse(0.0);
        }
        return subtotal + tax + shippingCost;
    }

    public Optional<Order> getOrderByIdOptional(String editOrderId) {
    if (editOrderId == null || editOrderId.isEmpty()) {
        return Optional.empty();
    }
    return orderRepository.findById(editOrderId);
    }

    public void recalculateOrderTotals(Order order) {
        if (order == null) {
            return;
        }
        double subtotal = calculateSubtotal(order);
        double tax = calculateTax(order);
        double shippingCost = 0.0;
        if (order.getShippingAddress() != null && !order.getItems().isEmpty()) {
            shippingCost = shippingService.getShippingCost(order.getShippingAddress()).orElse(0.0);
        }
        
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingCost(shippingCost);
        order.setTotal(subtotal + tax + shippingCost);
    }

                    // -----------------------------
                // Staff / Admin Operations
                // -----------------------------
                public List<Order> getAllOrders() {
                    return orderRepository.findAll();
                }

                /**
         * Validates if we can move from the current status to the new status.
         * Rules:
         *  - Allowed forward chain: Pending -> Confirmed -> Baking -> Ready_for_Shipment -> Shipped -> Delivered
         *  - Any status can move to Canceled.
         *  - No backward moves or skipping steps.
         */
        private boolean isValidStatusTransition(Order order, Status target) {
            if (order == null || target == null) {
                return false;
            }

            Status current = order.getStatus();

            // No-op change
            if (current == target) {
                return false;
            }

            // Allow cancel from any state
            if (target == Status.Canceled) {
                return true;
            }

            // Restore path: allow returning from Canceled to the immediate prior status
            if (current == Status.Canceled) {
                Status prior = order.getStatusBeforeCancel();
                return prior != null && target == prior;
            }

            // Ordered happy-path statuses
            List<Status> orderedStatuses = List.of(
                    Status.Pending,
                    Status.Confirmed,
                    Status.Baking,
                    Status.Ready_for_Shipment,
                    Status.Shipped,
                    Status.Delivered
            );

            int currentIndex = orderedStatuses.indexOf(current);
            int targetIndex = orderedStatuses.indexOf(target);

            // Only allow a *single step* forward
            return currentIndex != -1 && targetIndex == currentIndex + 1;
        }

                /**
         * Updates the status of an order, enforcing allowed transitions.
         *
         * @param orderId   the ID of the order to update
         * @param newStatus the requested new status
         * @return Optional<Order> with the updated order, or Optional.empty() if not found
         * @throws IllegalArgumentException if the transition is not allowed or arguments are invalid
         */
        public Optional<Order> updateOrderStatus(String orderId, Status newStatus) {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("Order id is required.");
            }
            if (newStatus == null) {
                throw new IllegalArgumentException("New status is required.");
            }

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                // Covers the "order no longer exists" rainy-day scenario
                return Optional.empty();
            }

            Order order = orderOpt.get();
            Status currentStatus = order.getStatus();

            if (!isValidStatusTransition(order, newStatus)) {
                throw new IllegalArgumentException(
                        String.format("Invalid status change from %s to %s.", currentStatus, newStatus)
                );
            }

            // Track where we came from when cancelling; clear when restoring
            if (newStatus == Status.Canceled) {
                order.setStatusBeforeCancel(currentStatus);
            } else if (currentStatus == Status.Canceled) {
                order.setStatusBeforeCancel(null);
            }

            order.setStatus(newStatus);
            // Reuse your existing update method (does validation + save)
            updateOrder(order);

            return Optional.of(order);
        }




}
