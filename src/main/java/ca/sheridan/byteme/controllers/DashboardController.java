package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.PromotionService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
@Controller
public class DashboardController {

    private final PromotionService promotionService;
    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping("/dashboard")
    public String getDashboard(Model model, Principal principal,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String filter) {

        // --- Add Cart Count ---
        model.addAttribute("cartCount", cartService.getCartCount());

        // --- 1. Dynamic Clock (unchanged) ---
        ZoneId userZone = ZoneId.of("America/Toronto");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            if (currentUser.getTimezone() != null && !currentUser.getTimezone().isEmpty()) {
                userZone = ZoneId.of(currentUser.getTimezone());
            }

            // ---- 2. Only for CUSTOMERS: add promotions and orders fetched from MongoDB ----
            if (currentUser.getRole() == Role.CUSTOMER) {
                model.addAttribute("promotions", promotionService.getActivePromotionsForToday());

                List<Order> orders;
                if (search != null && !search.isEmpty()) {
                    orders = orderService.searchOrders(currentUser.getId(), search);
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "No orders found matching your criteria");
                    }
                } else if (filter != null && !filter.isEmpty()) {
                    orders = orderService.filterOrders(currentUser.getId(), filter);
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "No orders found matching your criteria");
                    }
                } else {
                    orders = orderService.getOrdersForUser(currentUser.getId());
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "You haven't placed any orders yet. Start shopping!");
                    }
                }
                model.addAttribute("orders", orders);
            }
        }

        ZonedDateTime zonedTime = ZonedDateTime.now(userZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        String formattedTime = zonedTime.format(formatter);
        model.addAttribute("currentTime", formattedTime);

        String username = (principal != null) ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        return "dashboard";
    }
}
