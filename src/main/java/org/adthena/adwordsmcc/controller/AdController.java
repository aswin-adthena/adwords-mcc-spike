package org.adthena.adwordsmcc.controller;

import org.adthena.adwordsmcc.model.AdInfo;
import org.adthena.adwordsmcc.service.AdService;
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
 * Simple controller for ad information.
 */
@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdService adService;

    @Autowired
    public AdController(AdService adService) {
        this.adService = adService;
    }

    /**
     * Gets enabled ads with their final URLs for a specific customer account.
     *
     * @param customerId The ID of the Google Ads customer account
     * @return List of AdInfo objects
     */
    @GetMapping("/final-urls/{customerId}")
    public ResponseEntity<List<AdInfo>> getEnabledAdsWithFinalUrls(@PathVariable String customerId) {
        try {
            // Get the ad information using the Ad service
            List<AdInfo> adInfoList = adService.getEnabledAdsWithFinalUrls(customerId);

            return ResponseEntity.ok(adInfoList);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
