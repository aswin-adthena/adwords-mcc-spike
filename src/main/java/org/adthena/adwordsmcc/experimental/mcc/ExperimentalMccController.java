package org.adthena.adwordsmcc.experimental.mcc;

import org.adthena.adwordsmcc.experimental.mcc.model.MccTraversalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Experimental REST controller for complete MCC hierarchy traversal.
 * This controller provides endpoints for discovering complete MCC hierarchies,
 * including accounts that the user doesn't have direct access to.
 */
@RestController
@RequestMapping("/api/experimental/mcc")
@CrossOrigin(origins = "http://localhost:3000")
public class ExperimentalMccController {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentalMccController.class);

    @Autowired
    private ExperimentalMccService experimentalMccService;

    /**
     * Gets the complete MCC hierarchy by traversing through all accessible MCC accounts.
     * This endpoint can discover accounts that the user doesn't have direct access to
     * by using the login-customer-id header to authenticate through parent MCC accounts.
     *
     * @return MccTraversalResult containing the complete hierarchy and traversal metadata
     */
    @GetMapping("/complete-hierarchy")
    public ResponseEntity<MccTraversalResult> getCompleteMccHierarchy() {
        logger.info("Received request for complete MCC hierarchy traversal");
        
        try {
            MccTraversalResult result = experimentalMccService.getCompleteMccHierarchy();
            
            logger.info("Successfully completed MCC hierarchy traversal: {}", result.getTraversalSummary());
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("Failed to traverse MCC hierarchy", e);
            return ResponseEntity.status(500).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error during MCC hierarchy traversal", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Health check endpoint for the experimental MCC service.
     *
     * @return Simple status response
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Experimental MCC service is running");
    }
}
