package org.adthena.adwordsmcc.experimental.mcc;

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
     * Gets the complete MCC hierarchy using optimized single queries per MCC.
     * This endpoint reduces API quota usage by eliminating recursive calls.
     * Returns raw JSON data for initial implementation to avoid serialization complexity.
     *
     * @return Raw JSON string containing the complete hierarchy data
     */
    @GetMapping("/complete-hierarchy")
    public ResponseEntity<String> getCompleteMccHierarchy() {
        logger.info("Received request for optimized MCC hierarchy retrieval");

        try {
            String jsonResult = experimentalMccService.getCompleteMccHierarchy();

            logger.info("Successfully completed optimized MCC hierarchy retrieval");
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(jsonResult);

        } catch (IOException e) {
            logger.error("Failed to retrieve MCC hierarchy", e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"Failed to retrieve MCC hierarchy: " + e.getMessage().replace("\"", "\\\"") + "\"}");

        } catch (Exception e) {
            logger.error("Unexpected error during MCC hierarchy retrieval", e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"Unexpected error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
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
