package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.DeliveryDate;
import ca.sheridan.byteme.repositories.DeliveryDateRepository;
import ca.sheridan.byteme.services.DeliveryDateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class DeliveryDateServiceTest {

    @Mock
    private DeliveryDateRepository deliveryDateRepository;

    @InjectMocks
    private DeliveryDateService deliveryDateService;

    @Test
    void testIsDateAvailable_WhenOrderLimitNotReached() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        DeliveryDate deliveryDate = DeliveryDate.builder().date(testDate).orderCount(9).build();
        when(deliveryDateRepository.findByDate(testDate)).thenReturn(Optional.of(deliveryDate));

        // Act
        boolean result = deliveryDateService.isDateAvailable(testDate);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsDateAvailable_WhenOrderLimitReached() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        DeliveryDate deliveryDate = DeliveryDate.builder().date(testDate).orderCount(10).build();
        when(deliveryDateRepository.findByDate(testDate)).thenReturn(Optional.of(deliveryDate));

        // Act
        boolean result = deliveryDateService.isDateAvailable(testDate);

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsDateAvailable_WhenNoOrdersForDate() {
        // Arrange
        LocalDate testDate = LocalDate.now().plusDays(1);
        when(deliveryDateRepository.findByDate(testDate)).thenReturn(Optional.empty());

        // Act
        boolean result = deliveryDateService.isDateAvailable(testDate);

        // Assert
        assertTrue(result);
    }
}
