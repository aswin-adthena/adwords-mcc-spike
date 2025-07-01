package org.adthena.adwordsmcc.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import org.adthena.adwordsmcc.model.AdInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Simple service for retrieving ad information including final URLs.
 */
@Service
public class AdService {

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    /**
     * Gets enabled ads with their final URLs for a specific customer account.
     *
     * @param customerId The ID of the Google Ads customer account
     * @return List of AdInfo objects
     * @throws IOException if there's an error communicating with the API
     */
    public List<AdInfo> getEnabledAdsWithFinalUrls(String customerId) throws IOException {
        // Create credentials using the properties
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        // Build the Google Ads client
        GoogleAdsClient googleAdsClient = GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .build();

        // Create a list to store the ad information
        List<AdInfo> adInfoList = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient = googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            // Simple GAQL query to get enabled ads with their final URLs
            String query = "SELECT ad_group_ad.ad.id, ad_group_ad.ad.name, ad_group_ad.ad.final_urls, ad_group_ad.status " +
                "FROM ad_group_ad " +
                "WHERE ad_group_ad.status = 'ENABLED'";

            // Create the search request
            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(customerId)
                .setQuery(query)
                .build();

            // Execute the search request
            ServerStream<SearchGoogleAdsStreamResponse> stream = googleAdsServiceClient.searchStreamCallable().call(request);

            // Process the results
            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    Long adId = row.getAdGroupAd().getAd().getId();
                    String adName = row.getAdGroupAd().getAd().getName();
                    List<String> finalUrls = new ArrayList<>(row.getAdGroupAd().getAd().getFinalUrlsList());
                    String status = row.getAdGroupAd().getStatus().name();

                    // Only add ads with non-empty finalUrls
                    if (!finalUrls.isEmpty()) {
                        adInfoList.add(new AdInfo(adId, adName, finalUrls, status));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to retrieve ad information: " + e.getMessage(), e);
        }

        return adInfoList;
    }
}
