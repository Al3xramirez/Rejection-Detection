package rejectiondetection.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for handling authentication-related endpoints.
 * Currently provides an endpoint to initiate Google OAuth2 login.
 */
@Controller
public class AuthController {

    @GetMapping("/api/auth/google")
    public String startGoogleOauth() {
        return "redirect:/oauth2/authorization/google";
    }
}
