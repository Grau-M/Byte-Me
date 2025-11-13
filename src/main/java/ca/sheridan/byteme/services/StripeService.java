package ca.sheridan.byteme.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import ca.sheridan.byteme.beans.ShippingAddress;
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
    public Charge charge(ChargeRequest chargeRequest, String customerEmail, ShippingAddress shippingAddress) throws StripeException {
        
        // --- STEP 1: Create Stripe Address Map ---
        Map<String, Object> addressParams = new HashMap<>();
        addressParams.put("line1", shippingAddress.getAddressLine1());
        addressParams.put("line2", shippingAddress.getAddressLine2());
        addressParams.put("city", shippingAddress.getCity());
        addressParams.put("state", shippingAddress.getProvince());
        addressParams.put("postal_code", shippingAddress.getPostalCode());
        addressParams.put("country", shippingAddress.getCountry());

        // --- STEP 2: Create a Stripe Customer ---
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("name", shippingAddress.getName()); // Use shipping name
        customerParams.put("email", customerEmail);
        customerParams.put("source", chargeRequest.getStripeToken());
        customerParams.put("address", addressParams); // <-- ADD ADDRESS
        
        Customer customer = Customer.create(customerParams);

        // --- STEP 3: Charge the Customer ---
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", chargeRequest.getAmount()); // Amount is now Subtotal + Tax + Shipping
        chargeParams.put("currency", chargeRequest.getCurrency());
        chargeParams.put("description", chargeRequest.getDescription());
        chargeParams.put("customer", customer.getId());
        
        // Add shipping details to the charge itself for records
        Map<String, Object> shippingParams = new HashMap<>();
        shippingParams.put("name", shippingAddress.getName());
        shippingParams.put("address", addressParams);
        chargeParams.put("shipping", shippingParams);

        return Charge.create(chargeParams);
    }
}