package rejectiondetection.project.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;

@Repository
public interface GmailOAuthTokenRepository extends JpaRepository<GmailOAuthTokenEntity, Long> {

    // Finds the active (non-revoked) Gmail OAuth token for a specific user. This is essential for determining if we can sync a user's mailbox.
    Optional<GmailOAuthTokenEntity> findByUserIdAndRevokedAtIsNull(Long userId);

    // Finds all active (non-revoked) Gmail OAuth tokens. This is used to identify all users who are eligible for mailbox sync.
    List<GmailOAuthTokenEntity> findAllByRevokedAtIsNull();

    // Checks if a specific user has an active (non-revoked) Gmail OAuth token. This is used to quickly determine if a user can sync their mailbox.
    boolean existsByUserIdAndRevokedAtIsNull(Long userId);
}