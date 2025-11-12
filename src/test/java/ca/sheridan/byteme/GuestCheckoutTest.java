package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.ShippingAddress; // <-- IMPORT
import ca.sheridan.byteme.controllers.CheckoutController;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ShippingService; // <-- IMPORT
import ca.sheridan.byteme.services.StripeService;
import com.stripe.model.Charge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList; // <-- IMPORT

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify; // <-- IMPORT
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GuestCheckoutTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;
    
    @Mock
    private ShippingService shippingService; // <-- ADD MOCK FOR NEW SERVICE

    @Mock
    private Model model;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    public void testGuestCheckout() throws Exception {
        // Arrange
        ChargeRequest chargeRequest = new ChargeRequest();
        String stripeToken = "tok_visa";
        String guestEmail = "guest@example.com";
        double shippingCost = 12.99;
        
        // Mock shipping address details
        String shippingName = "Guest User";
        String shippingAddressLine1 = "123 Main St";
        String shippingAddressLine2 = "";
        String shippingCity = "Toronto";
        String shippingProvince = "ON";
        String shippingPostalCode = "M5V 2K7";
        String shippingCountry = "Canada";

        Charge charge = new Charge();
        charge.setStatus("succeeded");
        charge.setAmount(11299L); // (subtotal + tax + shipping) * 100
        charge.setId("ch_123");

        // --- Mock CartService calls ---
        double subtotal = 80.00;
        double tax = 10.40;
        double total = subtotal + tax; // 90.40
        
        when(cartService.getSubtotal()).thenReturn(subtotal);
        when(cartService.getTax()).thenReturn(tax);
        when(cartService.getTotal()).thenReturn(total); // Total before shipping
        when(cartService.getCartItems()).thenReturn(new ArrayList<>()); // Empty list for simplicity
        
        // --- Mock StripeService call ---
        // Signature is now (ChargeRequest, String customerEmail, ShippingAddress)
        when(stripeService.charge(any(ChargeRequest.class), any(String.class), any(ShippingAddress.class)))
                .thenReturn(charge);

        // --- Mock OrderService call ---
        when(orderService.createOrder(any(Order.class))).thenReturn(new Order());

        // Act
        String result = checkoutController.charge(
                chargeRequest,
                stripeToken,
                guestEmail,
                shippingName,
                shippingAddressLine1,
                shippingAddressLine2,
                shippingCity,
                shippingProvince,
                shippingPostalCode,
                shippingCountry,
                shippingCost,
                model
        );

        // Assert
        assertEquals("result", result);
        
        // Verify that the cart was cleared for the guest
        verify(cartService).clearCart();
        
        // Verify the order was saved
        verify(orderService).createOrder(any(Order.class));
    }
}