package org.adthena.adwordsmcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the Google Ads customer account hierarchy.
 * Each node contains customer information and references to child accounts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHierarchyNode {

    /**
     * The ID of the customer account.
     */
    private String customerId;

    /**
     * The resource name of the customer account.
     * Format: customers/{customer_id}
     */
    private String resourceName;

    /**
     * The descriptive name of the customer account.
     */
    private String descriptiveName;

    /**
     * The access role of the authenticated user for this customer account.
     */
    private String accessRole;

    /**
     * The currency code for this customer account.
     */
    private String currencyCode;

    /**
     * The time zone for this customer account.
     */
    private String timeZone;

    /**
     * Whether this customer account is a manager account.
     */
    private boolean isManager;

    /**
     * The level of this account in the hierarchy (0 = root, 1 = direct child, etc.).
     */
    private int level;

    /**
     * List of child customer accounts under this manager account.
     * Empty for client accounts (leaf nodes).
     */
    private List<CustomerHierarchyNode> children = new ArrayList<>();

    /**
     * Constructor for creating a hierarchy node from basic customer information.
     */
    public CustomerHierarchyNode(String customerId, String resourceName, String descriptiveName, 
                                String accessRole, String currencyCode, String timeZone, 
                                boolean isManager, int level) {
        this.customerId = customerId;
        this.resourceName = resourceName;
        this.descriptiveName = descriptiveName;
        this.accessRole = accessRole;
        this.currencyCode = currencyCode;
        this.timeZone = timeZone;
        this.isManager = isManager;
        this.level = level;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a child node to this manager account.
     */
    public void addChild(CustomerHierarchyNode child) {
        this.children.add(child);
    }

    /**
     * Returns whether this node has any children.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Returns the account type as a string for display purposes.
     */
    public String getAccountType() {
        return isManager ? "Manager" : "Client";
    }
}
