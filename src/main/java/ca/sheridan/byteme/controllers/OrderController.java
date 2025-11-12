package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.ProfanityFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.HashMap;

@Controller
public class OrderController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProfanityFilterService profanityFilterService;

    /**
     * Shows the main order page at /order
     */
    @GetMapping("/order")
    public String showOrderPage(Model model) {
        model.addAttribute("cartCount", cartService.getCartCount());
        
        if (!model.containsAttribute("lastMessage")) {
            model.addAttribute("lastMessage", "");
        }
        if (!model.containsAttribute("lastColor")) {
            model.addAttribute("lastColor", "");
        }
        
        return "order";
    }

    /**
     * Handles the "Add to Cart" form submission from the order page.
     */
    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam String productId,
                            @RequestParam String productName,
                            @RequestParam double price,
                            @RequestParam String cookieMessage,
                            @RequestParam("icingColor") String icingColorValue,
                            // --- 1. ADD THIS NEW PARAMETER --- (NEW)
                            @RequestParam(name = "buyNow", required = false, defaultValue = "false") boolean buyNow,
                            RedirectAttributes redirectAttributes) {

        if (profanityFilterService.hasProfanity(cookieMessage)) {
            redirectAttributes.addFlashAttribute("cartError", "Your message contains inappropriate language and was not added to the cart.");
            redirectAttributes.addFlashAttribute("lastMessage", cookieMessage);
            redirectAttributes.addFlashAttribute("lastColor", icingColorValue);
            return "redirect:/order";
        }

        Map<String, String> colorDetails = getIcingColorDetails(icingColorValue);

        CartItem item = new CartItem(
                productId + "_" + cookieMessage.hashCode(),
                productName,
                cookieMessage,
                colorDetails.get("name"),
                colorDetails.get("hex"),
                price
        );
        cartService.addItem(item);

        redirectAttributes.addFlashAttribute("cartSuccess", "Your cookie was added to the cart!");
        
        // --- 2. CHANGE THIS REDIRECT LOGIC --- (CHANGED)
        if (buyNow) {
            // Guest user (buyNow=true) -> go to checkout
            return "redirect:/checkout";
        } else {
            // Logged-in user (buyNow=false) -> stay on order page
            return "redirect:/order";
        }
    }

    /**
     * Helper method to get the full name and hex code for a given color value.
     */
    private Map<String, String> getIcingColorDetails(String colorValue) {
        Map<String, String> details = new HashMap<>();
        switch (colorValue) {
            case "white":
                details.put("name", "Classic White");
                details.put("hex", "#FFFFFF");
                break;
            case "pink":
                details.put("name", "Pretty Pink");
                details.put("hex", "hotpink");
                break;
            case "red":
                details.put("name", "Vibrant Red");
                details.put("hex", "red");
                break;
            case "blue":
                details.put("name", "Cool Blue");
                details.put("hex", "blue");
                break;
            case "green":
                details.put("name", "Festive Green");
                details.put("hex", "green");
                break;
            case "chocolate":
                details.put("name", "Chocolate");
                details.put("hex", "#8B5A2B");
                break;
            default:
                details.put("name", "Unknown");
                details.put("hex", "#eee");
                break;
        }
        return details;
    }
}