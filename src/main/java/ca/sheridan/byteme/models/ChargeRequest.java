package ca.sheridan.byteme.models;

import lombok.Data;

@Data
public class ChargeRequest {
    private String description;
    private int amount;
    private Currency currency;
    private String stripeToken;

    public enum Currency {
        EUR, USD, CAD;
    }
}
