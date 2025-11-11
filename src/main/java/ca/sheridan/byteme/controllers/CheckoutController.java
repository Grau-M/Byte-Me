package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
public class CheckoutController {

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("subtotal", cartService.getSubtotal());
        model.addAttribute("tax", cartService.getTax());
        model.addAttribute("total", cartService.getTotal());
        model.addAttribute("amount", (int) (cartService.getTotal() * 100));
        return "checkout";
    }

    @PostMapping("/charge")
    public String charge(ChargeRequest chargeRequest, Model model) throws StripeException {
        chargeRequest.setDescription("Cookiegram Order");
        chargeRequest.setCurrency(ChargeRequest.Currency.CAD);

        // Get the logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        // Get customer details from the user object
        String customerEmail = user.getEmail(); // Assuming your User bean has getEmail()
        
        // **IMPORTANT**: Adjust "getFirstName()" and "getLastName()" if your
        // User bean uses different method names (e.g., getFullName())
        String customerName = user.getName(); 

        // Handle case where user has no email
        if (customerEmail == null || customerEmail.isBlank()) {
            model.addAttribute("error", "Payment Failed: No email address is associated with your account.");
            return "result";
        }

        // Call the updated service method
        Charge charge = stripeService.charge(chargeRequest, customerEmail, customerName);

        // ... (rest of your order creation logic) ...
        Order order = Order.builder()
                .userId(user.getId())
                .items(cartService.getCartItems())
                .subtotal(cartService.getSubtotal())
                .tax(cartService.getTax())
                .total(cartService.getTotal())
                .orderDate(LocalDateTime.now())
                .chargeId(charge.getId())
                .build();

        orderService.createOrder(order);

        System.out.println("Order created: " + order);

        cartService.clearCart();

        model.addAttribute("status", charge.getStatus());
        model.addAttribute("amount", charge.getAmount() / 100.0);
        return "result";
    }

    @ExceptionHandler(StripeException.class)
    public String handleError(Model model, StripeException ex) {
        model.addAttribute("error", ex.getMessage());
        return "result";
    }
}
