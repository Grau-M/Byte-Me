package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.services.*;
import lombok.RequiredArgsConstructor;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Staff/Admin order management dashboard.
 * Allows updating the processing status of existing orders.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final OrderService orderService;
    private final NotificationService notificationService;
    

    @GetMapping
    public String showOrders(Model model, Principal principal) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", Status.values());

        // --- 1. Dynamic Clock (unchanged) ---
        ZoneId userZone = ZoneId.of("America/Toronto");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            if (currentUser.getTimezone() != null && !currentUser.getTimezone().isEmpty()) {
                userZone = ZoneId.of(currentUser.getTimezone());
            }

           
        }

        ZonedDateTime zonedTime = ZonedDateTime.now(userZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
        String formattedTime = zonedTime.format(formatter);
        model.addAttribute("currentTime", formattedTime);

        return "orders";
    }

    @PostMapping("/{orderId}/status")
    public String updateOrderStatus(@PathVariable String orderId,
                                    @RequestParam("status") Status newStatus,
                                    RedirectAttributes redirectAttributes) {

        try {
            Optional<Order> updatedOrderOpt = orderService.updateOrderStatus(orderId, newStatus);

            if (updatedOrderOpt.isEmpty()) {
                // Rainy-day: order was deleted / does not exist
                redirectAttributes.addFlashAttribute("errorMessage", "Order no longer exists.");
                return "redirect:/orders";
            }

            Order updatedOrder = updatedOrderOpt.get();

            // Try sending notification; if it fails, we still keep the status change
            try {
                notificationService.sendOrderStatusUpdateNotification(updatedOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Status updated successfully.");
            } catch (Exception ex) {
                // Rainy-day: notification failure
                redirectAttributes.addFlashAttribute(
                        "warningMessage",
                        "Status updated, but failed to send customer email."
                );
            }

        } catch (IllegalArgumentException ex) {
            // Invalid transition or missing params
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            // Rainy-day: database or other unexpected error
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not update order. Please try again."
            );
        }

        return "redirect:/orders";
    }
}
