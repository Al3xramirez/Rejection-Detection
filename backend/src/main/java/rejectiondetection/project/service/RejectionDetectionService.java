package rejectiondetection.project.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RejectionDetectionService {

    public static final String STATUS_REJECTION = "REJECTION";
    public static final String STATUS_POSSIBLE_REJECTION = "POSSIBLE_REJECTION";
    public static final String STATUS_NOT_REJECTION = "NOT_REJECTION";
    public static final String STATUS_UNRELATED = "UNRELATED";

    private static final String CONFIDENCE_HIGH = "HIGH";
    private static final String CONFIDENCE_MEDIUM = "MEDIUM";
    private static final String CONFIDENCE_LOW = "LOW";

    private static final List<String> STRONG_REJECTION_PHRASES = List.of(
        "we regret to inform you",
        "unfortunately we will not be moving forward",
        "we have decided not to proceed",
        "position has been filled",
        "we chose another candidate",
        "not selected",
        "application was unsuccessful",
        "we will not be moving forward",
        "we have decided to move forward with other candidates",
        "we have decided to move forward with other applicants"
    );

    private static final List<String> MODERATE_REJECTION_PHRASES = List.of(
        "moving forward with other candidates",
        "wont be moving forward",
        "not the right fit at this time",
        "we will not be proceeding"
        
    );

    private static final List<String> HIRING_CONTEXT_KEYWORDS = List.of(
        "application",
        "role",
        "position",
        "interview",
        "candidate",
        "hiring team",
        "recruiter",
        "job",
        "offer",
        "next steps"
    );

    private static final List<String> POSITIVE_ADVANCEMENT_PHRASES = List.of(
        "we would like to schedule an interview",
        "we would like to invite you to interview",
        "we would like to move you to the next round",
        "we are excited to move forward with you",
        "we are happy to move forward with you",
        "we are pleased to offer you the position",
        "we are pleased to offer you the role",
        "we would like to offer you the position",
        "we would like to offer you the role",
        "we are happy to move forward with your application",
        "we are excited to move forward with your application",
        "you have been selected for the position",
        "you have been selected for the role"
    );

    private static final List<String> MARKETING_HINTS = List.of(
        "unsubscribe",
        "marketing",
        "promo",
        "newsletter",
        "advertisement",
        "special offer",
        "limited time offer",
        "exclusive deal",
        "sale",
        "discount"
    );

    public DetectionResult detect(String fromAddress, String subject, String body) {

        String normalizedSubject = normalize(subject);
        String normalizedBody = normalize(body);
        String normalizedSender = normalize(fromAddress);

        String fullText =
            (normalizedSubject + " " + normalizedBody).trim();

        boolean strongRejectionSignal =
            containsAnyNormalized(
                fullText,
                STRONG_REJECTION_PHRASES
            );

        boolean moderateRejectionSignal =
            containsAnyNormalized(
                fullText,
                MODERATE_REJECTION_PHRASES
            );

        boolean positiveSignal =
            containsAnyNormalized(
                fullText,
                POSITIVE_ADVANCEMENT_PHRASES
            );

        boolean marketingSignal =
            containsAnyNormalized(
                fullText,
                MARKETING_HINTS
            );

        boolean hiringContextDetected =
            containsAnyNormalized(
                fullText,
                HIRING_CONTEXT_KEYWORDS
            )
            || senderLooksLikeRecruiting(normalizedSender);


        // 1. Strong rejection
        if (strongRejectionSignal) {
            return new DetectionResult(
                STATUS_REJECTION,
                firstMatchNormalized(
                    fullText,
                    STRONG_REJECTION_PHRASES
                ),
                CONFIDENCE_HIGH
            );
        }

         // 4. Marketing
        if (marketingSignal) {
            return new DetectionResult(
                STATUS_UNRELATED,
                "Likely marketing content",
                CONFIDENCE_LOW
            );
        }

        // 3. Weaker rejection wording
        if (moderateRejectionSignal) {
            return new DetectionResult(
                STATUS_POSSIBLE_REJECTION,
                firstMatchNormalized(
                    fullText,
                    MODERATE_REJECTION_PHRASES
                ),
                CONFIDENCE_MEDIUM
            );
        }

        // 2. Strong positive hiring decision
        if (positiveSignal) {
            return new DetectionResult(
                STATUS_NOT_REJECTION,
                firstMatchNormalized(
                    fullText,
                    POSITIVE_ADVANCEMENT_PHRASES
                ),
                CONFIDENCE_HIGH
            );
        }


        // 5. Hiring-related, but no actual decision was found
        if (hiringContextDetected) {
            return new DetectionResult(
                STATUS_UNRELATED,
                "Hiring-related email without a clear decision",
                CONFIDENCE_LOW
            );
        }


        // 6. Everything else
        return new DetectionResult(
            STATUS_UNRELATED,
            "No hiring decision detected",
            CONFIDENCE_LOW
        );
    }

    // Heuristic to determine if the sender email address looks like it belongs to a recruiter or hiring team
    private boolean senderLooksLikeRecruiting(String sender) {
        return sender.contains("careers")
            || sender.contains("jobs")
            || sender.contains("recruit")
            || sender.contains("talent")
            || sender.contains("hr");
    }

    // Normalizes the input string by removing diacritics, converting to lowercase, and removing non-alphanumeric characters
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);

        return normalized
            .replaceAll("[^a-z0-9@. ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    // Checks if any of the given phrases are present in the text after normalization
    private boolean containsAnyNormalized(String haystack, List<String> needles) {
        return needles.stream()
            .map(this::normalize)
            .anyMatch(haystack::contains);
        }
    // Returns the first matching phrase found in the text after normalization, or a default message if none are found
    private String firstMatchNormalized(String haystack, List<String> needles) {
        return needles.stream()
            .map(this::normalize)
            .filter(haystack::contains)
            .findFirst()
            .orElse("Matched heuristic");
    }

    //public record to hold results of the detection
    public record DetectionResult(String status, String reason, String confidence) {
    }
}
