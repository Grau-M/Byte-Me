package ca.sheridan.byteme.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON response from the API-Ninjas Profanity Filter.
 * The API returns keys named "has_profanity", "censored" and "original",
 * so we map those JSON properties to record components used by the app.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfanityResponse(
    @JsonProperty("has_profanity") boolean has_profanity,
    @JsonProperty("censored") String censored_text,
    @JsonProperty("original") String original_text
) {
    // Record components are intentionally named to match existing usage in the codebase.
}