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
        "unfortunately"
    );

    private static final List<String> MODERATE_REJECTION_PHRASES = List.of(
        "moving forward with other candidates",
        "wont be moving forward",
        "won't be moving forward",
        "thank you for your interest",
        "not the right fit at this time"
    );

    private static final List<String> HIRING_CONTEXT_KEYWORDS = List.of(
        "application",
        "role",
        "position",
        "interview",
        "candidate",
        "hiring team",
        "recruiter"
    );

    private static final List<String> POSITIVE_ADVANCEMENT_PHRASES = List.of(
        "next steps",
        "schedule interview",
        "offer",
        "congratulations",
        "move to next round",
        "congratulations!,",
        "we would like to invite you",
        "we are excited to move forward"
    );

    private static final List<String> MARKETING_HINTS = List.of(
        "unsubscribe",
        "view in browser",
        "marketing",
        "promo"
    );

    public DetectionResult detect(String fromAddress, String subject, String body) {

        // Normalize inputs which makes all inputs lowercase, removes accents, and collapses whitespace
        String normalizedSubject = normalize(subject);
        String normalizedBody = normalize(body);
        String normalizedSender = normalize(fromAddress);

        // Combine subject and body for easier searching, but keep sender separate for certain heuristics
        String fullText = (normalizedSubject + " " + normalizedBody).trim();

        // Whole bunch of if statements for now to determine rejection status based upon keywords and phrases.
        // my later TODO will try incorporate a more advanced algorithmic approach, not sure where I would start though


        // ContainsAny() checks if any phraces are in "fullText".
        if (containsAny(fullText, POSITIVE_ADVANCEMENT_PHRASES)) {
            return new DetectionResult(STATUS_NOT_REJECTION, "Positive advancement language", CONFIDENCE_LOW);
        }

        // Strong rejections signals should be pretty definitive
        if (containsAny(fullText, STRONG_REJECTION_PHRASES)) {
            String reason = firstMatch(fullText, STRONG_REJECTION_PHRASES);
            return new DetectionResult(STATUS_REJECTION, reason, CONFIDENCE_HIGH);
        }

        // Moderate rejection signals require additional context to be more confident
        // If there is a moderate rejction phrase, but does not contain any hiring context, that will lean towards a rejection 
        if (containsAny(fullText, MODERATE_REJECTION_PHRASES)) {
            boolean hasHiringContext = containsAny(fullText, HIRING_CONTEXT_KEYWORDS)
                || senderLooksLikeRecruiting(normalizedSender);

            
            // If it does contain hiring context, then its not a definitive rejection.
            if (hasHiringContext) {
                String reason = firstMatch(fullText, MODERATE_REJECTION_PHRASES);
                return new DetectionResult(STATUS_POSSIBLE_REJECTION, reason, CONFIDENCE_MEDIUM);
            }

            return new DetectionResult(
                STATUS_NOT_REJECTION,
                "Moderate rejection phrase without hiring context",
                CONFIDENCE_LOW
            );
        }

        // This is simply to try to by-pass any false positives like marketing emails
        if (containsAny(fullText, MARKETING_HINTS)) {
            return new DetectionResult(STATUS_NOT_REJECTION, "Likely marketing content", CONFIDENCE_LOW);
        }

        // This is for the most part to catch emails not related to hiring at all
        return new DetectionResult(STATUS_NOT_REJECTION, "No rejection signal matched", CONFIDENCE_LOW);
    }

    // Heuristic to determine if the sender email address looks like it belongs to a recruiter or hiring team
    private boolean senderLooksLikeRecruiting(String sender) {
        return sender.contains("careers")
            || sender.contains("jobs")
            || sender.contains("recruit")
            || sender.contains("talent")
            || sender.contains("hr");
    }

    // Checks if any of the given phrases are present in the text
    private boolean containsAny(String haystack, List<String> needles) {
        return needles.stream().anyMatch(haystack::contains);
    }

    // Returns the first matching phrase found in the text, or a default message if none are found
    private String firstMatch(String haystack, List<String> needles) {
        return needles.stream().filter(haystack::contains).findFirst().orElse("Matched heuristic");
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

    //public record to hold results of the detection
    public record DetectionResult(String status, String reason, String confidence) {
    }
}
