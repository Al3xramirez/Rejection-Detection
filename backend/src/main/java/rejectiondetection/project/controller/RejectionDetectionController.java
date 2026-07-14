package rejectiondetection.project.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import rejectiondetection.project.entity.EmailResultsEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.EmailResultsRepository;
import rejectiondetection.project.service.AuthenticatedUserService;
import rejectiondetection.project.service.RejectionDetectionService;

@RestController
@RequestMapping("/api/detection")
public class RejectionDetectionController {

    private final AuthenticatedUserService authenticatedUserService;
    private final EmailResultsRepository emailResultsRepository;
    private final RejectionDetectionService rejectionDetectionService;

    public RejectionDetectionController(
        AuthenticatedUserService authenticatedUserService,
        EmailResultsRepository emailResultsRepository,
        RejectionDetectionService rejectionDetectionService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.emailResultsRepository = emailResultsRepository;
        this.rejectionDetectionService = rejectionDetectionService;
    }

    // Endpoint to analyze then POSTMap the email content and return the detection result.
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
        @AuthenticationPrincipal OAuth2User principal,
        @RequestBody DetectionRequest request
    ) {
        // Uses OAuth principal to make sure user is authenticated
        // uses a requestBody to get content needed for analysis
        Optional<UsersEntity> maybeUser = authenticatedUserService.ensureAndGetUser(principal);
        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthenticated"));
        }

        // Calls the service to analyze the email content and determine if it's a rejection
        RejectionDetectionService.DetectionResult result = rejectionDetectionService.detect(
            request.fromAddress(),
            request.subject(),
            request.body()
        );

        // Saves the result along with the email metadata to the database
        EmailResultsEntity entity = new EmailResultsEntity();
        entity.setUserId(maybeUser.get().getId());
        entity.setGmailMessageId(resolveMessageId(request.gmailMessageId()));
        entity.setCompanyName(request.companyName());
        entity.setEmailSubject(request.subject());
        entity.setEmailSender(request.fromAddress());
        entity.setStatus(result.status());
        entity.setConfidenceScore(toConfidenceScore(result.confidence()));
        entity.setMatchedPhrase(result.reason());
        entity.setSummary(request.body());
        entity.setReceivedAt(request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt());

        // Save the entity to the database and return the result in the response
        EmailResultsEntity saved = emailResultsRepository.save(entity);

        // return a status of 200 with the detection result, including the ID of the saved entity, the determined status, reason, and confidence level.
        return ResponseEntity.ok(new DetectionResponse(
            saved.getId(),
            result.status(),
            result.reason(),
            result.confidence()
        ));
    }

    // Helper method to resolve the Gmail message ID, generating a unique one if not provided
    private String resolveMessageId(String providedMessageId) {
        if (providedMessageId == null || providedMessageId.isBlank()) {
            return "manual-" + UUID.randomUUID();
        }
        return providedMessageId;
    }

    // Helper method to convert confidence string to a numeric score
    private BigDecimal toConfidenceScore(String confidence) {
        if ("HIGH".equals(confidence)) {
            return BigDecimal.valueOf(0.90);
        }
        if ("MEDIUM".equals(confidence)) {
            return BigDecimal.valueOf(0.60);
        }
        return BigDecimal.valueOf(0.30);
    }

    // we could use a DTO here but this is just for simplicity right now
    public record DetectionRequest(
        String fromAddress,
        String subject,
        String body,
        String companyName,
        String gmailMessageId,
        LocalDateTime receivedAt
    ) {
    }

    public record DetectionResponse(Long id, String status, String reason, String confidence) {
    }
}
