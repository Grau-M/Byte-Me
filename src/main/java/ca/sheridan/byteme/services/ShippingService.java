package ca.sheridan.byteme.services;

import ca.sheridan.byteme.beans.ShippingAddress;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ShippingService {

    /**
     * Mocks a call to a shipping carrier API (e.g., Canada Post).
     * @param address The destination address.
     * @return The shipping cost, or empty if the address is invalid.
     */
    public Optional<Double> getShippingCost(ShippingAddress address) {
        // In a real app, you'd use an HTTP client to call the API.
        // For this demo, we'll just check for a postal code.
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            // Return a mock flat rate
            return Optional.of(12.99);
        }

        // Invalid address, return empty
        return Optional.empty();
    }
}