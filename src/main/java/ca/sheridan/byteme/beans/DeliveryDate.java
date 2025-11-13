package ca.sheridan.byteme.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "delivery_dates")
public class DeliveryDate {

    @Id
    private String id;
    private LocalDate date;
    private int orderCount;
}
