package rejectiondetection.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.service.AuthenticatedUserService;

/**
 * Controller class for handling authentication-related endpoints.
 * Provides endpoints to initiate Google OAuth2 login and check session state.
 */
@Controller
public class AuthController {

    private final AuthenticatedUserService authenticatedUserService;

    public AuthController(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    /**
     * Redirects the browser to the Google OAuth2 authorization page.
     */
    @GetMapping("/api/auth/google")
    public String startGoogleOauth() {
        return "redirect:/oauth2/authorization/google";
    }

    /**
     * Returns the currently authenticated user's info from the active session.
     * Returns 401 if no valid session cookie (JSESSIONID) is present.
     * The frontend uses this endpoint to check whether the user is logged in.
     */
    @GetMapping("/api/auth/me")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User principal) {

        Optional<UsersEntity> maybeUser = authenticatedUserService.ensureAndGetUser(principal);
        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        UsersEntity user = maybeUser.get();
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("name", user.getDisplayName());
        response.put("picture", user.getPictureUrl());
        response.put("userId", user.getId());

        return ResponseEntity.ok(response);
    }
}
