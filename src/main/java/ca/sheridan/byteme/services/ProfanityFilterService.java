package ca.sheridan.byteme.services;

import ca.sheridan.byteme.models.ProfanityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProfanityFilterService {

    private final RestTemplate restTemplate;

    // Inject the API key from application.properties
    @Value("${api.ninjas.key}")
    private String apiKey;

    private static final String API_URL = "https://api.api-ninjas.com/v1/profanityfilter";

    public ProfanityFilterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Checks a given text for profanity using the API-Ninjas service.
     * @param text The text to check.
     * @return ProfanityResponse object from the API.
     */
    public ProfanityResponse checkProfanity(String text) {
        
        // ********** THIS IS THE FIX **********
        // It now checks for the placeholder, not your real key.
        if (apiKey == null || apiKey.equals("YOUR_API_KEY_HERE")) {
            System.err.println("Profanity API key is not set. Skipping check.");
            // Return a "clean" response to avoid blocking functionality
            return new ProfanityResponse(false, text, text);
        }
        // *************************************

        try {
            // Build the URL with the query parameter (properly encoded)
            // Use 'build(false)' to prevent double-encoding by RestTemplate
            String urlWithParams = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("text", text)
                    .build(false)  // Prevent RestTemplate from re-encoding
                    .toUriString();

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // --- Debug logging for testing ---
            // String maskedKey = apiKey == null ? "null" : ("****" + apiKey.substring(Math.max(0, apiKey.length() - 4)));
            // System.out.println("[ProfanityFilter] Checking text: \"" + text + "\"");
            // System.out.println("[ProfanityFilter] Calling API URL: " + urlWithParams);
            // System.out.println("[ProfanityFilter] X-Api-Key (masked): " + maskedKey);

            // Make the API call
            ResponseEntity<ProfanityResponse> response = restTemplate.exchange(
                urlWithParams,
                HttpMethod.GET,
                entity,
                ProfanityResponse.class
            );

            ProfanityResponse body = response.getBody();
            // if (body != null) {
            //     System.out.println("[ProfanityFilter] API Response -> has_profanity: " + body.has_profanity()
            //             + ", original: \"" + body.original_text() + "\", censored: \"" + body.censored_text() + "\"");
            // } else {
            //     System.out.println("[ProfanityFilter] API Response -> body is null");
            // }

            return body;
        } catch (Exception e) {
            // Log the error and assume the text is "clean" to avoid false positives
            System.err.println("Error calling profanity filter API: " + e.getMessage());
            e.printStackTrace();
            return new ProfanityResponse(false, text, text);
        }
    }

    /**
     * A simple check to see if the response contains profanity.
     * @param text The text to check.
     * @return true if profanity is detected, false otherwise.
     */
    public boolean hasProfanity(String text) {
        ProfanityResponse response = checkProfanity(text);
        return response != null && response.has_profanity();
    }
}