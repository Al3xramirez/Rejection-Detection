package rejectiondetection.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import rejectiondetection.project.dto.GmailMessageEnvelopeDto;
import rejectiondetection.project.dto.ParsedEmailPayloadDto;
import rejectiondetection.project.entity.EmailResultsEntity;
import rejectiondetection.project.entity.GmailOAuthTokenEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.EmailResultsRepository;
import rejectiondetection.project.repository.GmailOAuthTokenRepository;
import rejectiondetection.project.repository.UsersRepository;
/**
 * This service is responsible for syncing Gmail messages for users who have authorized access.
 * It fetches recent messages, parses their content, and uses the RejectionDetectionService to analyze the emails for potential rejection signals.
 * The results are then persisted in the database if they meet certain criteria.
 * Admittingly, most of this service was written by AI. It is a bit of a mess, but it works for now.
 */
    
@Service
public class GmailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailSyncService.class);

    private final UsersRepository usersRepository;
    private final GmailOAuthTokenRepository gmailOAuthTokenRepository;
    private final EmailResultsRepository emailResultsRepository;
    private final RejectionDetectionService rejectionDetectionService;
    private final AuthenticatedUserService authenticatedUserService;
    private final GmailOAuthTokenService gmailOAuthTokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Constants for sync behavior, sets initial sync lookback to 90 days and limits the maximum number of messages fetched in a single request to 100.
    // These help control the amount of data processed as for one api call, we are fetching a multiple number of messages which can overwhelm the system if not limited.
    private static final int INITIAL_SYNC_LOOKBACK_DAYS = 90;
    private static final int MAX_MESSAGE_FETCH_LIMIT = 100;


    private static final Set<String> ALWAYS_PERSIST_STATUSES = Set.of(
        RejectionDetectionService.STATUS_REJECTION,
        RejectionDetectionService.STATUS_POSSIBLE_REJECTION,
        RejectionDetectionService.STATUS_NOT_REJECTION
    );

    // constructor injection for dependencies
    public GmailSyncService(
        UsersRepository usersRepository,
        GmailOAuthTokenRepository gmailOAuthTokenRepository,
        EmailResultsRepository emailResultsRepository,
        RejectionDetectionService rejectionDetectionService,
        AuthenticatedUserService authenticatedUserService,
        GmailOAuthTokenService gmailOAuthTokenService,
        ObjectMapper objectMapper
    ) {
        this.usersRepository = usersRepository;
        this.gmailOAuthTokenRepository = gmailOAuthTokenRepository;
        this.emailResultsRepository = emailResultsRepository;
        this.rejectionDetectionService = rejectionDetectionService;
        this.authenticatedUserService = authenticatedUserService;
        this.gmailOAuthTokenService = gmailOAuthTokenService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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

    // This method is called when a user triggers a manual sync from the frontend button
    // It will fetch recent messages since the last sync or if it's the first sync, it will fetch messages from the last 90 days.
    public ManualSyncResult runManualSync(OAuth2User principal) {
        Optional<UsersEntity> maybeUser =
            authenticatedUserService.ensureAndGetUser(principal);

        if (maybeUser.isEmpty()) {
            return new ManualSyncResult(
                0,
                0,
                "Unauthenticated",
                false
            );
        }

        UsersEntity user = maybeUser.get();
        GmailOAuthTokenEntity token = resolveSyncToken(user);

        if (token == null) {
            return new ManualSyncResult(
                0,
                0,
                "No active Gmail token found",
                false
            );
        }

        LocalDateTime syncStartedAt = LocalDateTime.now();

        final List<GmailMessageEnvelopeDto> messages;

        try {
            messages = pullRecentMessagesForUser(token);
        } catch (RuntimeException ex) {
            LOGGER.error(
                "Manual Gmail sync failed for user={}",
                user.getEmail(),
                ex
            );

            // Do not update lastSyncAt when Gmail failed.
            return new ManualSyncResult(
                0,
                0,
                "Gmail API request failed. Check backend logs.",
                false
            );
        }

        int persistedCount = 0;

        LOGGER.info(
            "Manual Gmail sync user={} fetchedMessageCount={}",
            user.getEmail(),
            messages.size()
        );

        for (GmailMessageEnvelopeDto message : messages) {
            ParsedEmailPayloadDto payload = parseMessagePayload(message);

            if (persistDetectionResult(user, payload)) {
                persistedCount++;
            }
        }

        // Only advance the cursor after a successful Gmail request.
        token.setLastSyncAt(syncStartedAt);
        gmailOAuthTokenRepository.save(token);

        return new ManualSyncResult(
            messages.size(),
            persistedCount,
            "Manual sync completed",
            true
        );
}

    // Lists users who have authorized Gmail access and are candidates for syncing.
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

    // Syncs the mailbox for a specific user by fetching recent messages, parsing them, and persisting detection results.
    private void syncUserMailbox(UsersEntity user) {
        String userLabel = user.getEmail() == null ? "unknown-email" : user.getEmail();
        GmailOAuthTokenEntity token = resolveSyncToken(user);
        if (token == null) {
            LOGGER.info("Skipping Gmail sync for user={} because no active token was found.", userLabel);
            return;
        }

        LocalDateTime syncStartedAt = LocalDateTime.now();
        List<GmailMessageEnvelopeDto> messages = pullRecentMessagesForUser(token);
        LOGGER.info("Gmail sync candidate user={} messageCount={}", userLabel, messages.size());

        for (GmailMessageEnvelopeDto message : messages) {
            ParsedEmailPayloadDto payload = parseMessagePayload(message);
            LOGGER.debug(
                "Parsed Gmail payload user={} messageId={} from={} subject={}",
                userLabel,
                payload.gmailMessageId(),
                payload.fromAddress(),
                payload.subject()
            );

            persistDetectionResult(user, payload);
        }

        token.setLastSyncAt(syncStartedAt);
        gmailOAuthTokenRepository.save(token);
    }

    // Persists the detection result for a given user and email payload if it meets the criteria for storage.
    private boolean persistDetectionResult(UsersEntity user, ParsedEmailPayloadDto payload) {

        // Skip processing if the Gmail message ID is missing
        if (payload.gmailMessageId() == null || payload.gmailMessageId().isBlank()) {
            LOGGER.warn("Skipping Gmail payload for userId={} because message id is missing.", user.getId());
            return false;
        }
        // Check if the email has already been processed to avoid duplicate entries
        if (emailResultsRepository.existsByUserIdAndGmailMessageId(user.getId(), payload.gmailMessageId())) {
            LOGGER.debug(
                "Skipping already-processed Gmail message userId={} messageId={}",
                user.getId(),
                payload.gmailMessageId()
            );
            return false;
        }

        RejectionDetectionService.DetectionResult result = rejectionDetectionService.detect(
            payload.fromAddress(),
            payload.subject(),
            payload.body()
        );

        if (!shouldPersistResult(result)) {
            LOGGER.debug(
                "Skipping persistence for message userId={} messageId={} status={} to avoid storing low-signal emails.",
                user.getId(),
                payload.gmailMessageId(),
                result.status()
            );
            return false;
        }

        // sets the fields of EmailResultsEntity into the database
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
        return true;
    }

    // Determines whether the detection result should be persisted (stored in the database) based on its status.
    private boolean shouldPersistResult(RejectionDetectionService.DetectionResult result) {
        if (result == null || result.status() == null || result.status().isBlank()) {
            return false;
        }

        if (ALWAYS_PERSIST_STATUSES.contains(result.status())) {
            return true;
        }

        return false;
    }

    // Pulls recent Gmail messages for a specific user using their OAuth token.
    private GmailOAuthTokenEntity resolveSyncToken(UsersEntity user) {
        Optional<GmailOAuthTokenEntity> maybeToken = gmailOAuthTokenService.findActiveTokenForUser(user.getId());
        if (maybeToken.isEmpty()) {
            return null;
        }

        GmailOAuthTokenEntity token = gmailOAuthTokenService.ensureValidAccessToken(maybeToken.get());
        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            return null;
        }

        return token;
    }

    // Pulls recent Gmail messages for a specific user using their OAuth token, starting from the last 90 days when lastSyncAt is null.
    private List<GmailMessageEnvelopeDto> pullRecentMessagesForUser(GmailOAuthTokenEntity token) {
            
        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            return Collections.emptyList();
        }

        LocalDateTime syncCursor = token.getLastSyncAt();

        // First sync: search historical messages instead of starting
        // from the moment the OAuth token row was created.
        if (syncCursor == null) {
            syncCursor = LocalDateTime.now()
                .minusDays(INITIAL_SYNC_LOOKBACK_DAYS);
        }

        List<String> messageIds =
            listRecentMessageIds(token.getAccessToken(), syncCursor);

        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<GmailMessageEnvelopeDto> messages = new ArrayList<>();

        for (String messageId : messageIds) {
            GmailMessageEnvelopeDto envelope =
                fetchMessageEnvelope(token.getAccessToken(), messageId);

            if (envelope != null) {
                messages.add(envelope);
            }
        }

        return messages;
    }

    // Lists the IDs of recent Gmail messages for the authenticated user using the Gmail API.
    private List<String> listRecentMessageIds(String accessToken, LocalDateTime lastSyncAt) {
        try {
            List<String> ids = new ArrayList<>();

            String pageToken = null;
            do {
                URI requestUri = buildMessagesListUri(lastSyncAt, pageToken);
                HttpRequest request = HttpRequest.newBuilder(requestUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    LOGGER.error(  "Gmail messages.list failed. statusCode={} responseBody={}", response.statusCode(), response.body()
                    );

                    throw new IllegalStateException(
                        "Gmail API returned HTTP " + response.statusCode()
                    );  
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode messagesNode = root.path("messages");
                if (messagesNode.isArray()) {
                    for (JsonNode messageNode : messagesNode) {
                        String messageId = textOrNull(messageNode.path("id"));
                        if (messageId != null) {
                            ids.add(messageId);
                        }
                        if(ids.size() >= MAX_MESSAGE_FETCH_LIMIT) {
                            break;
                        }
                    }
                }

                pageToken = textOrNull(root.path("nextPageToken"));
            } while (pageToken != null && ids.size() < MAX_MESSAGE_FETCH_LIMIT);

            return ids;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Gmail message listing was interrupted",
                ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
            "Failed to list Gmail messages",
            ex
        );
        }
    }

    // Fetches the full Gmail message envelope (metadata) for a specific message ID using the Gmail API.
    private GmailMessageEnvelopeDto fetchMessageEnvelope(String accessToken, String messageId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId + "?format=full"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Failed to fetch Gmail message id={} statusCode={}", messageId, response.statusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode payload = root.path("payload");
            JsonNode headers = payload.path("headers");

            return new GmailMessageEnvelopeDto(
                textOrNull(root.path("id")),
                textOrNull(root.path("threadId")),
                textOrNull(root.path("historyId")),
                textOrNull(root.path("snippet")),
                toStringList(root.path("labelIds")),
                extractHeader(headers, "From"),
                extractRecipients(headers, "To"),
                extractRecipients(headers, "Cc"),
                extractHeader(headers, "Subject"),
                extractBodyText(payload),
                extractHtmlText(payload),
                parseLong(root.path("internalDate").asText(null))
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch Gmail message id={}", messageId, ex);
            return null;
        }
    }

    // Normalizes a string by removing diacritics, converting to lowercase, and replacing non-alphanumeric characters with spaces.
    private ParsedEmailPayloadDto parseMessagePayload(GmailMessageEnvelopeDto message) {
        return new ParsedEmailPayloadDto(
            message.gmailMessageId(),
            message.threadId(),
            blankToEmpty(message.fromAddress()),
            blankToEmpty(message.subject()),
            buildDetectionBody(message),
            inferCompanyName(message.fromAddress()),
            toLocalDateTime(message.internalTimestampMs())
        );
    }

    // Builds the body text for detection by prioritizing the plain text body, then the snippet, and finally returning an empty string if both are unavailable.
    private String buildDetectionBody(GmailMessageEnvelopeDto message) {
        if (message.bodyText() != null && !message.bodyText().isBlank()) {
            return message.bodyText().trim();
        }
        if (message.snippet() != null && !message.snippet().isBlank()) {
            return message.snippet().trim();
        }
        return "";
    }

    // Converts a confidence level string to a corresponding BigDecimal score for storage.
    private BigDecimal toConfidenceScore(String confidence) {
        if ("HIGH".equals(confidence)) {
            return BigDecimal.valueOf(0.90);
        }
        if ("MEDIUM".equals(confidence)) {
            return BigDecimal.valueOf(0.60);
        }
        return BigDecimal.valueOf(0.30);
    }

    // Infers the company name from the sender's email address.
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

    // Converts an internal timestamp in milliseconds to a LocalDateTime object in the system's default time zone.
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

    private String extractHeader(JsonNode headersNode, String headerName) {
        if (headersNode == null || !headersNode.isArray()) {
            return "";
        }

        for (JsonNode headerNode : headersNode) {
            if (headerName.equalsIgnoreCase(textOrNull(headerNode.path("name")))) {
                return blankToEmpty(textOrNull(headerNode.path("value")));
            }
        }

        return "";
    }

    // Extracts the list of recipient email addresses from the specified header (e.g., "To" or "Cc") in the Gmail message headers.
    private List<String> extractRecipients(JsonNode headersNode, String headerName) {
        String value = extractHeader(headersNode, headerName);
        if (value.isBlank()) {
            return List.of();
        }

        String[] recipients = value.split(",");
        List<String> result = new ArrayList<>();
        for (String recipient : recipients) {
            String trimmed = recipient.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    // Normalizes a string by removing diacritics, converting to lowercase, and replacing non-alphanumeric characters with spaces.
    private String extractBodyText(JsonNode payloadNode) {
        String body = findBodyByMimeType(payloadNode, "text/plain");
        if (!body.isBlank()) {
            return body;
        }

        body = findBodyByMimeType(payloadNode, "text/html");
        if (!body.isBlank()) {
            return body;
        }

        JsonNode bodyNode = payloadNode.path("body");
        String data = textOrNull(bodyNode.path("data"));
        return data == null ? "" : decodeBase64Url(data);
    }

    private String extractHtmlText(JsonNode payloadNode) {
        return findBodyByMimeType(payloadNode, "text/html");
    }

    private String findBodyByMimeType(JsonNode payloadNode, String mimeType) {
        if (payloadNode == null || payloadNode.isMissingNode()) {
            return "";
        }

        String currentMimeType = textOrNull(payloadNode.path("mimeType"));
        if (mimeType.equalsIgnoreCase(currentMimeType)) {
            JsonNode bodyNode = payloadNode.path("body");
            String data = textOrNull(bodyNode.path("data"));
            if (data != null) {
                return decodeBase64Url(data);
            }
        }

        JsonNode parts = payloadNode.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                String extracted = findBodyByMimeType(part, mimeType);
                if (!extracted.isBlank()) {
                    return extracted;
                }
            }
        }

        return "";
    }

    // Converts a JsonNode array to a list of strings, returning an empty list if the node is null or not an array.
    private List<String> toStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String value = textOrNull(node);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private String decodeBase64Url(String value) {
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    public record ManualSyncResult(int scannedCount, int persistedCount, String message, boolean success) {
    }

    // Builds the URI for listing Gmail messages, including optional query parameters for filtering by last sync time and pagination. 
    // maxResults is set to 100 to limit the number of messages fetched in a single request.
    private URI buildMessagesListUri(LocalDateTime lastSyncAt, String pageToken) {
        StringBuilder uri = new StringBuilder("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=100");

        if (lastSyncAt != null) {
            long epochSeconds = lastSyncAt.atZone(ZoneId.systemDefault()).toEpochSecond();
            uri.append("&q=").append(URLEncoder.encode("after:" + epochSeconds, StandardCharsets.UTF_8));
        }

        if (pageToken != null && !pageToken.isBlank()) {
            uri.append("&pageToken=").append(URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
        }

        return URI.create(uri.toString());
    }
}
