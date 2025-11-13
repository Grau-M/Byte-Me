package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.models.UpdateProfileRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SettingsController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CartService cartService;

    @Autowired
    public SettingsController(UserService userService, PasswordEncoder passwordEncoder, CartService cartService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.cartService = cartService;
    }

    @GetMapping("/settings")
    public String getSettingsPage(Model model) {
        // Add cart count
        model.addAttribute("cartCount", cartService.getCartCount());

        // Add current time for the navbar clock
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
        model.addAttribute("currentTime", LocalDateTime.now().format(formatter));

        // Get all available time zones and sort them by region/city
        List<Map<String, String>> timeZones = ZoneId.getAvailableZoneIds().stream()
            .filter(zoneId -> !zoneId.startsWith("Etc/") && !zoneId.startsWith("SystemV/"))
            .map(zoneId -> {
                ZoneId zone = ZoneId.of(zoneId);
                ZonedDateTime now = ZonedDateTime.now(zone);
                String offset = now.getOffset().getId().replace("Z", "+00:00");
                return Map.of(
                    "id", zoneId,
                    "display", String.format("%s (%s)", zoneId, offset)
                );
            })
            .sorted((a, b) -> a.get("display").compareTo(b.get("display")))
            .collect(Collectors.toList());
        
        model.addAttribute("timeZones", timeZones);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder()
                    .name(currentUser.getName())
                    .email(currentUser.getEmail())
                    .timezone(currentUser.getTimezone() != null ? currentUser.getTimezone() : "America/Toronto") // Default to Toronto
                    .emailNotifications(currentUser.isEmailNotifications())
                    .inAppNotifications(currentUser.isInAppNotifications())
                    .pushNotifications(currentUser.isPushNotifications())
                    .darkThemeEnabled(currentUser.isDarkThemeEnabled())
                    .build();
            model.addAttribute("updateProfileRequest", updateProfileRequest);
        } else {
            model.addAttribute("updateProfileRequest", new UpdateProfileRequest());
        }

        return "settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(@ModelAttribute UpdateProfileRequest updateProfileRequest, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not authenticated.");
            return "redirect:/settings";
        }

        // Validate current password if new password fields are provided
        if (!updateProfileRequest.getNewPassword().isEmpty() || !updateProfileRequest.getConfirmNewPassword().isEmpty()) {
            if (!passwordEncoder.matches(updateProfileRequest.getCurrentPassword(), currentUser.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Incorrect current password.");
                return "redirect:/settings";
            }
            if (!updateProfileRequest.getNewPassword().equals(updateProfileRequest.getConfirmNewPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
                return "redirect:/settings";
            }
            if (updateProfileRequest.getNewPassword().length() < 8) { // Example: minimum password length
                redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 8 characters long.");
                return "redirect:/settings";
            }
            currentUser.setPassword(passwordEncoder.encode(updateProfileRequest.getNewPassword()));
        }

        // Update name and email
        currentUser.setName(updateProfileRequest.getName());
        currentUser.setEmail(updateProfileRequest.getEmail());

        userService.saveUser(currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/settings";
    }

    @PostMapping("/settings/preferences")
    public String updatePreferences(@ModelAttribute UpdateProfileRequest updateProfileRequest, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not authenticated.");
            return "redirect:/settings";
        }

        currentUser.setTimezone(updateProfileRequest.getTimezone());
        currentUser.setEmailNotifications(updateProfileRequest.isEmailNotifications());
        currentUser.setInAppNotifications(updateProfileRequest.isInAppNotifications());
        currentUser.setPushNotifications(updateProfileRequest.isPushNotifications());
        currentUser.setDarkThemeEnabled(updateProfileRequest.isDarkThemeEnabled());
        userService.saveUser(currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Preferences updated successfully!");
        return "redirect:/settings";
    }
}

