package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.models.CartItem; // Assuming models package
import ca.sheridan.byteme.services.CartService; // Assuming services package
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    // Autowire the same session-scoped CartService
    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String showCartPage(Model model) {
        
        // --- NO MORE MOCK DATA ---
        // We get the real items from the session-scoped service
        List<CartItem> items = cartService.getItems();
        double subtotal = cartService.getSubtotal();
        int cartCount = cartService.getCartCount();

        // Add real data to the model for Thymeleaf to use
        model.addAttribute("cartItems", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartCount", cartCount); // For the navbar bubble

        // This tells Spring to render the "cart.html" template
        // This is what is failing, because the file is in the wrong spot
        return "cart";
    }

    /**
     * Handles removing an item from the cart.
     */
    @PostMapping("/remove")
    public String removeFromCart(@RequestParam String itemId,
                                 RedirectAttributes redirectAttributes) {
                                     
        cartService.removeItem(itemId);
        
        // Add a success message for the cart page
        redirectAttributes.addFlashAttribute("cartMessage", "Item removed from cart.");
        
        // Redirect back to the cart page
        return "redirect:/cart";
    }
}