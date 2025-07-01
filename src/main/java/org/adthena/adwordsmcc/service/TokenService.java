package org.adthena.adwordsmcc.service;

import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenService {

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.refresh-token}")
    private String refreshToken;

    public String getUserEmailFromToken() throws IOException {
        UserCredentials credentials = createUserCredentials();
        String accessToken = getAccessToken(credentials);
        return fetchUserEmailFromGoogleApi(accessToken);
    }

    private UserCredentials createUserCredentials() {
        return UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();
    }

    private String getAccessToken(UserCredentials credentials) throws IOException {
        credentials.refresh();
        return credentials.getAccessToken().getTokenValue();
    }

    private String fetchUserEmailFromGoogleApi(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> userInfo = response.getBody();
            return (String) userInfo.get("email");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user email from Google API", e);
        }
    }
}
