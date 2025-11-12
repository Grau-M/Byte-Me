package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.ShippingAddress;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ShippingService; // <-- IMPORT
import ca.sheridan.byteme.services.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // <-- IMPORT

import java.time.LocalDateTime;
import java.util.Map; // <-- IMPORT
import java.util.Optional; // <-- IMPORT

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
    @Autowired
    private ShippingService shippingService; // <-- INJECT

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("subtotal", cartService.getSubtotal());
        model.addAttribute("tax", cartService.getTax());
        model.addAttribute("shippingCost", 0.00);
        model.addAttribute("total", cartService.getTotal());
        model.addAttribute("amount", (int) (cartService.getTotal() * 100));
        model.addAttribute("cartItems", cartService.getCartItems());

        // --- New Logic to Get Default Name ---
        String defaultName = ""; // Start with empty
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            defaultName = user.getName(); // Get the user's name
        }
        model.addAttribute("defaultName", defaultName); // Add it to the model
        // --- End of New Logic ---

        return "checkout";
    }

    /**
     * NEW API ENDPOINT
     * Called by JavaScript to get a shipping cost estimate.
     */
    @PostMapping("/api/shipping/calculate")
    @ResponseBody // Tells Spring to return JSON
    public ResponseEntity<?> calculateShipping(@RequestBody ShippingAddress address) {
        Optional<Double> costOpt = shippingService.getShippingCost(address);

        if (costOpt.isPresent()) {
            return ResponseEntity.ok(Map.of("shippingCost", costOpt.get()));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid address, please check postal code."));
        }
    }


    /**
     * UPDATED CHARGE ENDPOINT
     * Now takes all form fields and creates the order with shipping.
     */
    @PostMapping("/charge")
    public String charge(ChargeRequest chargeRequest,
                         @RequestParam String stripeToken, // Stripe token is now passed manually
                         @RequestParam(name = "guestEmail", required = false) String guestEmail,
                         @RequestParam(name = "shippingName") String shippingName,
                         @RequestParam(name = "shippingAddressLine1") String shippingAddressLine1,
                         @RequestParam(name = "shippingAddressLine2", required = false) String shippingAddressLine2,
                         @RequestParam(name = "shippingCity") String shippingCity,
                         @RequestParam(name = "shippingProvince") String shippingProvince,
                         @RequestParam(name = "shippingPostalCode") String shippingPostalCode,
                         @RequestParam(name = "shippingCountry") String shippingCountry,
                         @RequestParam(name = "shippingCost") double shippingCost,
                         Model model) throws StripeException {
        
        chargeRequest.setDescription("Cookiegram Order");
        chargeRequest.setCurrency(ChargeRequest.Currency.CAD);
        chargeRequest.setStripeToken(stripeToken); // Set the token from the form

        // 1. Get Customer Email
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String customerEmail;
        User user = null;

        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
            customerEmail = user.getEmail();
        } else {
            customerEmail = guestEmail;
        }

        // 2. Build ShippingAddress from form
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .name(shippingName)
                .addressLine1(shippingAddressLine1)
                .addressLine2(shippingAddressLine2)
                .city(shippingCity)
                .province(shippingProvince)
                .postalCode(shippingPostalCode)
                .country(shippingCountry)
                .build();

        // 3. Charge the card
        // We pass the *full amount* (which includes shipping) to the charge request
        double total = cartService.getTotal() + shippingCost;
        chargeRequest.setAmount((int) (total * 100));
        
        Charge charge = stripeService.charge(chargeRequest, customerEmail, shippingAddress);

        // 4. Create the Order
        Order order = Order.builder()
                .userId(user != null ? user.getId() : null)
                .items(cartService.getCartItems())
                .subtotal(cartService.getSubtotal())
                .tax(cartService.getTax())
                .shippingCost(shippingCost) // <-- SAVE SHIPPING
                .total(total)               // <-- SAVE NEW TOTAL
                .orderDate(LocalDateTime.now())
                .chargeId(charge.getId())
                .shippingAddress(shippingAddress) // <-- SAVE ADDRESS
                .build();

        orderService.createOrder(order);
        cartService.clearCart();

        model.addAttribute("status", charge.getStatus());
        model.addAttribute("amount", charge.getAmount() / 100.0);
        model.addAttribute("isGuest", user == null);
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