package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.ShippingAddress;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.DeliveryDateService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map; // <-- IMPORT
import java.util.Optional; // <-- IMPORT

@Controller
public class CheckoutController {

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    private final StripeService stripeService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ShippingService shippingService;
    private final DeliveryDateService deliveryDateService;

    @Autowired
    public CheckoutController(StripeService stripeService, CartService cartService, OrderService orderService, ShippingService shippingService, DeliveryDateService deliveryDateService) {
        this.stripeService = stripeService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.shippingService = shippingService;
        this.deliveryDateService = deliveryDateService;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(name = "editOrderId", required = false) String editOrderId, Model model) {

        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("availableDates", deliveryDateService.getAvailableDates());

        // Default name from logged-in user
        String defaultName = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
            defaultName = user.getName();
        }
        model.addAttribute("defaultName", defaultName);

        if (editOrderId != null) {
            // --- EDITING AN EXISTING ORDER ---
            Optional<Order> existingOrderOpt = Optional.of(orderService.getOrderById(editOrderId));
            if (existingOrderOpt.isPresent()) {
                Order existingOrder = existingOrderOpt.get();
                double subtotal = orderService.calculateSubtotal(existingOrder);
                double tax = orderService.calculateTax(existingOrder);
                double total = subtotal + tax; // Initial total without shipping

                existingOrder.setSubtotal(subtotal);
                existingOrder.setTax(tax);
                existingOrder.setTotal(total);

                model.addAttribute("order", existingOrder);
                model.addAttribute("cartItems", existingOrder.getItems());
                model.addAttribute("subtotal", subtotal);
                model.addAttribute("tax", tax);
                model.addAttribute("shippingCost", existingOrder.getShippingCost());
                model.addAttribute("total", total);
                model.addAttribute("amount", (int) (total * 100));

                if (existingOrder.getShippingAddress() != null) {
                    model.addAttribute("shippingAddress", existingOrder.getShippingAddress());
                } else {
                    model.addAttribute("shippingAddress", new ShippingAddress());
                }
            } else {
                // Handle case where order ID is invalid
                return "redirect:/cart";
            }
        } else {
            // --- NEW CHECKOUT FROM CART ---
            model.addAttribute("cartItems", cartService.getCartItems());
            model.addAttribute("subtotal", cartService.getSubtotal());
            model.addAttribute("tax", cartService.getTax());
            model.addAttribute("shippingCost", 0.00);
            model.addAttribute("total", cartService.getTotal());
            model.addAttribute("amount", (int) (cartService.getTotal() * 100));
            model.addAttribute("shippingAddress", new ShippingAddress());
        }

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
                         @RequestParam String stripeToken,
                         @RequestParam(name = "guestEmail", required = false) String guestEmail,
                         @RequestParam(name = "shippingName") String shippingName,
                         @RequestParam(name = "shippingAddressLine1") String shippingAddressLine1,
                         @RequestParam(name = "shippingAddressLine2", required = false) String shippingAddressLine2,
                         @RequestParam(name = "shippingCity") String shippingCity,
                         @RequestParam(name = "shippingProvince") String shippingProvince,
                         @RequestParam(name = "shippingPostalCode") String shippingPostalCode,
                         @RequestParam(name = "shippingCountry") String shippingCountry,
                         @RequestParam(name = "shippingCost") double shippingCost,
                         @RequestParam(name = "deliveryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                         @RequestParam(name = "billingName", required = false) String billingName,
                         @RequestParam(name = "billingAddressLine1", required = false) String billingAddressLine1,
                         @RequestParam(name = "billingAddressLine2", required = false) String billingAddressLine2,
                         @RequestParam(name = "billingCity", required = false) String billingCity,
                         @RequestParam(name = "billingProvince", required = false) String billingProvince,
                         @RequestParam(name = "billingPostalCode", required = false) String billingPostalCode,
                         @RequestParam(name = "billingCountry", required = false) String billingCountry,
                         Model model, RedirectAttributes redirectAttributes) throws StripeException {
        
        if (!deliveryDateService.isDateAvailable(deliveryDate)) {
            redirectAttributes.addFlashAttribute("error", "The selected delivery date is no longer available.");
            return "redirect:/checkout";
        }

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

        // 3. Build BillingAddress from form
        ca.sheridan.byteme.beans.BillingAddress billingAddress = ca.sheridan.byteme.beans.BillingAddress.builder()
                .name(billingName)
                .addressLine1(billingAddressLine1)
                .addressLine2(billingAddressLine2)
                .city(billingCity)
                .province(billingProvince)
                .postalCode(billingPostalCode)
                .country(billingCountry)
                .build();

        // 4. Charge the card
        // We pass the *full amount* (which includes shipping) to the charge request
        double total = cartService.getTotal() + shippingCost;
        chargeRequest.setAmount((int) (total * 100));
        
        Charge charge = stripeService.charge(chargeRequest, customerEmail, shippingAddress);

        // 5. Create the Order
        Order order = Order.builder()
                .userId(user != null ? user.getId() : null)
                .items(cartService.getCartItems())
                .subtotal(cartService.getSubtotal())
                .tax(cartService.getTax())
                .shippingCost(shippingCost) // <-- SAVE SHIPPING
                .total(total)               // <-- SAVE NEW TOTAL
                .orderDate(LocalDateTime.now())
                .deliveryDate(deliveryDate)
                .chargeId(charge.getId())
                .shippingAddress(shippingAddress) // <-- SAVE ADDRESS
                .billingAddress(billingAddress) // <-- SAVE BILLING ADDRESS
                .build();

        orderService.createOrder(order);
        deliveryDateService.addOrderToDate(deliveryDate);
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

    @GetMapping("/checkout/edit/{orderId}")
    public String editOrder(@PathVariable String orderId, Model model) {
        Optional<Order> orderOpt = Optional.of(orderService.getOrderById(orderId));
        if (orderOpt.isEmpty()) {
            return "redirect:/"; // or some error page
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("availableDates", deliveryDateService.getAvailableDates());

        // Pre-fill shipping info
        model.addAttribute("shippingName", order.getShippingAddress().getName());
        model.addAttribute("shippingAddressLine1", order.getShippingAddress().getAddressLine1());
        model.addAttribute("shippingAddressLine2", order.getShippingAddress().getAddressLine2());
        model.addAttribute("shippingCity", order.getShippingAddress().getCity());
        model.addAttribute("shippingProvince", order.getShippingAddress().getProvince());
        model.addAttribute("shippingPostalCode", order.getShippingAddress().getPostalCode());
        model.addAttribute("shippingCountry", order.getShippingAddress().getCountry());
        model.addAttribute("shippingCost", order.getShippingCost());

        model.addAttribute("cartItems", order.getItems());
        model.addAttribute("subtotal", order.getSubtotal());
        model.addAttribute("tax", order.getTax());
        model.addAttribute("total", order.getTotal());

        return "checkout"; // reuse the same template
    }
    @PostMapping("/checkout/edit/{orderId}")
    public String saveEditedOrder(@PathVariable String orderId,
                                @RequestParam(required = false) String shippingName,
                                @RequestParam(required = false) String shippingAddressLine1,
                                @RequestParam(required = false) String shippingAddressLine2,
                                @RequestParam(required = false) String shippingCity,
                                @RequestParam(required = false) String shippingProvince,
                                @RequestParam(required = false) String shippingPostalCode,
                                @RequestParam(required = false) String shippingCountry,
                                @RequestParam(name = "deliveryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                @RequestParam(name = "billingName", required = false) String billingName,
                                @RequestParam(name = "billingAddressLine1", required = false) String billingAddressLine1,
                                @RequestParam(name = "billingAddressLine2", required = false) String billingAddressLine2,
                                @RequestParam(name = "billingCity", required = false) String billingCity,
                                @RequestParam(name = "billingProvince", required = false) String billingProvince,
                                @RequestParam(name = "billingPostalCode", required = false) String billingPostalCode,
                                @RequestParam(name = "billingCountry", required = false) String billingCountry,
                                RedirectAttributes redirectAttributes) {

        Optional<Order> orderOpt = Optional.of(orderService.getOrderById(orderId));
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/"; // or error page
        }

        Order order = orderOpt.get();

        if (!deliveryDateService.isDateAvailable(deliveryDate)) {
            redirectAttributes.addFlashAttribute("error", "The selected delivery date is no longer available.");
            return "redirect:/checkout?editOrderId=" + orderId;
        }

        ShippingAddress updatedAddress = ShippingAddress.builder()
                .name(shippingName)
                .addressLine1(shippingAddressLine1)
                .addressLine2(shippingAddressLine2)
                .city(shippingCity)
                .province(shippingProvince)
                .postalCode(shippingPostalCode)
                .country(shippingCountry)
                .build();

        ca.sheridan.byteme.beans.BillingAddress billingAddress = ca.sheridan.byteme.beans.BillingAddress.builder()
                .name(billingName)
                .addressLine1(billingAddressLine1)
                .addressLine2(billingAddressLine2)
                .city(billingCity)
                .province(billingProvince)
                .postalCode(billingPostalCode)
                .country(billingCountry)
                .build();

        order.setShippingAddress(updatedAddress);
        order.setBillingAddress(billingAddress);
        order.setDeliveryDate(deliveryDate);

        orderService.recalculateOrderTotals(order); // New line

        orderService.updateOrder(order);
        deliveryDateService.addOrderToDate(deliveryDate);

        redirectAttributes.addFlashAttribute("cartMessage", "Shipping details saved successfully!");
        // Redirect back to cart in edit mode
        return "redirect:/order/edit/" + order.getId();
        }

}