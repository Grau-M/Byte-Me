package ca.sheridan.byteme.repositories;

import ca.sheridan.byteme.beans.DeliveryDate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryDateRepository extends MongoRepository<DeliveryDate, String> {

    Optional<DeliveryDate> findByDate(LocalDate date);
    List<DeliveryDate> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
