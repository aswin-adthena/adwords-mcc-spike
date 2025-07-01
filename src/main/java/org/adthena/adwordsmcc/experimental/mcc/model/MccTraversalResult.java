package org.adthena.adwordsmcc.experimental.mcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for MCC traversal results with metadata.
 * Contains the complete hierarchy tree and statistics about the traversal process.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MccTraversalResult {

    /**
     * The complete MCC hierarchy tree.
     */
    private List<MccHierarchyNode> hierarchy;

    /**
     * Total number of accounts discovered during traversal.
     */
    private int totalAccountsDiscovered;

    /**
     * Number of accounts with direct access.
     */
    private int directAccessAccounts;

    /**
     * Number of accounts discovered via MCC traversal.
     */
    private int mccDiscoveredAccounts;

    /**
     * Number of errors encountered during traversal.
     */
    private int errorsEncountered;

    /**
     * List of customer IDs that could not be accessed.
     */
    private List<String> inaccessibleAccounts;

    /**
     * Maximum depth reached during traversal.
     */
    private int maxDepthReached;

    /**
     * Time taken for traversal in milliseconds.
     */
    private long traversalTimeMs;

    /**
     * List of MCC accounts used as entry points for traversal.
     */
    private List<String> entryPointMccIds;

    /**
     * Constructor with basic hierarchy data.
     */
    public MccTraversalResult(List<MccHierarchyNode> hierarchy) {
        this.hierarchy = hierarchy != null ? hierarchy : new ArrayList<>();
        this.inaccessibleAccounts = new ArrayList<>();
        this.entryPointMccIds = new ArrayList<>();
    }

    /**
     * Adds an inaccessible account to the list.
     */
    public void addInaccessibleAccount(String customerId) {
        if (inaccessibleAccounts == null) {
            inaccessibleAccounts = new ArrayList<>();
        }
        inaccessibleAccounts.add(customerId);
    }

    /**
     * Adds an entry point MCC ID.
     */
    public void addEntryPointMccId(String mccId) {
        if (entryPointMccIds == null) {
            entryPointMccIds = new ArrayList<>();
        }
        entryPointMccIds.add(mccId);
    }

    /**
     * Increments the error counter.
     */
    public void incrementErrors() {
        this.errorsEncountered++;
    }

    /**
     * Gets a summary of the traversal results.
     */
    public String getTraversalSummary() {
        return String.format(
            "Traversal completed in %d ms. Found %d total accounts (%d direct access, %d via MCC). " +
            "Max depth: %d. Errors: %d. Inaccessible: %d.",
            traversalTimeMs, totalAccountsDiscovered, directAccessAccounts, 
            mccDiscoveredAccounts, maxDepthReached, errorsEncountered, 
            inaccessibleAccounts != null ? inaccessibleAccounts.size() : 0
        );
    }
}
