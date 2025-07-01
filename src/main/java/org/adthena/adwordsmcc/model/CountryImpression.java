package org.adthena.adwordsmcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple model for country impression data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryImpression {
    private Long countryCriterionId;
    private Long impressions;
}
