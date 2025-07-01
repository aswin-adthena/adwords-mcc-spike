package org.adthena.adwordsmcc.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.resources.Customer;
import com.google.ads.googleads.v18.resources.CustomerUserAccess;
import com.google.ads.googleads.v18.services.CustomerServiceClient;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.ListAccessibleCustomersRequest;
import com.google.ads.googleads.v18.services.ListAccessibleCustomersResponse;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.adthena.adwordsmcc.model.CustomerHierarchyNode;
import org.adthena.adwordsmcc.model.GoogleAdsCustomer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with Google Ads Customer Service API. This service provides methods to list accessible customers for a user.
 */
@Service
public class CustomerService {

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private TokenService tokenService;

    /**
     * Lists all accessible Google Ads customers for the authenticated user.
     *
     * @return List of Google Ads customers
     * @throws IOException if there's an error communicating with the API
     */
    public List<GoogleAdsCustomer> listAccessibleCustomers() throws IOException {
        GoogleAdsClient googleAdsClient = createGoogleAdsClient();
        String userEmail = getUserEmail();

        List<GoogleAdsCustomer> customers = new ArrayList<>();

        try (CustomerServiceClient customerServiceClient =
            googleAdsClient.getLatestVersion().createCustomerServiceClient()) {

            ListAccessibleCustomersResponse response = getAccessibleCustomersResponse(customerServiceClient);
            customers = processCustomersWithAccessLevels(googleAdsClient, response, userEmail);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to list accessible Google Ads customers: " + e.getMessage(), e);
        }

        return customers;
    }

    private GoogleAdsClient createGoogleAdsClient() {
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        return GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .setLoginCustomerId(5798658445L)
            .build();
    }

    private String getUserEmail() throws IOException {
        return tokenService.getUserEmailFromToken();
    }

    private ListAccessibleCustomersResponse getAccessibleCustomersResponse(
            CustomerServiceClient customerServiceClient) {
        ListAccessibleCustomersRequest request = ListAccessibleCustomersRequest.newBuilder().build();
        return customerServiceClient.listAccessibleCustomers(request);
    }

    private List<GoogleAdsCustomer> processCustomersWithAccessLevels(
            GoogleAdsClient googleAdsClient,
            ListAccessibleCustomersResponse response,
            String userEmail) {

        List<GoogleAdsCustomer> customers = new ArrayList<>();

        for (String customerResourceName : response.getResourceNamesList()) {
            String customerId = extractCustomerIdFromResourceName(customerResourceName);
            String accessRole = getAccessRoleForCustomer(googleAdsClient, customerId, userEmail);

            GoogleAdsCustomer customer = new GoogleAdsCustomer(customerId, customerResourceName, accessRole);
            customers.add(customer);
        }

        return customers;
    }

    private String extractCustomerIdFromResourceName(String customerResourceName) {
        return customerResourceName.substring(customerResourceName.lastIndexOf('/') + 1);
    }

    private String getAccessRoleForCustomer(GoogleAdsClient googleAdsClient, String customerId, String userEmail) {
        try {
            long customerIdLong = Long.parseLong(customerId);
            Optional<CustomerUserAccess> userAccess = userAccessService.getCustomerUserAccess(
                googleAdsClient, customerIdLong, userEmail);

            return userAccess
                .map(access -> userAccessService.getAccessRoleDisplayName(access.getAccessRole()))
                .orElse("Unknown");
        } catch (Exception e) {
            return "Error";
        }
    }

    private List<GoogleAdsCustomer> getClientAccountsForManager(GoogleAdsClient googleAdsClient, String managerCustomerId, String userEmail) {
        List<GoogleAdsCustomer> clientAccounts = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient =
            googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer_client_link.client_customer, customer_client_link.status " +
                          "FROM customer_client_link " +
                          "WHERE customer_client_link.status = 'ACTIVE'";

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(managerCustomerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream = googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    String clientResourceName = row.getCustomerClientLink().getClientCustomer();
                    String clientCustomerId = extractCustomerIdFromResourceName(clientResourceName);
                    String accessRole = getAccessRoleForCustomer(googleAdsClient, clientCustomerId, userEmail);

                    GoogleAdsCustomer clientCustomer = new GoogleAdsCustomer(
                        clientCustomerId,
                        clientResourceName,
                        accessRole
                    );
                    clientAccounts.add(clientCustomer);
                }
            }
        } catch (Exception e) {
            // If we can't get client accounts, just log and continue
            System.err.println("Could not retrieve client accounts for manager " + managerCustomerId + ": " + e.getMessage());
        }

        return clientAccounts;
    }

    public List<CustomerHierarchyNode> getCustomerHierarchy() throws IOException {
        GoogleAdsClient googleAdsClient = createGoogleAdsClient();
        String userEmail = getUserEmail();

        List<CustomerHierarchyNode> hierarchy = new ArrayList<>();
        Map<String, CustomerHierarchyNode> customerMap = new HashMap<>();
        Map<String, String> clientToManagerMap = new HashMap<>();

        try (CustomerServiceClient customerServiceClient =
            googleAdsClient.getLatestVersion().createCustomerServiceClient()) {

            ListAccessibleCustomersResponse response = getAccessibleCustomersResponse(customerServiceClient);

            // First, get details for all directly accessible accounts
            for (String customerResourceName : response.getResourceNamesList()) {
                String customerId = extractCustomerIdFromResourceName(customerResourceName);
                CustomerHierarchyNode node = getCustomerDetails(googleAdsClient, customerId, userEmail);
                if (node != null) {
                    customerMap.put(customerId, node);

                    // If this is a client account, find its manager
                    if (!node.isManager()) {
                        String managerId = findManagerForClient(googleAdsClient, customerId);
                        if (managerId != null) {
                            clientToManagerMap.put(customerId, managerId);
                        }
                    }
                }
            }

            // Also check if any manager accounts have client relationships with our accessible accounts
            for (CustomerHierarchyNode managerNode : customerMap.values()) {
                if (managerNode.isManager()) {
                    List<String> clientIds = findClientsForManager(googleAdsClient, managerNode.getCustomerId());
                    for (String clientId : clientIds) {
                        if (customerMap.containsKey(clientId)) {
                            clientToManagerMap.put(clientId, managerNode.getCustomerId());
                            System.out.println("Found client-manager relationship: " + clientId + " -> " + managerNode.getCustomerId());
                        }
                    }
                }
            }

            hierarchy = buildHierarchyStructure(customerMap, clientToManagerMap);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to build customer hierarchy: " + e.getMessage(), e);
        }

        return hierarchy;
    }

    private CustomerHierarchyNode getCustomerDetails(GoogleAdsClient googleAdsClient, String customerId, String userEmail) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
            googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer.id, customer.descriptive_name, customer.currency_code, " +
                          "customer.time_zone, customer.manager FROM customer WHERE customer.id = " + customerId;

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(customerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream = googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    Customer customer = row.getCustomer();
                    String accessRole = getAccessRoleForCustomer(googleAdsClient, customerId, userEmail);

                    return new CustomerHierarchyNode(
                        customerId,
                        "customers/" + customerId,
                        customer.getDescriptiveName(),
                        accessRole,
                        customer.getCurrencyCode(),
                        customer.getTimeZone(),
                        customer.getManager(),
                        0
                    );
                }
            }
        } catch (Exception e) {
            String accessRole = getAccessRoleForCustomer(googleAdsClient, customerId, userEmail);
            return new CustomerHierarchyNode(
                customerId,
                "customers/" + customerId,
                "Unknown",
                accessRole,
                "USD",
                "UTC",
                false,
                0
            );
        }

        return null;
    }

    private String findManagerForClient(GoogleAdsClient googleAdsClient, String clientCustomerId) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
            googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer_manager_link.manager_customer, customer_manager_link.status " +
                          "FROM customer_manager_link " +
                          "WHERE customer_manager_link.status = 'ACTIVE'";

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(clientCustomerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream = googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    String managerResourceName = row.getCustomerManagerLink().getManagerCustomer();
                    return extractCustomerIdFromResourceName(managerResourceName);
                }
            }
        } catch (Exception e) {
            // If we can't find the manager, just continue
        }

        return null;
    }

    private List<String> findClientsForManager(GoogleAdsClient googleAdsClient, String managerCustomerId) {
        List<String> clientIds = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient =
            googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer_client_link.client_customer, customer_client_link.status " +
                          "FROM customer_client_link " +
                          "WHERE customer_client_link.status = ACTIVE";

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(managerCustomerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream = googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    String clientResourceName = row.getCustomerClientLink().getClientCustomer();
                    String clientId = extractCustomerIdFromResourceName(clientResourceName);
                    clientIds.add(clientId);
                }
            }
        } catch (Exception e) {
            // If we can't find clients, just continue
        }

        return clientIds;
    }

    private List<CustomerHierarchyNode> buildHierarchyStructure(Map<String, CustomerHierarchyNode> customerMap, Map<String, String> clientToManagerMap) {
        List<CustomerHierarchyNode> rootNodes = new ArrayList<>();

        // First, add all manager accounts as root nodes
        for (CustomerHierarchyNode node : customerMap.values()) {
            if (node.isManager()) {
                rootNodes.add(node);
            }
        }

        // Then, for each client account, try to add it under its manager
        for (Map.Entry<String, String> entry : clientToManagerMap.entrySet()) {
            String clientId = entry.getKey();
            String managerId = entry.getValue();

            CustomerHierarchyNode clientNode = customerMap.get(clientId);
            CustomerHierarchyNode managerNode = customerMap.get(managerId);

            if (clientNode != null && managerNode != null) {
                clientNode.setLevel(1); // Set client as level 1 under manager
                managerNode.addChild(clientNode);
            } else if (clientNode != null) {
                // If manager is not in our accessible accounts, add client as root
                rootNodes.add(clientNode);
            }
        }

        // Add any remaining client accounts that don't have a manager relationship
        for (CustomerHierarchyNode node : customerMap.values()) {
            if (!node.isManager() && !clientToManagerMap.containsKey(node.getCustomerId())) {
                rootNodes.add(node);
            }
        }

        return rootNodes;
    }
}
