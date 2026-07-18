package rejectiondetection.project.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.GmailOAuthTokenRepository;

@Service
public class GmailOAuthTokenService {

    // private instance of the GmailOAuthTokenRepository needed to manage Gmail OAuth tokens, injected via constructor
    private final GmailOAuthTokenRepository gmailOAuthTokenRepository;

    // constructor
    public GmailOAuthTokenService(GmailOAuthTokenRepository gmailOAuthTokenRepository) {
        this.gmailOAuthTokenRepository = gmailOAuthTokenRepository;
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

    // Checks if the user has an active Gmail OAuth token
    public boolean hasActiveToken(Long userId) {
        if (userId == null) {
            return false;
        }
        return gmailOAuthTokenRepository.existsByUserIdAndRevokedAtIsNull(userId);
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}