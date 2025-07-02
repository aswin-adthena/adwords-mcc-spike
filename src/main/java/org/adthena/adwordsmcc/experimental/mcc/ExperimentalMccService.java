package org.adthena.adwordsmcc.experimental.mcc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ads.googleads.lib.GoogleAdsClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Experimental service for optimized MCC hierarchy retrieval.
 * Uses single customer_client queries per MCC to reduce API quota usage
 * and displays accounts in a simple flat structure.
 */
@Service
public class ExperimentalMccService {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentalMccService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    /**
     * Performs optimized complete MCC hierarchy retrieval using single queries per MCC.
     * This method eliminates recursive API calls to reduce quota usage.
     */
    public String getCompleteMccHierarchy() throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting optimized MCC hierarchy retrieval");

        try {
            // Step 1: Get all accounts the authenticated user has direct access to
            List<String> accessibleCustomerIds = getAccessibleCustomerIds();
            logger.info("Found {} directly accessible customers", accessibleCustomerIds.size());

            // Step 2: Identify MCC accounts from accessible customers
            List<String> mccIds = identifyMccAccounts(accessibleCustomerIds);
            logger.info("Found {} MCC accounts: {}", mccIds.size(), mccIds);

            // Step 3: For each MCC, fetch complete hierarchy using single query
            Map<String, Object> completeHierarchyData = new HashMap<>();
            completeHierarchyData.put("entryPointMccIds", mccIds);
            completeHierarchyData.put("directAccessCustomers", accessibleCustomerIds);

            List<Map<String, Object>> mccHierarchies = new ArrayList<>();
            int totalAccounts = 0;
            int totalErrors = 0;

            for (String mccId : mccIds) {
                try {
                    Map<String, Object> mccHierarchy = getDirectChildrenForMcc(mccId);
                    mccHierarchies.add(mccHierarchy);

                    Integer accountCount = (Integer) mccHierarchy.get("totalAccounts");
                    if (accountCount != null) {
                        totalAccounts += accountCount;
                    }
                } catch (Exception e) {
                    logger.warn("Error fetching hierarchy for MCC {}: {}", mccId, e.getMessage());
                    totalErrors++;
                }
            }

            completeHierarchyData.put("mccHierarchies", mccHierarchies);
            completeHierarchyData.put("totalAccounts", totalAccounts);
            completeHierarchyData.put("totalErrors", totalErrors);
            completeHierarchyData.put("traversalTimeMs", System.currentTimeMillis() - startTime);

            // Convert to JSON string
            String jsonResult = convertToJsonString(completeHierarchyData);

            logger.info("Optimized MCC hierarchy retrieval completed in {}ms. Found {} total accounts across {} MCCs with {} errors",
                       System.currentTimeMillis() - startTime, totalAccounts, mccIds.size(), totalErrors);

            return jsonResult;

        } catch (Exception e) {
            logger.error("Error during optimized MCC hierarchy retrieval", e);
            throw new IOException("Failed to retrieve MCC hierarchy: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all accessible customer IDs for the authenticated user.
     */
    private List<String> getAccessibleCustomerIds() throws IOException {
        List<String> customerIds = new ArrayList<>();
        GoogleAdsClient googleAdsClient = createGoogleAdsClient();

        try (CustomerServiceClient customerServiceClient =
             googleAdsClient.getLatestVersion().createCustomerServiceClient()) {

            ListAccessibleCustomersRequest request = ListAccessibleCustomersRequest.newBuilder().build();
            ListAccessibleCustomersResponse response = customerServiceClient.listAccessibleCustomers(request);

            for (String customerResourceName : response.getResourceNamesList()) {
                String customerId = extractCustomerIdFromResourceName(customerResourceName);
                customerIds.add(customerId);
            }
        }

        return customerIds;
    }

    /**
     * Identifies which of the accessible customers are MCC accounts.
     */
    private List<String> identifyMccAccounts(List<String> customerIds) throws IOException {
        List<String> mccIds = new ArrayList<>();
        GoogleAdsClient googleAdsClient = createGoogleAdsClient();

        for (String customerId : customerIds) {
            if (isManagerAccount(googleAdsClient, customerId)) {
                mccIds.add(customerId);
            }
        }

        return mccIds;
    }

    /**
     * Gets direct child customers for a specific MCC using a single customer_client query.
     * Returns only the immediate children (level 1) of the root MCC account.
     */
    private Map<String, Object> getDirectChildrenForMcc(String mccId) throws IOException {
        GoogleAdsClient googleAdsClient = createGoogleAdsClientWithLoginCustomer(mccId);
        Map<String, Object> hierarchyData = new HashMap<>();
        List<Map<String, Object>> accounts = new ArrayList<>();

        try (GoogleAdsServiceClient googleAdsServiceClient =
             googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {

            // Single query to get direct children only with enabled status filter
            String query = "SELECT " +
                          "customer_client.client_customer, " +
                          "customer_client.descriptive_name, " +
                          "customer_client.level, " +
                          "customer_client.manager, " +
                          "customer_client.currency_code, " +
                          "customer_client.time_zone, " +
                          "customer_client.id " +
                          "FROM customer_client " +
                          "WHERE customer_client.status = 'ENABLED' " +
                          "AND customer_client.level = 1";

            SearchGoogleAdsStreamRequest request = SearchGoogleAdsStreamRequest.newBuilder()
                .setCustomerId(mccId)
                .setQuery(query)
                .build();

            ServerStream<SearchGoogleAdsStreamResponse> stream =
                googleAdsServiceClient.searchStreamCallable().call(request);

            for (SearchGoogleAdsStreamResponse response : stream) {
                for (GoogleAdsRow row : response.getResultsList()) {
                    CustomerClient customerClient = row.getCustomerClient();

                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("customerId", String.valueOf(customerClient.getId()));
                    accountData.put("descriptiveName", customerClient.getDescriptiveName());
                    accountData.put("level", (int) customerClient.getLevel());
                    accountData.put("isManager", customerClient.getManager());
                    accountData.put("currencyCode", customerClient.getCurrencyCode());
                    accountData.put("timeZone", customerClient.getTimeZone());
                    accountData.put("resourceName", customerClient.getClientCustomer());
                    accountData.put("discoveredViaMccId", mccId);
                    // No children array for individual accounts in flat structure

                    accounts.add(accountData);
                }
            }

            // Build simple flat tree structure
            Map<String, Object> hierarchyTree = buildFlatTree(accounts, mccId);

            hierarchyData.put("mccId", mccId);
            hierarchyData.put("hierarchyTree", hierarchyTree);
            hierarchyData.put("totalAccounts", accounts.size());

        } catch (Exception e) {
            logger.warn("Error fetching hierarchy for MCC {}: {}", mccId, e.getMessage());
            hierarchyData.put("mccId", mccId);
            hierarchyData.put("hierarchyTree", new HashMap<>());
            hierarchyData.put("totalAccounts", 0);
            hierarchyData.put("error", e.getMessage());
        }

        return hierarchyData;
    }

    /**
     * Builds a simple flat tree structure where all accounts are direct children of the root MCC.
     * Applies sorting: manager accounts first, then client accounts, both alphabetically sorted.
     */
    private Map<String, Object> buildFlatTree(List<Map<String, Object>> accounts, String rootMccId) {
        // Sort accounts: managers first, then clients, both alphabetically by descriptive name
        List<Map<String, Object>> sortedAccounts = sortAccountsForDisplay(accounts);

        // Create virtual root for the queried MCC
        Map<String, Object> rootAccount = new HashMap<>();
        rootAccount.put("customerId", rootMccId);
        rootAccount.put("descriptiveName", "MCC Root (" + rootMccId + ")");
        rootAccount.put("level", 0);
        rootAccount.put("isManager", true);
        rootAccount.put("currencyCode", "");
        rootAccount.put("timeZone", "");
        rootAccount.put("resourceName", "customers/" + rootMccId);
        rootAccount.put("discoveredViaMccId", rootMccId);
        rootAccount.put("children", sortedAccounts);

        return rootAccount;
    }

    /**
     * Sorts accounts for optimal display: manager accounts first, then client accounts,
     * with case-sensitive alphabetical sorting by descriptive name within each group.
     * Uppercase letters are sorted before lowercase letters within each group.
     */
    private List<Map<String, Object>> sortAccountsForDisplay(List<Map<String, Object>> accounts) {
        return accounts.stream()
            .sorted((account1, account2) -> {
                Boolean isManager1 = (Boolean) account1.get("isManager");
                Boolean isManager2 = (Boolean) account2.get("isManager");
                String name1 = (String) account1.get("descriptiveName");
                String name2 = (String) account2.get("descriptiveName");

                // Handle null values
                if (isManager1 == null) isManager1 = false;
                if (isManager2 == null) isManager2 = false;
                if (name1 == null) name1 = "";
                if (name2 == null) name2 = "";

                // Primary sort: managers first (true > false, so we reverse the comparison)
                int managerComparison = Boolean.compare(isManager2, isManager1);
                if (managerComparison != 0) {
                    return managerComparison;
                }

                // Secondary sort: case-sensitive alphabetical by descriptive name
                // This naturally sorts uppercase letters before lowercase letters
                return name1.compareTo(name2);
            })
            .collect(Collectors.toList());
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
            logger.warn("Could not determine if customer {} is manager: {}", customerId, e.getMessage());
        }

        return false;
    }

    /**
     * Converts the hierarchy data to JSON string using Jackson ObjectMapper.
     */
    private String convertToJsonString(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("Error converting to JSON", e);
            return "{\"error\": \"Failed to convert data to JSON: " + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * Extracts customer ID from resource name.
     */
    private String extractCustomerIdFromResourceName(String customerResourceName) {
        return customerResourceName.substring(customerResourceName.lastIndexOf('/') + 1);
    }
}
