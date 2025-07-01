package org.adthena.adwordsmcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Google Ads customer account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAdsCustomer {

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
     * The access role of the authenticated user for this customer account.
     */
    private String accessRole;
}
