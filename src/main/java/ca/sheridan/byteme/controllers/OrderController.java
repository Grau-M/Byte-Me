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

@Controller
public class OrderController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProfanityFilterService profanityFilterService;

    /**
     * Shows the main order page at /order
     */
    @GetMapping("/order") // <-- This now matches your path
    public String showOrderPage(Model model) {
        // This line will now work!
        model.addAttribute("cartCount", cartService.getCartCount());
        
        if (!model.containsAttribute("lastMessage")) {
            model.addAttribute("lastMessage", "");
        }
        if (!model.containsAttribute("lastColor")) {
            model.addAttribute("lastColor", "");
        }
        
        return "order"; // Renders order.html
    }

    /**
     * Handles the "Add to Cart" form submission from the order page.
     */
    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam String productId,
                            @RequestParam String productName,
                            @RequestParam double price,
                            @RequestParam String cookieMessage,
                            @RequestParam String icingColor,
                            RedirectAttributes redirectAttributes) {

        // 1. Check for profanity
        if (profanityFilterService.hasProfanity(cookieMessage)) {
            // Profanity detected!
            redirectAttributes.addFlashAttribute("cartError", "Your message contains inappropriate language and was not added to the cart.");
            redirectAttributes.addFlashAttribute("lastMessage", cookieMessage);
            redirectAttributes.addFlashAttribute("lastColor", icingColor);
            
            // *** FIX: Redirect back to /order ***
            return "redirect:/order";
        }

        // 2. No profanity, add to cart
        CartItem item = new CartItem(
                productId + "_" + cookieMessage.hashCode(), 
                productName,
                cookieMessage,
                icingColor,
                "",
                price
        );
        cartService.addItem(item);

        // 3. Add success message
        redirectAttributes.addFlashAttribute("cartSuccess", "Your cookie was added to the cart!");

        // 4. *** FIX: Redirect back to /order ***
        //    (You can change this to "redirect:/cart" if you prefer)
        return "redirect:/order";
    }
}