package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.services.CartService;
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

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String showCartPage(Model model, @RequestParam(required = false) String orderId) {
        List<CartItem> items = cartService.getCartItems();
        double subtotal = cartService.getSubtotal();
        double tax = cartService.getTax();
        double total = cartService.getTotal();
        int cartCount = cartService.getCartCount();

        model.addAttribute("cartItems", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);
        model.addAttribute("cartCount", cartCount);
        if (orderId != null) {
            model.addAttribute("orderId", orderId);
        }

        return "cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam String itemId,
                                 RedirectAttributes redirectAttributes) {
                                     
        cartService.removeItem(itemId);
        
        redirectAttributes.addFlashAttribute("cartMessage", "Item removed from cart.");
        
        return "redirect:/cart";
    }
}
