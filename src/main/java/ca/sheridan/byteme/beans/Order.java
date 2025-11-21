package ca.sheridan.byteme.beans;

import ca.sheridan.byteme.models.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String userId;
    private List<CartItem> items;
    private double subtotal;
    private double tax;
    private double shippingCost;
    private double total;
    private LocalDateTime orderDate;
    private LocalDate deliveryDate;
    private String chargeId;
    private ShippingAddress shippingAddress;
    private BillingAddress billingAddress;
    private String carrier;         // e.g., "UPS", "FedEx", "USPS"
    private String trackingNumber;

    @Builder.Default
    private Status status = Status.Pending;
    private Status statusBeforeCancel;


}
