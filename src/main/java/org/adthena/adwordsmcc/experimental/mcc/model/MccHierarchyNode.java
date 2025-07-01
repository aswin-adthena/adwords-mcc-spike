package org.adthena.adwordsmcc.experimental.mcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the complete MCC hierarchy tree.
 * This model is used for experimental MCC traversal that can discover
 * accounts not directly accessible to the authenticated user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MccHierarchyNode {

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
     * The access level for this account.
     * Can be "Direct Access", "Via MCC", or "No Access"
     */
    private String accessLevel;

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
     * The ID of the MCC account through which this account was discovered.
     * Null for directly accessible accounts.
     */
    private String discoveredViaMccId;

    /**
     * List of child customer accounts under this manager account.
     * Empty for client accounts (leaf nodes).
     */
    private List<MccHierarchyNode> children = new ArrayList<>();

    /**
     * Constructor for creating a hierarchy node from basic customer information.
     */
    public MccHierarchyNode(String customerId, String resourceName, String descriptiveName, 
                           String accessLevel, String currencyCode, String timeZone, 
                           boolean isManager, int level, String discoveredViaMccId) {
        this.customerId = customerId;
        this.resourceName = resourceName;
        this.descriptiveName = descriptiveName;
        this.accessLevel = accessLevel;
        this.currencyCode = currencyCode;
        this.timeZone = timeZone;
        this.isManager = isManager;
        this.level = level;
        this.discoveredViaMccId = discoveredViaMccId;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a child node to this manager account.
     */
    public void addChild(MccHierarchyNode child) {
        this.children.add(child);
    }

    /**
     * Checks if this node has children.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Gets the account type for display purposes.
     */
    public String getAccountType() {
        return isManager ? "Manager" : "Client";
    }
}
