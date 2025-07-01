package org.adthena.adwordsmcc.experimental.mcc;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.resources.Customer;
import com.google.ads.googleads.v18.resources.CustomerClient;
import com.google.ads.googleads.v18.services.CustomerServiceClient;
import com.google.ads.googleads.v18.services.GoogleAdsRow;
import com.google.ads.googleads.v18.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v18.services.ListAccessibleCustomersRequest;
import com.google.ads.googleads.v18.services.ListAccessibleCustomersResponse;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v18.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.UserCredentials;
import org.adthena.adwordsmcc.experimental.mcc.model.MccHierarchyNode;
import org.adthena.adwordsmcc.experimental.mcc.model.MccTraversalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Experimental service for complete MCC hierarchy traversal.
 * This service can discover accounts that the user doesn't have direct access to
 * by traversing through MCC accounts using the login-customer-id header.
 */
@Service
public class ExperimentalMccService {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentalMccService.class);

    private static final int MAX_TRAVERSAL_DEPTH = 10;
    private static final long TRAVERSAL_TIMEOUT_MS = 60000; // 1 minute timeout

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    /**
     * Performs complete MCC hierarchy traversal to discover all accounts
     * under accessible MCC accounts, even those without direct user access.
     */
    public MccTraversalResult getCompleteMccHierarchy() throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting experimental MCC hierarchy traversal");

        MccTraversalResult result = new MccTraversalResult(new ArrayList<>());
        Set<String> visitedAccounts = ConcurrentHashMap.newKeySet();
        Map<String, MccHierarchyNode> allNodes = new HashMap<>();

        try {
            // Step 1: Get accessible customers and identify MCC entry points
            List<String> entryPointMccIds = getAccessibleMccAccounts();
            result.getEntryPointMccIds().addAll(entryPointMccIds);

            logger.info("Found {} MCC entry points: {}", entryPointMccIds.size(), entryPointMccIds);

            // Step 2: For each MCC entry point, perform recursive traversal
            for (String mccId : entryPointMccIds) {
                if (!visitedAccounts.contains(mccId)) {
                    traverseMccHierarchy(mccId, mccId, 0, visitedAccounts, allNodes, result);
                }
            }

            // Step 3: Build the final hierarchy structure
            result.setHierarchy(buildHierarchyFromNodes(allNodes));

            // Step 4: Calculate statistics
            calculateTraversalStatistics(result, allNodes);

        } catch (Exception e) {
            logger.error("Error during MCC hierarchy traversal", e);
            result.incrementErrors();
            throw new IOException("Failed to traverse MCC hierarchy: " + e.getMessage(), e);
        } finally {
            long endTime = System.currentTimeMillis();
            result.setTraversalTimeMs(endTime - startTime);
            logger.info("MCC traversal completed: {}", result.getTraversalSummary());
        }

        return result;
    }

    /**
     * Gets accessible customer accounts and filters for MCC accounts.
     */
    private List<String> getAccessibleMccAccounts() throws IOException {
        List<String> mccIds = new ArrayList<>();
        GoogleAdsClient googleAdsClient = createGoogleAdsClient();

        try (CustomerServiceClient customerServiceClient =
             googleAdsClient.getLatestVersion().createCustomerServiceClient()) {

            ListAccessibleCustomersRequest request = ListAccessibleCustomersRequest.newBuilder().build();
            ListAccessibleCustomersResponse response = customerServiceClient.listAccessibleCustomers(request);

            for (String customerResourceName : response.getResourceNamesList()) {
                String customerId = extractCustomerIdFromResourceName(customerResourceName);

                // Check if this customer is a manager account
                if (isManagerAccount(googleAdsClient, customerId)) {
                    mccIds.add(customerId);
                    logger.debug("Found MCC account: {}", customerId);
                }
            }
        }

        return mccIds;
    }

    /**
     * Recursively traverses MCC hierarchy using the customer_client resource.
     */
    private void traverseMccHierarchy(String currentCustomerId, String loginCustomerId, int depth,
                                    Set<String> visitedAccounts, Map<String, MccHierarchyNode> allNodes,
                                    MccTraversalResult result) {

        if (depth > MAX_TRAVERSAL_DEPTH) {
            logger.warn("Maximum traversal depth reached for customer {}", currentCustomerId);
            return;
        }

        if (visitedAccounts.contains(currentCustomerId)) {
            return;
        }

        visitedAccounts.add(currentCustomerId);
        logger.debug("Traversing customer {} at depth {} via login customer {}",
                    currentCustomerId, depth, loginCustomerId);

        try {
            // Create GoogleAdsClient with login-customer-id header
            GoogleAdsClient googleAdsClient = createGoogleAdsClientWithLoginCustomer(loginCustomerId);

            // Get customer details
            MccHierarchyNode currentNode = getCustomerDetails(googleAdsClient, currentCustomerId,
                                                            loginCustomerId, depth);
            if (currentNode != null) {
                allNodes.put(currentCustomerId, currentNode);
                result.setMaxDepthReached(Math.max(result.getMaxDepthReached(), depth));
            }

            // Query for child accounts using customer_client resource
            List<CustomerClient> childAccounts = getChildAccounts(googleAdsClient, currentCustomerId);

            for (CustomerClient childAccount : childAccounts) {
                String childCustomerId = String.valueOf(childAccount.getId());

                if (!visitedAccounts.contains(childCustomerId)) {
                    // Recursively traverse child accounts
                    if (childAccount.getManager()) {
                        // For manager accounts, use them as the login customer for their subtree
                        traverseMccHierarchy(childCustomerId, childCustomerId, depth + 1,
                                           visitedAccounts, allNodes, result);
                    } else {
                        // For client accounts, continue using the current login customer
                        traverseMccHierarchy(childCustomerId, loginCustomerId, depth + 1,
                                           visitedAccounts, allNodes, result);
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Error traversing customer {}: {}", currentCustomerId, e.getMessage());
            result.incrementErrors();
            result.addInaccessibleAccount(currentCustomerId);
        }
    }

    /**
     * Creates a GoogleAdsClient with default credentials.
     */
    private GoogleAdsClient createGoogleAdsClient() {
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        return GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .build();
    }

    /**
     * Creates a GoogleAdsClient with a specific login-customer-id header.
     */
    private GoogleAdsClient createGoogleAdsClientWithLoginCustomer(String loginCustomerId) {
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

        return GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(developerToken)
            .setLoginCustomerId(Long.parseLong(loginCustomerId))
            .build();
    }

    /**
     * Checks if a customer account is a manager account.
     */
    private boolean isManagerAccount(GoogleAdsClient googleAdsClient, String customerId) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
             googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer.manager FROM customer WHERE customer.id = " + customerId;

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(customerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream =
                googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    return row.getCustomer().getManager();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine if customer {} is manager: {}", customerId, e.getMessage());
        }

        return false;
    }

    /**
     * Gets detailed information about a customer account.
     */
    private MccHierarchyNode getCustomerDetails(GoogleAdsClient googleAdsClient, String customerId,
                                              String discoveredViaMccId, int level) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
             googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer.id, customer.descriptive_name, customer.currency_code, " +
                          "customer.time_zone, customer.manager FROM customer WHERE customer.id = " + customerId;

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(customerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream =
                googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    Customer customer = row.getCustomer();

                    String accessLevel = customerId.equals(discoveredViaMccId) ? "Direct Access" : "Via MCC";

                    return new MccHierarchyNode(
                        customerId,
                        "customers/" + customerId,
                        customer.getDescriptiveName(),
                        accessLevel,
                        customer.getCurrencyCode(),
                        customer.getTimeZone(),
                        customer.getManager(),
                        level,
                        discoveredViaMccId
                    );
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get details for customer {}: {}", customerId, e.getMessage());

            // Return a basic node with unknown details
            String accessLevel = customerId.equals(discoveredViaMccId) ? "Direct Access" : "Via MCC";
            return new MccHierarchyNode(
                customerId,
                "customers/" + customerId,
                "Unknown",
                accessLevel,
                "USD",
                "UTC",
                false,
                level,
                discoveredViaMccId
            );
        }

        return null;
    }

    /**
     * Gets child accounts using the customer_client resource.
     */
    private List<CustomerClient> getChildAccounts(GoogleAdsClient googleAdsClient, String customerId) {
        List<CustomerClient> childAccounts = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient =
             googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            String query = "SELECT customer_client.client_customer, customer_client.level, " +
                          "customer_client.manager, customer_client.descriptive_name, " +
                          "customer_client.currency_code, customer_client.time_zone, " +
                          "customer_client.id FROM customer_client WHERE customer_client.level <= 1";

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(customerId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream =
                googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    CustomerClient customerClient = row.getCustomerClient();

                    // Skip the current customer (level 0)
                    if (customerClient.getLevel() == 0) {
                        continue;
                    }

                    childAccounts.add(customerClient);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get child accounts for customer {}: {}", customerId, e.getMessage());
        }

        return childAccounts;
    }

    /**
     * Builds the final hierarchy structure from the collected nodes.
     */
    private List<MccHierarchyNode> buildHierarchyFromNodes(Map<String, MccHierarchyNode> allNodes) {
        List<MccHierarchyNode> rootNodes = new ArrayList<>();
        Map<String, List<MccHierarchyNode>> parentToChildren = new HashMap<>();

        // Group nodes by their parent relationships
        for (MccHierarchyNode node : allNodes.values()) {
            if (node.getLevel() == 0) {
                rootNodes.add(node);
            } else {
                // For now, we'll add all non-root nodes as children of their discovered MCC
                String parentId = node.getDiscoveredViaMccId();
                parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
            }
        }

        // Build parent-child relationships
        for (MccHierarchyNode node : allNodes.values()) {
            List<MccHierarchyNode> children = parentToChildren.get(node.getCustomerId());
            if (children != null) {
                node.getChildren().addAll(children);
            }
        }

        return rootNodes;
    }

    /**
     * Calculates traversal statistics.
     */
    private void calculateTraversalStatistics(MccTraversalResult result, Map<String, MccHierarchyNode> allNodes) {
        result.setTotalAccountsDiscovered(allNodes.size());

        int directAccess = 0;
        int mccDiscovered = 0;

        for (MccHierarchyNode node : allNodes.values()) {
            if ("Direct Access".equals(node.getAccessLevel())) {
                directAccess++;
            } else {
                mccDiscovered++;
            }
        }

        result.setDirectAccessAccounts(directAccess);
        result.setMccDiscoveredAccounts(mccDiscovered);
    }

    /**
     * Extracts customer ID from resource name.
     */
    private String extractCustomerIdFromResourceName(String customerResourceName) {
        return customerResourceName.substring(customerResourceName.lastIndexOf('/') + 1);
    }
}
