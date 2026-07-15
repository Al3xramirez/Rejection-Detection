package rejectiondetection.project.dto;

import java.time.LocalDateTime;

public record ParsedEmailPayloadDto(
    String gmailMessageId,
    String threadId,
    String fromAddress,
    String subject,
    String body,
    String companyName,
    LocalDateTime receivedAt
) {
}