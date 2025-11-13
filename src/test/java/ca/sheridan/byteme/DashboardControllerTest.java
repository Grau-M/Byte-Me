package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.controllers.DashboardController;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    @Mock
    private PromotionService promotionService;

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @Mock
    private Principal principal;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetDashboard_Customer() {
        // Arrange
        User customer = new User();
        customer.setId("customerId");
        customer.setRole(Role.CUSTOMER);
        when(authentication.getPrincipal()).thenReturn(customer);
        when(principal.getName()).thenReturn("customer");
        when(cartService.getCartCount()).thenReturn(2);
        when(promotionService.getActivePromotionsForToday()).thenReturn(new ArrayList<>());
        when(orderService.getOrdersForUser("customerId")).thenReturn(new ArrayList<>());

        // Act
        String viewName = dashboardController.getDashboard(model, principal);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("cartCount", 2);
        verify(model).addAttribute(eq("promotions"), any());
        verify(model).addAttribute(eq("orders"), any());
        verify(model).addAttribute("username", "customer");
    }
    @Test
    void testGetDashboard_Admin() {
        // Arrange
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(authentication.getPrincipal()).thenReturn(admin);
        when(principal.getName()).thenReturn("admin");
        when(cartService.getCartCount()).thenReturn(0);

        // Act
        String viewName = dashboardController.getDashboard(model, principal);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("cartCount", 0);
        verify(model, never()).addAttribute(eq("promotions"), any());
        verify(model, never()).addAttribute(eq("orders"), any());
        verify(model).addAttribute("username", "admin");
    }

    @Test
    void testGetDashboard_Unauthenticated() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(new Object()); // Not a User instance
        when(principal.getName()).thenReturn("guest");

        // Act
        String viewName = dashboardController.getDashboard(model, principal);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model, never()).addAttribute(eq("promotions"), any());
        verify(model, never()).addAttribute(eq("orders"), any());
        verify(model).addAttribute("username", "guest");
    }
}
