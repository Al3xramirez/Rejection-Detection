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
        "thank you for your interest",
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
        "next steps",
        "schedule interview",
        "offer",
        "congratulations",
        "move to next round",
        "we would like to invite you",
        "we are excited to move forward",
        "we are pleased to offer you",
        "we are happy to move forward",
        "we are excited to move forward with you",
        "we are pleased to offer you the position",
        "we are happy to move forward with your application"
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

        // Normalize inputs which makes all inputs lowercase, removes accents, and collapses whitespace
        String normalizedSubject = normalize(subject);
        String normalizedBody = normalize(body);
        String normalizedSender = normalize(fromAddress);

        // Combine subject and body for easier searching, but keep sender separate for certain heuristics
        String fullText = (normalizedSubject + " " + normalizedBody).trim();

        int rejectionScore = 0;
        int positiveScore = 0;
        int marketingScore = 0;

        String reason = "No rejection signal matched";

        boolean strongRejectionSignal = containsAnyNormalized(fullText, STRONG_REJECTION_PHRASES);
        boolean moderateRejectionSignal = containsAnyNormalized(fullText, MODERATE_REJECTION_PHRASES);
        boolean positiveSignal = containsAnyNormalized(fullText, POSITIVE_ADVANCEMENT_PHRASES);
        boolean marketingSignal = containsAnyNormalized(fullText, MARKETING_HINTS);
        boolean hiringContextDetected = containsAnyNormalized(fullText, HIRING_CONTEXT_KEYWORDS)
            || senderLooksLikeRecruiting(normalizedSender);

        // Keep unrelated email categories out of the database entirely.
        if (marketingSignal && !hiringContextDetected && !strongRejectionSignal && !moderateRejectionSignal) {
            return new DetectionResult(STATUS_UNRELATED, "Likely marketing content", CONFIDENCE_LOW);
        }

        if (!hiringContextDetected && !strongRejectionSignal && !moderateRejectionSignal && !positiveSignal) {
            return new DetectionResult(STATUS_UNRELATED, "No hiring context detected", CONFIDENCE_LOW);
        }

        if (positiveSignal) {
            positiveScore += 6;
        }

        if (marketingSignal) {
            marketingScore += 4;
        }

        if (strongRejectionSignal) {
            rejectionScore += 8;
            reason = firstMatchNormalized(fullText, STRONG_REJECTION_PHRASES);
        }

        if (moderateRejectionSignal) {
            rejectionScore += 4;
            reason = firstMatchNormalized(fullText, MODERATE_REJECTION_PHRASES);
        }

        int strongWordMatches = countPhraseWordMatches(fullText, STRONG_REJECTION_PHRASES);
        int moderateWordMatches = countPhraseWordMatches(fullText, MODERATE_REJECTION_PHRASES);

        rejectionScore += strongWordMatches;
        rejectionScore += moderateWordMatches;

        if (hiringContextDetected) {
            rejectionScore += 3;
        }

        int totalScore = rejectionScore - positiveScore - marketingScore;

        if (totalScore >= 8) {
            return new DetectionResult(STATUS_REJECTION, reason, CONFIDENCE_HIGH);
        } else if (totalScore >= 4) {
            return new DetectionResult(STATUS_POSSIBLE_REJECTION, reason, CONFIDENCE_MEDIUM);
        }

        return new DetectionResult(STATUS_NOT_REJECTION, reason, CONFIDENCE_LOW);
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

    // Counts the number of phrases that have at least two important word matches in the text
    private int countPhraseWordMatches(String fullText, List<String> phrases) {
        int score = 0;

        for (String phrase : phrases) {
            String normalizedPhrase = normalize(phrase);
            String[] words = normalizedPhrase.split(" "); // Split the phrase into words

            int matches = 0;

            for (String word : words) {
                if (isImportantWord(word) && fullText.contains(word)) {
                    matches++;
                }
            }

            if (matches >= 2) {
                score += 2;
            }

            if (matches >= 3) {
                score += 2;
            }
        }

        return score;
    }   

    // Counts the number of phrases that are present in the text after normalization
    private boolean isImportantWord(String word) {
        return word.length() >= 4
            && !word.equals("with")
            && !word.equals("your")
            && !word.equals("have")
            && !word.equals("will")
            && !word.equals("this")
            && !word.equals("that")
            && !word.equals("from")
            && !word.equals("thank")
            && !word.equals("interest");
    }
    //public record to hold results of the detection
    public record DetectionResult(String status, String reason, String confidence) {
    }
}
