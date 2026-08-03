package rejectiondetection.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.service.AuthenticatedUserService;
import rejectiondetection.project.service.GmailOAuthTokenService;

/**
 * Controller class for handling authentication-related endpoints.
 * Provides endpoints to initiate Google OAuth2 login and check session state.
 */
@Controller
public class AuthController {

    // Single instance of AuthenticatedUserService & GmailOAuthTokenService injected via constructor
    private final AuthenticatedUserService authenticatedUserService;
    private final GmailOAuthTokenService gmailOAuthTokenService;

    // Constructor 
    public AuthController(

        AuthenticatedUserService authenticatedUserService, GmailOAuthTokenService gmailOAuthTokenService) {
        this.authenticatedUserService = authenticatedUserService;
        this.gmailOAuthTokenService = gmailOAuthTokenService;
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
     * RegisteredOAuth is used to get the authorized client which contains the access token, this is needed to save the token to the database for later use with Gmail API calls.
     * AuthenticationPrincipal is used to get the user's info from the active session, this is used to check if the user is authenticated and to return the user's info to the frontend.
     * These are distinct because the user could be authenticated (have a valid session) but not have an active Gmail OAuth token (if they haven't completed the OAuth flow or if the token was revoked/expired).
     */
    @GetMapping("/api/auth/me")
    @ResponseBody 
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User principal, @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
    
        Optional<UsersEntity> maybeUser = authenticatedUserService.ensureAndGetUser(principal);
        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        UsersEntity user = maybeUser.get();
        gmailOAuthTokenService.saveAuthorizedClient(user, authorizedClient); // Save or update the user's Gmail OAuth token in the database

        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("name", user.getDisplayName());
        response.put("picture", user.getPictureUrl());
        response.put("userId", user.getId());
        response.put("gmailTokenStored", gmailOAuthTokenService.hasActiveToken(user.getId())); // insert info of gmail token in db

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
