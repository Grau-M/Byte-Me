package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.repositories.OrderRepository;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ShippingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ManageOrderStatusServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void updateOrderStatus_rejectsSkippingAhead() {
        Order order = Order.builder()
                .id("order-123")
                .status(Status.Confirmed)
                .build();
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus("order-123", Status.Ready_for_Shipment));

        assertEquals("Invalid status change from Confirmed to Ready_for_Shipment.", thrown.getMessage());
        assertEquals(Status.Confirmed, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_returnsEmptyWhenOrderMissing() {
        when(orderRepository.findById("missing-id")).thenReturn(Optional.empty());

        Optional<Order> result = orderService.updateOrderStatus("missing-id", Status.Shipped);

        assertTrue(result.isEmpty());
        verify(orderRepository, never()).save(any());
    }
}
