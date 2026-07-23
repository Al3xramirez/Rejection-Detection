package rejectiondetection.project.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rejectiondetection.project.dto.DashboardResponseDto;
import rejectiondetection.project.service.DashboardService;
import rejectiondetection.project.service.GmailSyncService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // Single instance of DashboardService injected via constructor
    private final DashboardService dashboardService;
    private final GmailSyncService gmailSyncService;

    // Constructor injection of the DashboardService
    public DashboardController(DashboardService dashboardService, GmailSyncService gmailSyncService) {
        this.dashboardService = dashboardService;
        this.gmailSyncService = gmailSyncService;
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

    @PostMapping("/sync")
    public ResponseEntity<?> sync(@AuthenticationPrincipal OAuth2User principal) {
        GmailSyncService.ManualSyncResult result = gmailSyncService.runManualSync(principal);

        if (!result.success()) {
            return ResponseEntity.status(401).body(Map.of("error", result.message()));
        }

        return dashboardService.buildDashboard(principal)
            .<ResponseEntity<?>>map(dashboard -> ResponseEntity.ok(Map.of(
                "message", result.message(),
                "scannedCount", result.scannedCount(),
                "persistedCount", result.persistedCount(),
                "dashboard", dashboard
            )))
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Unauthenticated")));
    }
}
