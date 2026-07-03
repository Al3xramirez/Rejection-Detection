package rejectiondetection.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import rejectiondetection.project.entity.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    Optional<UsersEntity> findByEmail(String email);
    Optional<UsersEntity> findByGoogleSub(String googleSub);
    
    // The methods above allow finding a user by their email or Google sub (unique identifier).
    // This is useful for authentication and ensuring that a user exists in the database based on their OAuth2 information.
    
    
}
