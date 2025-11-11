package ca.sheridan.byteme.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import ca.sheridan.byteme.models.ChargeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    // --- Replace your 'charge' method with this one ---
    public Charge charge(ChargeRequest chargeRequest, String customerEmail, String customerName) throws StripeException {
        
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", chargeRequest.getAmount());
        chargeParams.put("currency", chargeRequest.getCurrency());
        chargeParams.put("description", chargeRequest.getDescription());
        chargeParams.put("source", chargeRequest.getStripeToken());
        
        // This tells Stripe to send a receipt to this address
        chargeParams.put("receipt_email", customerEmail);

        // --- UPDATED METADATA ---
        // This attaches the data to the charge so you can see it
        // in your Stripe dashboard's "Metadata" section.
        Map<String, String> metadata = new HashMap<>();
        metadata.put("customer_name", customerName);
        metadata.put("customer_email", customerEmail); // Added email here
        chargeParams.put("metadata", metadata);

        // We removed the System.out.println from here

        return Charge.create(chargeParams);
    }
}