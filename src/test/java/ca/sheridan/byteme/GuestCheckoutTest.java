package ca.sheridan.byteme;

import ca.sheridan.byteme.controllers.CheckoutController;
import ca.sheridan.byteme.models.ChargeRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.StripeService;
import com.stripe.model.Charge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private Model model;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    public void testGuestCheckout() throws Exception {
        // Arrange
        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setStripeToken("tok_visa");

        Charge charge = new Charge();
        charge.setStatus("succeeded");
        charge.setAmount(1000L);

        when(stripeService.charge(any(ChargeRequest.class), any(String.class), any(String.class)))
                .thenReturn(charge);

        // Act
        String result = checkoutController.charge(chargeRequest, "Guest User", "guest@example.com", model);

        // Assert
        assertEquals("result", result);
    }
}
