package ca.sheridan.byteme.controllers;

import java.security.Principal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.services.PromotionService;   // <-- add
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Controller
public class DashboardController {

    private final PromotionService promotionService;   // <-- add

    @GetMapping("/dashboard")
    public String getDashboard(Model model, Principal principal) {

        // --- 1. Dynamic Clock (unchanged) ---
        ZoneId userZone = ZoneId.of("America/Toronto");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            if (currentUser.getTimezone() != null && !currentUser.getTimezone().isEmpty()) {
                userZone = ZoneId.of(currentUser.getTimezone());
            }

            // ---- 2. Only for CUSTOMERS: add promotions fetched from MongoDB ----
            if (currentUser.getRole() == Role.CUSTOMER) {
                model.addAttribute("promotions", promotionService.getActivePromotionsForToday());
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
