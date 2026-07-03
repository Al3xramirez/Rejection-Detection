package rejectiondetection.project.service;

import java.util.Optional;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import rejectiondetection.project.controller.AuthController;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.UsersRepository;
/* 
 * Service for managing authenticated users.
 * Ensures that a user exists in the database and retrieves the user entity.
 * If the user does not exist, a new user entity is created and saved.
 * Moved AuthController logic to this class to adhear to springboot conventions of service :)
 */
@Service
public class AuthenticatedUserService {

    private final AuthController authController;
    private final UsersRepository usersRepository;

    public AuthenticatedUserService(UsersRepository usersRepository, AuthController authController) {
        this.usersRepository = usersRepository;
        this.authController = authController;
    }
    // Optional is used here to indicate that the method may or may not return a UsersEntity.
    // This allows handling cases where the user is not authenticated or required attributes are missing.

    public Optional<UsersEntity> ensureAndGetUser(OAuth2User principal) {
        // principle is the auth user, if null it will return Optional.empty() indicating no authenticated user. 
        // If the principal is not null, the method will attempt to extract the user's email, Google sub, name, and picture.
        if (principal == null) {
            return Optional.empty();
        }

        //.getAttribute is a method call in OAuth2User that retrieves value of the specific attribute from the user info.
        String email = principal.getAttribute("email");
        String googleSub = principal.getAttribute("sub");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        // if email or google sub (which is the unique id for google accounts) is missing the method cannot be used to identify the user
        // This is more of a triple check, rarely needed honestly.
        if (email == null || googleSub == null) {
            return Optional.empty();
        }
        
        // Create a user entity if it does not exist or retrieve the existing user entity from the database.
        // This is because when a user logs in through gmail, it might be their first time, but if they've done it before
        // the user entity should be retrieved from the database to maintain consistency and avoid creating duplicate entries.
        UsersEntity user = usersRepository.findByGoogleSub(googleSub)
            .or(() -> usersRepository.findByEmail(email))
            .orElseGet(UsersEntity::new);

        // Set the user's attributes on the user entity
        user.setEmail(email);
        user.setGoogleSub(googleSub);
        user.setDisplayName(name);
        user.setPictureUrl(picture);

        return Optional.of(usersRepository.save(user));
    }
}
