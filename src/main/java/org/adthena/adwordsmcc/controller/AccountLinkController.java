package org.adthena.adwordsmcc.controller;

import org.adthena.adwordsmcc.model.LinkResponse;
import org.adthena.adwordsmcc.service.AccountLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller for account linking operations.
 */
@RestController
@RequestMapping("/api/account-links")
public class AccountLinkController {

    private final AccountLinkService accountLinkService;

    @Autowired
    public AccountLinkController(AccountLinkService accountLinkService) {
        this.accountLinkService = accountLinkService;
    }

    /**
     * Sends an invitation from the manager account to a client account.
     *
     * @param clientCustomerId The ID of the client account
     * @return LinkResponse containing the result of the operation
     */
    @PostMapping("/send-invitation/{clientCustomerId}")
    public ResponseEntity<LinkResponse> sendInvitation(
        @PathVariable long clientCustomerId) {
        try {
            LinkResponse response = accountLinkService.sendInvitation(clientCustomerId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                new LinkResponse(false, "Error sending invitation: " + e.getMessage(), null));
        }
    }

    /**
     * Accepts an invitation from a client account to the manager account.
     *
     * @param clientCustomerId The ID of the client account
     * @return LinkResponse containing the result of the operation
     */
    @PostMapping("/accept-invitation/{clientCustomerId}")
    public ResponseEntity<LinkResponse> acceptInvitation(
        @PathVariable long clientCustomerId) {
        try {
            LinkResponse response = accountLinkService.acceptInvitation(clientCustomerId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                new LinkResponse(false, "Error accepting invitation: " + e.getMessage(), null));
        }
    }
}
