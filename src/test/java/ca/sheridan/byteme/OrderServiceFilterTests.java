package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.ShippingAddress;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.repositories.OrderRepository;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for OrderService.filterOrders() method
 * Use Case: Filter and Search Orders
 * Story: As an employee, I would like to view all orders based on filter criteria
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OrderServiceFilterTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private OrderService orderService;

    private List<Order> testOrders;

    @BeforeEach
    void setUp() {
        testOrders = new ArrayList<>();

        testOrders.add(Order.builder()
                .id("order-001")
                .status(Status.Confirmed)
                .orderDate(LocalDateTime.of(2025, 12, 1, 10, 30))
                .deliveryDate(LocalDate.of(2025, 12, 10))
                .shippingAddress(ShippingAddress.builder().name("John Doe").build())
                .items(List.of(new CartItem("item-1", "Chocolate Chip Cookie", "", "Blue", "#0000FF", 25.99)))
                .build());

        testOrders.add(Order.builder()
                .id("order-002")
                .status(Status.Baking)
                .orderDate(LocalDateTime.of(2025, 12, 2, 14, 15))
                .deliveryDate(LocalDate.of(2025, 12, 15))
                .shippingAddress(ShippingAddress.builder().name("james@example.com").build())
                .items(List.of(new CartItem("item-2", "Sugar Cookie", "", "Red", "#FF0000", 19.99)))
                .build());

        testOrders.add(Order.builder()
                .id("order-003")
                .status(Status.Shipped)
                .orderDate(LocalDateTime.of(2025, 11, 28, 9, 0))
                .deliveryDate(LocalDate.of(2025, 12, 3))
                .shippingAddress(ShippingAddress.builder().name("Jane Smith").build())
                .items(List.of(new CartItem("item-3", "Brownie", "", "Green", "#00FF00", 22.50)))
                .build());
    }

    // ==================== SUNNY DAY SCENARIOS ====================

    @Test
    void filterOrders_byStatus_showsOnlyConfirmedOrders() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders(null, Status.Confirmed, null, null, null);

        assertEquals(1, result.size());
        assertEquals(Status.Confirmed, result.get(0).getStatus());
    }

    @Test
    void filterOrders_byDeliveryDate_showsOrdersDueToday() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders(null, null, null, null, "2025-12-10");

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2025, 12, 10), result.get(0).getDeliveryDate());
    }

    @Test
    void filterOrders_byOrderDateRange_showsLast7Days() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders(null, null, "2025-11-27", "2025-12-03", null);

        assertEquals(3, result.size());
    }

    @Test
    void filterOrders_byCustomerEmail_findsSpecificOrder() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders("james@example.com", null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("order-002", result.get(0).getId());
    }

    @Test
    void filterOrders_byCustomerName_findsMatchingOrders() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders("Jane Smith", null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("Jane Smith", result.get(0).getShippingAddress().getName());
    }

    @Test
    void filterOrders_combinesMultipleCriteria_returnsRefinedList() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders(
                "cookie",
                Status.Confirmed,
                "2025-12-01",
                "2025-12-02",
                "2025-12-10"
        );

        assertEquals(1, result.size());
        assertEquals("order-001", result.get(0).getId());
    }

    // ==================== RAINY DAY SCENARIOS ====================

    @Test
    void filterOrders_noMatchingCriteria_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders("nonexistent@example.com", null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void filterOrders_emptyDatabase_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<Order> result = orderService.filterOrders(null, Status.Pending, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void filterOrders_noFiltersApplied_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(testOrders);

        List<Order> result = orderService.filterOrders(null, null, null, null, null);

        assertEquals(3, result.size());
    }
}
