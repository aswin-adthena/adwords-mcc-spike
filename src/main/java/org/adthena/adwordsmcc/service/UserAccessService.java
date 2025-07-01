package org.adthena.adwordsmcc.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.enums.AccessRoleEnum.AccessRole;
import com.google.ads.googleads.v18.resources.CustomerUserAccess;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    public Optional<CustomerUserAccess> getCustomerUserAccess(
            GoogleAdsClient googleAdsClient, long customerId, String emailAddress) {

        String query = buildUserAccessQuery(emailAddress);

        try (GoogleAdsServiceClient googleAdsServiceClient =
                googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            ServerStream<SearchGoogleAdsStreamResponse> stream = executeUserAccessQuery(
                googleAdsServiceClient, customerId, query);

            return extractUserAccessFromResponse(stream);
        }
    }

    private String buildUserAccessQuery(String emailAddress) {
        return String.format(
            "SELECT "
                + "  customer_user_access.user_id,"
                + "  customer_user_access.email_address,"
                + "  customer_user_access.access_role,"
                + "  customer_user_access.access_creation_date_time "
                + "FROM customer_user_access "
                + "WHERE"
                + "  customer_user_access.email_address = '%s'",
            emailAddress);
    }

    private ServerStream<SearchGoogleAdsStreamResponse> executeUserAccessQuery(
            GoogleAdsServiceClient googleAdsServiceClient, long customerId, String query) {
        SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
            .setCustomerId(String.valueOf(customerId))
            .setQuery(query)
            .build();
        return googleAdsServiceClient.searchStreamCallable().call(request);
    }

    private Optional<CustomerUserAccess> extractUserAccessFromResponse(ServerStream<SearchGoogleAdsStreamResponse> stream) {
        for (SearchGoogleAdsStreamResponse response : stream) {
            for (GoogleAdsRow row : response.getResultsList()) {
                if (row.hasCustomerUserAccess()) {
                    return Optional.of(row.getCustomerUserAccess());
                }
            }
        }
        return Optional.empty();
    }

    public String getAccessRoleDisplayName(AccessRole accessRole) {
        if (accessRole == null) {
            return "Unknown";
        }

        switch (accessRole) {
            case ADMIN:
                return "Admin";
            case STANDARD:
                return "Standard";
            case READ_ONLY:
                return "Read Only";
            case EMAIL_ONLY:
                return "Email Only";
            default:
                return accessRole.name();
        }
    }
}
