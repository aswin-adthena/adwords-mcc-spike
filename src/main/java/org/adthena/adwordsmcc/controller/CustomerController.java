package org.adthena.adwordsmcc.controller;

import org.adthena.adwordsmcc.model.CustomerHierarchyNode;
import org.adthena.adwordsmcc.model.GoogleAdsCustomer;
import org.adthena.adwordsmcc.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controller for Google Ads customer-related endpoints.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Lists all accessible Google Ads customers for the authenticated user.
     *
     * @return List of Google Ads customers
     */
    @GetMapping
    public ResponseEntity<List<GoogleAdsCustomer>> listAccessibleCustomers() {
        try {
            // Get the customers using the Customer service
            List<GoogleAdsCustomer> customers = customerService.listAccessibleCustomers();

            return ResponseEntity.ok(customers);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Gets the hierarchical structure of Google Ads customer accounts.
     *
     * @return List of CustomerHierarchyNode representing the account hierarchy
     */
    @GetMapping("/hierarchy")
    public ResponseEntity<List<CustomerHierarchyNode>> getCustomerHierarchy() {
        try {
            List<CustomerHierarchyNode> hierarchy = customerService.getCustomerHierarchy();
            return ResponseEntity.ok(hierarchy);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}
