package org.adthena.adwordsmcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for account linking operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkResponse {
    
    /**
     * Whether the operation was successful.
     */
    private boolean success;
    
    /**
     * A message describing the result of the operation.
     */
    private String message;
    
    /**
     * The resource name of the created or updated link, if applicable.
     */
    private String resourceName;
}
