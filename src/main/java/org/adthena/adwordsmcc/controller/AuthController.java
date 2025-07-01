package org.adthena.adwordsmcc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication-related endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    /**
     * Constructs the Google OAuth URL for authentication.
     *
     * @return A map containing the OAuth URL
     */
    @GetMapping("/google-oauth-url")
    public ResponseEntity<Map<String, String>> getGoogleOAuthUrl() throws UnsupportedEncodingException {

        // Encode the redirect URI
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());

        // Construct the OAuth URL with email scope to get user's email address for access level queries
        String oauthUrl = String.format(
            "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=https://www.googleapis.com/auth/adwords+email&state=state",
//            "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=https://www.googleapis.com/auth/adwords%%20email&state=state",
                clientId,
                encodedRedirectUri
        );


        // Create a map to hold the URL
        Map<String, String> response = new HashMap<>();
        response.put("url", oauthUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Exchanges an authorization code for an access token and refresh token.
     *
     * @param requestBody Map containing the authorization code
     * @return A map containing the access token, refresh token, and other OAuth information
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> exchangeCodeForToken(@RequestBody Map<String, String> requestBody) throws UnsupportedEncodingException {
        String code = requestBody.get("code");

        if (code == null || code.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authorization code is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }


        // Create the request to exchange the authorization code for tokens
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", redirectUri); // Don't URL encode here, the RestTemplate will handle it
        map.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            System.out.println("Refresh token: " + responseBody.get("refresh_token"));

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to exchange code for token");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
