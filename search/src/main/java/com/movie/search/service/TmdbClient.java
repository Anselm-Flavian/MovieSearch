package com.movie.search.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.logging.Logger;

@Service
public class TmdbClient {

    private static final Logger LOGGER = Logger.getLogger(TmdbClient.class.getName());
    private final RestTemplate restTemplate;

    public TmdbClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = {ResourceAccessException.class, RestClientException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Map<String, Object> fetchFromTmdb(String url) {
        LOGGER.info("Fetching from TMDB: " + url);
        try {
            // [FIX] Log attempt for debugging
            LOGGER.info("Attempting TMDB API call");
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                LOGGER.warning("TMDB returned null response for URL: " + url);
                throw new RestClientException("Null response from TMDB");
            }
            return response;
        } catch (RestClientException e) {
            LOGGER.severe("TMDB API call failed: " + e.getMessage());
            throw e;
        }
    }

    // [FIX] Fallback when retries fail
    @Recover
    public Map<String, Object> recoverFromTmdbFailure(RestClientException e, String url) {
        LOGGER.severe("All retries failed for TMDB API call: " + url + ", error: " + e.getMessage());
        return Map.of("results", new java.util.ArrayList<>()); // Return empty results
    }
}