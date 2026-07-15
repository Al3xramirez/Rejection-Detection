package rejectiondetection.project.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;

@Repository
public interface GmailOAuthTokenRepository extends JpaRepository<GmailOAuthTokenEntity, Long> {

    Optional<GmailOAuthTokenEntity> findByUserIdAndRevokedAtIsNull(Long userId);

    List<GmailOAuthTokenEntity> findAllByRevokedAtIsNull();

    boolean existsByUserIdAndRevokedAtIsNull(Long userId);
}