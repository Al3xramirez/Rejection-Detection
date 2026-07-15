package rejectiondetection.project.dto;

import java.util.List;

public record GmailMessageEnvelopeDto(
    String gmailMessageId,
    String threadId,
    String historyId,
    String snippet,
    List<String> labelIds,
    String fromAddress,
    List<String> toAddresses,
    List<String> ccAddresses,
    String subject,
    String bodyText,
    String bodyHtml,
    Long internalTimestampMs
) {
    // This is a constructor which just ensures that the list of the fields are never null
    public GmailMessageEnvelopeDto {
        labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
        toAddresses = toAddresses == null ? List.of() : List.copyOf(toAddresses);
        ccAddresses = ccAddresses == null ? List.of() : List.copyOf(ccAddresses);
    }
}