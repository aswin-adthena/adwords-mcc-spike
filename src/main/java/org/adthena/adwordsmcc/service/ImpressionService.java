package org.adthena.adwordsmcc.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import org.adthena.adwordsmcc.model.CountryImpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Simple service for retrieving impression data by country.
 */
@Service
public class ImpressionService {

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    /**
     * Gets impressions by country for the last 7 days.
     *
     * @param customerId  The ID of the Google Ads customer account
     * @return List of CountryImpression objects
     * @throws IOException if there's an error communicating with the API
     */
    public List<CountryImpression> getImpressionsByCountry(String customerId) throws IOException {
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


        // Create a list to store the country impression data
        List<CountryImpression> countryImpressions = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient = googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            // Simple GAQL query to get impressions by country for the last 7 days
            String query = "SELECT geographic_view.country_criterion_id, metrics.impressions " +
                    "FROM geographic_view " +
                    "WHERE segments.date DURING LAST_7_DAYS";

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
                    Long countryCriterionId = row.getGeographicView().getCountryCriterionId();
                    Long impressions = row.getMetrics().getImpressions();

                    countryImpressions.add(new CountryImpression(countryCriterionId, impressions));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to retrieve impression data: " + e.getMessage(), e);
        }

        return countryImpressions;
    }
}
