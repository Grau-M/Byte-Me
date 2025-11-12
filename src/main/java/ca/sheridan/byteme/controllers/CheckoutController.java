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
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("cartItems", cartService.getCartItems());
        return "checkout";
    }

    @PostMapping("/charge")
    public String charge(ChargeRequest chargeRequest,
                         @RequestParam(name = "guestName", required = false) String guestName,
                         @RequestParam(name = "guestEmail", required = false) String guestEmail,
                         Model model) throws StripeException {
        chargeRequest.setDescription("Cookiegram Order");
        chargeRequest.setCurrency(ChargeRequest.Currency.CAD);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String customerEmail;
        String customerName;
        User user = null;

        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
            customerEmail = user.getEmail();
            customerName = user.getName();
        } else {
            customerEmail = guestEmail;
            customerName = guestName;
        }

        boolean isGuest = user == null;

        if (customerEmail == null || customerEmail.isBlank()) {
            model.addAttribute("error", "Payment Failed: Email address is required.");
            model.addAttribute("isGuest", isGuest);
            return "result";
        }

        Charge charge = stripeService.charge(chargeRequest, customerEmail, customerName);

        Order order = Order.builder()
                .userId(user != null ? user.getId() : null) // Handle guest user ID
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
        model.addAttribute("isGuest", isGuest);
        return "result";
    }

    @ExceptionHandler(StripeException.class)
    public String handleError(Model model, StripeException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isGuest = authentication == null || !(authentication.getPrincipal() instanceof User);
        model.addAttribute("isGuest", isGuest);
        model.addAttribute("error", ex.getMessage());
        return "result";
    }
}
