package rejectiondetection.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Controller class for handling authentication-related endpoints.
 * Provides endpoints to initiate Google OAuth2 login and check session state.
 */
@Controller
public class AuthController {

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
    public ResponseEntity<Map<String, Object>> me(
            @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "email", principal.getAttribute("email"),
                "name",  principal.getAttribute("name"),
                "picture", principal.getAttribute("picture")
        ));
    }
}
