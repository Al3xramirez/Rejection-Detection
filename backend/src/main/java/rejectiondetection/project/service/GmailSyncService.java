package rejectiondetection.project.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rejectiondetection.project.dto.GmailMessageEnvelope;
import rejectiondetection.project.dto.ParsedEmailPayload;
import rejectiondetection.project.entity.EmailResultsEntity;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.EmailResultsRepository;
import rejectiondetection.project.repository.GmailOAuthTokenRepository;
import rejectiondetection.project.repository.UsersRepository;

@Service
public class GmailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailSyncService.class);

    private final UsersRepository usersRepository;
    private final GmailOAuthTokenRepository gmailOAuthTokenRepository;
    private final EmailResultsRepository emailResultsRepository;
    private final RejectionDetectionService rejectionDetectionService;

    public GmailSyncService(
        UsersRepository usersRepository,
        GmailOAuthTokenRepository gmailOAuthTokenRepository,
        EmailResultsRepository emailResultsRepository,
        RejectionDetectionService rejectionDetectionService
    ) {
        this.usersRepository = usersRepository;
        this.gmailOAuthTokenRepository = gmailOAuthTokenRepository;
        this.emailResultsRepository = emailResultsRepository;
        this.rejectionDetectionService = rejectionDetectionService;
    }

    /**
     * Entry point called by the scheduler. This keeps the hourly flow in one place so we can
     * replace stub methods with real Gmail API logic incrementally.
     */
    public void runHourlySync() {
        List<UsersEntity> users = listSyncCandidates();
        LOGGER.info("Gmail sync run started for {} users.", users.size());

        for (UsersEntity user : users) {
            syncUserMailbox(user);
        }

        LOGGER.info("Gmail sync run completed.");
    }

    private List<UsersEntity> listSyncCandidates() {
        List<Long> userIds = gmailOAuthTokenRepository.findAllByRevokedAtIsNull().stream()
            .map(GmailOAuthTokenEntity::getUserId)
            .distinct()
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return List.of();
        }

        return usersRepository.findAllById(userIds);
    }

    private void syncUserMailbox(UsersEntity user) {
        String userLabel = user.getEmail() == null ? "unknown-email" : user.getEmail();

        // TODO: Replace with Gmail API messages.list + incremental sync support.
        List<GmailMessageEnvelope> messages = pullRecentMessagesForUser(user);
        LOGGER.info("Gmail sync candidate user={} messageCount={}", userLabel, messages.size());

        for (GmailMessageEnvelope message : messages) {
            ParsedEmailPayload payload = parseMessagePayload(message);
            LOGGER.debug(
                "Parsed Gmail payload user={} messageId={} from={} subject={}",
                userLabel,
                payload.gmailMessageId(),
                payload.fromAddress(),
                payload.subject()
            );

            persistDetectionResult(user, payload);
        }
    }

    private void persistDetectionResult(UsersEntity user, ParsedEmailPayload payload) {
        if (payload.gmailMessageId() == null || payload.gmailMessageId().isBlank()) {
            LOGGER.warn("Skipping Gmail payload for userId={} because message id is missing.", user.getId());
            return;
        }

        if (emailResultsRepository.existsByUserIdAndGmailMessageId(user.getId(), payload.gmailMessageId())) {
            LOGGER.debug(
                "Skipping already-processed Gmail message userId={} messageId={}",
                user.getId(),
                payload.gmailMessageId()
            );
            return;
        }

        RejectionDetectionService.DetectionResult result = rejectionDetectionService.detect(
            payload.fromAddress(),
            payload.subject(),
            payload.body()
        );

        EmailResultsEntity entity = new EmailResultsEntity();
        entity.setUserId(user.getId());
        entity.setGmailMessageId(payload.gmailMessageId());
        entity.setCompanyName(payload.companyName());
        entity.setEmailSubject(payload.subject());
        entity.setEmailSender(payload.fromAddress());
        entity.setStatus(result.status());
        entity.setConfidenceScore(toConfidenceScore(result.confidence()));
        entity.setMatchedPhrase(result.reason());
        entity.setSummary(payload.body());
        entity.setReceivedAt(payload.receivedAt() == null ? LocalDateTime.now() : payload.receivedAt());

        emailResultsRepository.save(entity);
    }

    private List<GmailMessageEnvelope> pullRecentMessagesForUser(UsersEntity user) {
        // TODO: Load the user's active token and call Gmail messages.list/messages.get.
        // TODO: Implement Gmail API call(s) using stored OAuth credentials.
        return Collections.emptyList();
    }

    private ParsedEmailPayload parseMessagePayload(GmailMessageEnvelope message) {
        return new ParsedEmailPayload(
            message.gmailMessageId(),
            message.threadId(),
            blankToEmpty(message.fromAddress()),
            blankToEmpty(message.subject()),
            buildDetectionBody(message),
            inferCompanyName(message.fromAddress()),
            toLocalDateTime(message.internalTimestampMs())
        );
    }

    private String buildDetectionBody(GmailMessageEnvelope message) {
        if (message.bodyText() != null && !message.bodyText().isBlank()) {
            return message.bodyText().trim();
        }
        if (message.snippet() != null && !message.snippet().isBlank()) {
            return message.snippet().trim();
        }
        return "";
    }

    private BigDecimal toConfidenceScore(String confidence) {
        if ("HIGH".equals(confidence)) {
            return BigDecimal.valueOf(0.90);
        }
        if ("MEDIUM".equals(confidence)) {
            return BigDecimal.valueOf(0.60);
        }
        return BigDecimal.valueOf(0.30);
    }

    private String inferCompanyName(String fromAddress) {
        if (fromAddress == null || fromAddress.isBlank() || !fromAddress.contains("@")) {
            return "";
        }

        String domain = fromAddress.substring(fromAddress.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        String[] parts = domain.split("\\.");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "";
        }

        String label = parts[0].replace('-', ' ').trim();
        if (label.isBlank()) {
            return "";
        }

        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private LocalDateTime toLocalDateTime(Long internalTimestampMs) {
        if (internalTimestampMs == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(internalTimestampMs),
            ZoneId.systemDefault()
        );
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
