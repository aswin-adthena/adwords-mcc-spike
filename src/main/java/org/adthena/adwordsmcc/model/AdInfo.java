package org.adthena.adwordsmcc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Simple model for ad information including final URLs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdInfo {
    private Long adId;
    private String adName;
    private List<String> finalUrls;
    private String status;
}
