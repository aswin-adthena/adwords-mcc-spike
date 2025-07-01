package org.adthena.adwordsmcc.controller;

import org.adthena.adwordsmcc.model.CountryImpression;
import org.adthena.adwordsmcc.service.ImpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Simple controller for impression data.
 */
@RestController
@RequestMapping("/api/impressions")
public class ImpressionController {

    private final ImpressionService impressionService;

    @Autowired
    public ImpressionController(ImpressionService impressionService) {
        this.impressionService = impressionService;
    }

    /**
     * Gets impressions by country for the last 7 days.
     *
     * @param customerId The ID of the Google Ads customer account
     * @return List of CountryImpression objects
     */
    @GetMapping("/by-country/{customerId}")
    public ResponseEntity<List<CountryImpression>> getImpressionsByCountry(@PathVariable String customerId) {
        try {
            // Get the impression data using the Impression service
            List<CountryImpression> impressions = impressionService.getImpressionsByCountry(customerId);

            return ResponseEntity.ok(impressions);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
