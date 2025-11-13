package ca.sheridan.byteme.services;

import ca.sheridan.byteme.beans.DeliveryDate;
import ca.sheridan.byteme.repositories.DeliveryDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeliveryDateService {

    private static final int MAX_ORDERS_PER_DAY = 10;

    @Autowired
    private DeliveryDateRepository deliveryDateRepository;

    public List<LocalDate> getAvailableDates() {
        LocalDate today = LocalDate.now();
        LocalDate oneYearFromNow = today.plusYears(1);
        List<DeliveryDate> deliveryDates = deliveryDateRepository.findByDateBetween(today, oneYearFromNow);

        return today.datesUntil(oneYearFromNow)
                .filter(date -> {
                    Optional<DeliveryDate> deliveryDate = deliveryDates.stream()
                            .filter(dd -> dd.getDate().equals(date))
                            .findFirst();
                    return deliveryDate.map(dd -> dd.getOrderCount() < MAX_ORDERS_PER_DAY).orElse(true);
                })
                .collect(Collectors.toList());
    }

    public boolean isDateAvailable(LocalDate date) {
        Optional<DeliveryDate> deliveryDate = deliveryDateRepository.findByDate(date);
        return deliveryDate.map(dd -> dd.getOrderCount() < MAX_ORDERS_PER_DAY).orElse(true);
    }

    public void addOrderToDate(LocalDate date) {
        DeliveryDate deliveryDate = deliveryDateRepository.findByDate(date)
                .orElse(DeliveryDate.builder().date(date).orderCount(0).build());
        deliveryDate.setOrderCount(deliveryDate.getOrderCount() + 1);
        deliveryDateRepository.save(deliveryDate);
    }
}
