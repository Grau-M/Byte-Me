package ca.sheridan.byteme;

import ca.sheridan.byteme.controllers.OrderController;
import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ProfanityFilterService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private ProfanityFilterService profanityFilterService;

    @Mock
    private OrderService orderService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private HttpSession session;

    @InjectMocks
    private OrderController orderController;

    @Test
    void testAddToCart_Success() {
        // Arrange
        when(profanityFilterService.hasProfanity("Clean message")).thenReturn(false);

        // Act
        String result = orderController.addToCart("prod1", "Product 1", 10.0, "Clean message", "white", false, null, null, redirectAttributes, session);

        // Assert
        assertEquals("redirect:/order", result);
        verify(cartService).addItem(any(CartItem.class));
        verify(redirectAttributes).addFlashAttribute("cartSuccess", "Your cookie has been saved!");
    }    @Test
    void testAddToCart_WithProfanity() {
        // Arrange
        when(profanityFilterService.hasProfanity("Dirty message")).thenReturn(true);

        // Act
        String result = orderController.addToCart("prod1", "Product 1", 10.0, "Dirty message", "white", false, null, null, redirectAttributes, session);

        // Assert
        assertEquals("redirect:/order", result);
        verify(cartService, never()).addItem(any(CartItem.class));
        verify(redirectAttributes).addFlashAttribute("cartError", "Your message contains inappropriate language and was not added to the cart.");
        verify(redirectAttributes).addFlashAttribute("lastMessage", "Dirty message");
        verify(redirectAttributes).addFlashAttribute("lastColor", "white");
    }

    @Test
    void testAddToCart_UpdateItemInOrder() {
        // Arrange
        ca.sheridan.byteme.beans.Order mockOrder = new ca.sheridan.byteme.beans.Order();
        mockOrder.setItems(new java.util.ArrayList<>());
        when(orderService.getOrderById("order123")).thenReturn(mockOrder);
        when(profanityFilterService.hasProfanity("Updated message")).thenReturn(false);

        // Act
        String result = orderController.addToCart("prod1", "Product 1", 10.0, "Updated message", "blue", false, "item123", "order123", redirectAttributes, session);

        // Assert
        assertEquals("redirect:/order/edit/order123", result);
        verify(orderService).updateOrder(mockOrder);
        verify(redirectAttributes).addFlashAttribute("cartMessage", "Item updated successfully!");
    }
}
