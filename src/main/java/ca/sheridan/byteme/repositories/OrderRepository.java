package ca.sheridan.byteme.repositories;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByUserIdAndIdContainingIgnoreCase(String userId, String searchTerm);
    List<Order> findByUserIdAndStatus(String userId, Status status);

    @Query("{ 'userId' : ?0, $or: [ " +
           "{ 'id' : { $regex: ?1, $options: 'i' } }, " +
           "{ 'items.name' : { $regex: ?1, $options: 'i' } }, " +
           "{ 'items.message' : { $regex: ?1, $options: 'i' } }, " +
           "{ 'shippingAddress.addressLine1' : { $regex: ?1, $options: 'i' } }, " +
           "{ 'shippingAddress.city' : { $regex: ?1, $options: 'i' } }, " +
           "{ 'shippingAddress.postalCode' : { $regex: ?1, $options: 'i' } } " +
           "] }")
    List<Order> findByUserIdAndSearchTerm(String userId, String searchTerm);

    List<Order> findByUserIdAndOrderDateBetween(String userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
