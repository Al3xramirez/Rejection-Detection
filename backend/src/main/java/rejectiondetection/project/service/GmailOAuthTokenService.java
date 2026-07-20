package rejectiondetection.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.GmailOAuthTokenRepository;

@Service
public class GmailOAuthTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailOAuthTokenService.class);

    // private instance of the GmailOAuthTokenRepository needed to manage Gmail OAuth tokens, injected via constructor
    private final GmailOAuthTokenRepository gmailOAuthTokenRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    // constructor
    public GmailOAuthTokenService(GmailOAuthTokenRepository gmailOAuthTokenRepository, ObjectMapper objectMapper) {
        this.gmailOAuthTokenRepository = gmailOAuthTokenRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    // Saves client token to db
    public void saveAuthorizedClient(UsersEntity user, OAuth2AuthorizedClient authorizedClient) {
        if (user == null || authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return;
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        GmailOAuthTokenEntity entity = gmailOAuthTokenRepository
            .findByUserIdAndRevokedAtIsNull(user.getId())
            .orElseGet(GmailOAuthTokenEntity::new);

        // Updates entity with new information from the authorized client. This will create a new record if one doesn't exist or update the existing active token for the user.
        entity.setUserId(user.getId());
        entity.setGoogleEmail(user.getEmail());
        entity.setAccessToken(accessToken.getTokenValue());
        entity.setTokenType(accessToken.getTokenType() == null ? null : accessToken.getTokenType().getValue());
        entity.setScope(String.join(" ", accessToken.getScopes()));
        entity.setAccessTokenExpiresAt(toLocalDateTime(accessToken.getExpiresAt()));
        entity.setRevokedAt(null);

        
        if (refreshToken != null && refreshToken.getTokenValue() != null && !refreshToken.getTokenValue().isBlank()) {
            entity.setRefreshToken(refreshToken.getTokenValue());
        }

        gmailOAuthTokenRepository.save(entity);
    }

    public Optional<GmailOAuthTokenEntity> findActiveTokenForUser(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return gmailOAuthTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
    }

    public GmailOAuthTokenEntity ensureValidAccessToken(GmailOAuthTokenEntity tokenEntity) {
        if (tokenEntity == null) {
            return null;
        }

        if (!isExpiringSoon(tokenEntity.getAccessTokenExpiresAt()) && tokenEntity.getAccessToken() != null) {
            return tokenEntity;
        }

        if (tokenEntity.getRefreshToken() == null || tokenEntity.getRefreshToken().isBlank()) {
            return tokenEntity;
        }

        return refreshAccessToken(tokenEntity);
    }

    // Checks if the user has an active Gmail OAuth token
    public boolean hasActiveToken(Long userId) {
        if (userId == null) {
            return false;
        }
        return gmailOAuthTokenRepository.existsByUserIdAndRevokedAtIsNull(userId);
    }

    private boolean isExpiringSoon(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return true;
        }

        return expiresAt.isBefore(LocalDateTime.now().plusMinutes(2));
    }

    private GmailOAuthTokenEntity refreshAccessToken(GmailOAuthTokenEntity tokenEntity) {
        try {
            String formBody = buildFormBody(
                "grant_type", "refresh_token",
                "client_id", googleClientId,
                "client_secret", googleClientSecret,
                "refresh_token", tokenEntity.getRefreshToken()
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Failed to refresh Gmail access token for userId={} statusCode={}", tokenEntity.getUserId(), response.statusCode());
                return tokenEntity;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String accessToken = textOrNull(root.path("access_token"));
            if (accessToken == null || accessToken.isBlank()) {
                LOGGER.warn("Token refresh response missing access_token for userId={}", tokenEntity.getUserId());
                return tokenEntity;
            }

            tokenEntity.setAccessToken(accessToken);
            tokenEntity.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(root.path("expires_in").asLong(3600L)));

            String tokenType = textOrNull(root.path("token_type"));
            if (tokenType != null) {
                tokenEntity.setTokenType(tokenType);
            }

            String scope = textOrNull(root.path("scope"));
            if (scope != null) {
                tokenEntity.setScope(scope);
            }

            gmailOAuthTokenRepository.save(tokenEntity);
            return tokenEntity;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Exception while refreshing Gmail access token for userId={}", tokenEntity.getUserId(), ex);
            return tokenEntity;
        }
    }

    private String buildFormBody(String... pairs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                builder.append('&');
            }
            builder.append(encode(pairs[i])).append('=').append(encode(pairs[i + 1]));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}