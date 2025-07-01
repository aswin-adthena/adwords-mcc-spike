package org.adthena.adwordsmcc.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.enums.ManagerLinkStatusEnum.ManagerLinkStatus;
import com.google.ads.googleads.v18.services.CustomerClientLinkOperation;
import com.google.ads.googleads.v18.services.CustomerClientLinkServiceClient;
import com.google.ads.googleads.v18.services.CustomerManagerLinkOperation;
import com.google.ads.googleads.v18.services.CustomerManagerLinkServiceClient;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.MutateCustomerClientLinkResponse;
import com.google.ads.googleads.v18.services.MutateCustomerManagerLinkResponse;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.UserCredentials;
import com.google.protobuf.FieldMask;
import org.adthena.adwordsmcc.model.LinkResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * Service for linking client accounts to manager accounts.
 */
@Service
public class AccountLinkService {

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    @Value("${google.ads.manager-id}")
    private String managerId;

    /**
     * Sends an invitation from the manager account to a client account.
     *
     * @param clientCustomerId The ID of the client account
     * @return LinkResponse containing the result of the operation
     * @throws IOException if there's an error communicating with the API
     */
    public LinkResponse sendInvitation(long clientCustomerId) throws IOException {
        // Use the manager ID from application.properties
        long managerIdValue = Long.parseLong(managerId);
        // Create credentials
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        // Build the Google Ads client with manager account as login customer ID
        GoogleAdsClient googleAdsClient = GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .setLoginCustomerId(managerIdValue)
            .build();

        // Create the operation to extend an invitation
        CustomerClientLinkOperation.Builder clientLinkOp = CustomerClientLinkOperation.newBuilder();
        clientLinkOp
            .getCreateBuilder()
            .setStatus(ManagerLinkStatus.PENDING)
            .setClientCustomer("customers/" + clientCustomerId);

        String pendingLinkResourceName;

        try (CustomerClientLinkServiceClient customerClientLinkServiceClient =
            googleAdsClient.getLatestVersion().createCustomerClientLinkServiceClient()) {
            MutateCustomerClientLinkResponse response =
                customerClientLinkServiceClient.mutateCustomerClientLink(
                    String.valueOf(managerIdValue), clientLinkOp.build());

            pendingLinkResourceName = response.getResult().getResourceName();

            return new LinkResponse(
                true,
                String.format(
                    "Extended an invitation from customer %s to customer %s with client link resource name %s",
                    managerIdValue, clientCustomerId, pendingLinkResourceName),
                pendingLinkResourceName);
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkResponse(
                false,
                "Failed to send invitation: " + e.getMessage(),
                null);
        }
    }

    /**
     * Accepts an invitation from a client account to the manager account.
     *
     * @param clientCustomerId The ID of the client account
     * @return LinkResponse containing the result of the operation
     * @throws IOException if there's an error communicating with the API
     */
    public LinkResponse acceptInvitation(long clientCustomerId) throws IOException {
        // Use the manager ID from application.properties
        long managerIdValue = Long.parseLong(managerId);
        // Create credentials
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        // First, build the Google Ads client with manager account as login customer ID
        // to find the manager_link_id
        GoogleAdsClient googleAdsClient = GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .setLoginCustomerId(managerIdValue)
            .build();

        // Find the pending link
        String query = String.format(
            "SELECT customer_client_link.manager_link_id FROM customer_client_link " +
                "WHERE customer_client_link.client_customer = 'customers/%s' " +
                "AND customer_client_link.status = 'PENDING'",
            clientCustomerId);

        Long managerLinkId = null;
        try (GoogleAdsServiceClient googleAdsServiceClient =
            googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            ServerStream<SearchGoogleAdsStreamResponse> stream =
                googleAdsServiceClient.searchStreamCallable().call(
                    SearchGoogleAdsStreamRequest.newBuilder()
                        .setCustomerId(String.valueOf(managerIdValue))
                        .setQuery(query)
                        .build());

            for (SearchGoogleAdsStreamResponse response : stream) {
                if (response.getResultsCount() > 0) {
                    GoogleAdsRow result = response.getResults(0);
                    managerLinkId = result.getCustomerClientLink().getManagerLinkId();
                    break;
                }
            }

            if (managerLinkId == null) {
                return new LinkResponse(
                    false,
                    "No pending invitation found from manager " + managerIdValue + " to client " + clientCustomerId,
                    null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkResponse(
                false,
                "Failed to find pending invitation: " + e.getMessage(),
                null);
        }

        // Create a new client with the client customer ID as the login customer ID
        GoogleAdsClient clientAdsClient = googleAdsClient.toBuilder()
            .setLoginCustomerId(clientCustomerId)
            .build();

        try (CustomerManagerLinkServiceClient clientManagerLinkServiceClient =
            clientAdsClient.getLatestVersion().createCustomerManagerLinkServiceClient()) {
            // Create a simple update operation with just the resource name and status
            CustomerManagerLinkOperation managerLinkOp = CustomerManagerLinkOperation.newBuilder()
                .setUpdateMask(FieldMask.newBuilder().addPaths("status").build())
                .setUpdate(
                    com.google.ads.googleads.v18.resources.CustomerManagerLink.newBuilder()
                        .setResourceName(String.format("customers/%s/customerManagerLinks/%s~%s", clientCustomerId, managerIdValue, managerLinkId))
                        .setStatus(ManagerLinkStatus.ACTIVE)
                        .build())
                .build();

            MutateCustomerManagerLinkResponse response =
                clientManagerLinkServiceClient.mutateCustomerManagerLink(
                    String.valueOf(clientCustomerId), Arrays.asList(managerLinkOp));

            return new LinkResponse(
                true,
                "Client accepted invitation with resource name " + response.getResults(0).getResourceName(),
                response.getResults(0).getResourceName());
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkResponse(
                false,
                "Failed to accept invitation: " + e.getMessage(),
                null);
        }
    }
}
