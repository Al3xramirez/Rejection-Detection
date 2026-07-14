package rejectiondetection.project.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rejectiondetection.project.dto.DashboardResponseDto;
import rejectiondetection.project.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // Single instance of DashboardService injected via constructor
    private final DashboardService dashboardService;

    // Constructor injection of the DashboardService
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Endpoint to get dashboard data for the authenticated user
    // Returns a DashboardResponseDto containing the user's dashboard information   
    // If user is not Authenticated, returns a 401 Unauthorized response with an error message
    @GetMapping
    public ResponseEntity<?> dashboard(@AuthenticationPrincipal OAuth2User principal) {
        return dashboardService.buildDashboard(principal)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Unauthenticated")));
    }
}
