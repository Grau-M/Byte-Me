package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.Status;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerDashboardFilterTests {

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

    private User customer;

    @BeforeEach
    void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        customer = new User();
        customer.setId("customerId");
        customer.setRole(Role.CUSTOMER);

        when(authentication.getPrincipal()).thenReturn(customer);
        when(principal.getName()).thenReturn("customer");
    }

    @Test
    void testSearchOrders_ResultsFound() {
        // Arrange
        String searchTerm = "order123";
        Order order = new Order();
        order.setId(searchTerm);
        List<Order> foundOrders = Arrays.asList(order);
        when(orderService.searchOrders("customerId", searchTerm)).thenReturn(foundOrders);

        // Act
        String viewName = dashboardController.getDashboard(model, principal, searchTerm, null);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("orders", foundOrders);
        verify(model, never()).addAttribute(eq("message"), anyString());
    }

    @Test
    void testSearchOrders_NoResultsFound() {
        // Arrange
        String searchTerm = "NonExistentOrder";
        when(orderService.searchOrders("customerId", searchTerm)).thenReturn(Collections.emptyList());

        // Act
        String viewName = dashboardController.getDashboard(model, principal, searchTerm, null);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("orders", Collections.emptyList());
        verify(model).addAttribute("message", "No orders found matching your criteria");
    }

    @Test
    void testFilterOrders_ResultsFound() {
        // Arrange
        String filter = "SHIPPED"; // Use uppercase as per Status enum
        Order order = new Order();
        order.setStatus(Status.Shipped);
        List<Order> filteredOrders = Arrays.asList(order);
        when(orderService.filterOrders("customerId", filter)).thenReturn(filteredOrders);

        // Act
        String viewName = dashboardController.getDashboard(model, principal, null, filter);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("orders", filteredOrders);
        verify(model, never()).addAttribute(eq("message"), anyString());
    }

    @Test
    void testFilterOrders_NoResultsFound() {
        // Arrange
        String filter = "NON_EXISTENT_STATUS";
        when(orderService.filterOrders("customerId", filter)).thenReturn(Collections.emptyList());

        // Act
        String viewName = dashboardController.getDashboard(model, principal, null, filter);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("orders", Collections.emptyList());
        verify(model).addAttribute("message", "No orders found matching your criteria");
    }

    @Test
    void testEmptyOrderHistory() {
        // Arrange
        when(orderService.getOrdersForUser("customerId")).thenReturn(Collections.emptyList());

        // Act
        String viewName = dashboardController.getDashboard(model, principal, null, null);

        // Assert
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("orders", Collections.emptyList());
        verify(model).addAttribute("message", "You haven't placed any orders yet. Start shopping!");
    }
    
    @Test
    void testGetDashboard_PreventsViewingOtherUsersOrders() {
        // Arrange
        User anotherCustomer = new User();
        anotherCustomer.setId("anotherCustomerId");
        anotherCustomer.setRole(Role.CUSTOMER);

        Order otherUsersOrder = new Order();
        otherUsersOrder.setId("otherOrder123");
        otherUsersOrder.setUserId("anotherCustomerId");

        // Mock that order service will *not* return another user's order when asked for current user's orders
        when(orderService.getOrdersForUser(customer.getId())).thenReturn(new ArrayList<>());
        
        // This test primarily relies on the orderService.getOrdersForUser returning only relevant orders
        // If it were to return others, the service itself would be flawed.
        // The controller's responsibility here is to call the service with the correct userId.

        // Act
        String viewName = dashboardController.getDashboard(model, principal, null, null);

        // Assert
        assertEquals("dashboard", viewName);
        // Verify that orderService was asked for orders belonging to 'customerId'
        verify(orderService).getOrdersForUser(customer.getId());
        // Verify that the model does not contain another user's order
        verify(model).addAttribute("orders", new ArrayList<>()); // Or a list that specifically doesn't contain 'otherUsersOrder' if the initial getOrdersForUser was mocked to return something else
        verify(model, never()).addAttribute("orders", Collections.singletonList(otherUsersOrder));
    }
}