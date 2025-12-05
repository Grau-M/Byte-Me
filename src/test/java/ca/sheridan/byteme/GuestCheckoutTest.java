package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.controllers.CheckoutController;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.StripeService;
import ca.sheridan.byteme.services.DeliveryDateService; // <-- IMPORTANT: delivery date service
import com.stripe.model.Charge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class GuestCheckoutTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private DeliveryDateService deliveryDateService; // <-- ADD THIS MOCK

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    public void testGuestCheckout() throws Exception {
        // Arrange
        ChargeRequest chargeRequest = new ChargeRequest();
        String stripeToken = "tok_test_123";
        String guestEmail = "guest@example.com";
        double shippingCost = 5.99;
        LocalDate deliveryDate = LocalDate.now().plusDays(2);

        // Mock delivery date availability
        when(deliveryDateService.isDateAvailable(deliveryDate)).thenReturn(true);

        // Mock shipping address details
        String shippingName = "John Doe";
        String shippingAddressLine1 = "123 Main St";
        String shippingAddressLine2 = "Apt 4B";
        String shippingCity = "Toronto";
        String shippingProvince = "ON";
        String shippingPostalCode = "M1M1M1";
        String shippingCountry = "Canada";

        // --- Mock CartService calls ---
        double subtotal = 80.00;
        double tax = 10.40;
        double total = subtotal + tax; // 90.40

        when(cartService.getSubtotal()).thenReturn(subtotal);
        when(cartService.getTax()).thenReturn(tax);
        when(cartService.getTotal()).thenReturn(total);
        when(cartService.getCartItems()).thenReturn(new ArrayList<>());

        // --- Mock StripeService call ---
        Charge charge = new Charge();
        charge.setStatus("succeeded");
        charge.setAmount((long) ((total + shippingCost) * 100));
        charge.setId("ch_123");

        when(stripeService.charge(any(), any(), any()))
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
                deliveryDate,
                "Jane Doe", // billingName
                "456 Billing St", // billingAddressLine1
                "", // billingAddressLine2
                "Billing City", // billingCity
                "ON", // billingProvince
                "B2B 2B2", // billingPostalCode
                "Canada", // billingCountry
                model,
                redirectAttributes
        );

        // Assert
        assertEquals("result", result);
        verify(cartService).clearCart();
        verify(orderService).createOrder(any(Order.class));
        verify(deliveryDateService).addOrderToDate(deliveryDate);
    }
}
