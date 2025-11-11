package ca.sheridan.byteme.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer; // <-- ADD THIS IMPORT
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

    // --- THIS 'charge' METHOD IS NOW UPDATED ---
    public Charge charge(ChargeRequest chargeRequest, String customerEmail, String customerName) throws StripeException {
        
        // --- STEP 1: Create a Stripe Customer ---
        // We use the token (source) to create the customer and their payment method
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("name", customerName);
        customerParams.put("email", customerEmail);
        customerParams.put("source", chargeRequest.getStripeToken());
        
        Customer customer = Customer.create(customerParams);

        // --- STEP 2: Charge the Customer ---
        // Now we charge the Scustomer ID instead of the token
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", chargeRequest.getAmount());
        chargeParams.put("currency", chargeRequest.getCurrency());
        chargeParams.put("description", chargeRequest.getDescription());
        chargeParams.put("customer", customer.getId()); // <-- THE KEY CHANGE

        // (We don't need metadata anymore, as the charge is linked to the customer)

        return Charge.create(chargeParams);
    }
}