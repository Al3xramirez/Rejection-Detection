package rejectiondetection.project.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import rejectiondetection.project.dto.DashboardRejectionItemDto;
import rejectiondetection.project.dto.DashboardResponseDto;
import rejectiondetection.project.dto.MonthlyStatusPointDto;
import rejectiondetection.project.entity.EmailResultsEntity;
import rejectiondetection.project.entity.UsersEntity;
import rejectiondetection.project.repository.EmailResultsRepository;

@Service
public class DashboardService {

    
    private static final String STATUS_REJECTION = "REJECTION";
    private static final String STATUS_POSSIBLE_REJECTION = "POSSIBLE_REJECTION";
    private static final String STATUS_NOT_REJECTION = "NOT_REJECTION";

    //private instances of services and repositories needed to build the dashboard, injected via constructor
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailResultsRepository emailResultsRepository;

    //constructor
    public DashboardService(AuthenticatedUserService authenticatedUserService, EmailResultsRepository emailResultsRepository) {
        
        this.authenticatedUserService = authenticatedUserService;
        this.emailResultsRepository = emailResultsRepository;
    }

    /*
     * Builds the dashboard for the authenticated user.
     * This includes counts of rejections, possible rejections, and acceptances,
     * as well as monthly status points and recent rejections.
     */
    public Optional<DashboardResponseDto> buildDashboard(OAuth2User principal) {
        // checks the authenticated user from the OAuth2 principal, if not authenticated returns empty optional
        Optional<UsersEntity> maybeUser = authenticatedUserService.ensureAndGetUser(principal);
        if (maybeUser.isEmpty()) {
            return Optional.empty();
        }

        // retrieves the user ID from the authenticated user entity
        Long userId = maybeUser.get().getId();

        // retrieves counts of rejections, possible rejections, and acceptances for the user from the email results repository
        long rejectionCount = emailResultsRepository.countByUserIdAndStatus(userId, STATUS_REJECTION);
        long possibleRejectionCount = emailResultsRepository.countByUserIdAndStatus(userId, STATUS_POSSIBLE_REJECTION);
        long acceptanceCount = emailResultsRepository.countByUserIdAndStatus(userId, STATUS_NOT_REJECTION);
        List<MonthlyStatusPointDto> monthly = toMonthlyStatusPoints(emailResultsRepository.findMonthlyCountsByStatus(userId));
        List<DashboardRejectionItemDto> recentRejections = emailResultsRepository
            .findTop50ByUserIdOrderByReceivedAtDesc(userId)
            .stream()
            .map(this::toRejectionItem)
            .toList();

        // constructs and returns the dashboard response DTO with the retrieved data
        return Optional.of(new DashboardResponseDto(
            rejectionCount,
            possibleRejectionCount,
            acceptanceCount,
            monthly,
            recentRejections
        ));
    }

    // Helper method to convert raw query results into a list of MonthlyStatusPointDto, which contains the monthly counts of each status for the dashboard chart.
    private List<MonthlyStatusPointDto> toMonthlyStatusPoints(List<Object[]> rows) {
        Map<String, long[]> buckets = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String status = String.valueOf(row[2]);
            long count = ((Number) row[3]).longValue();

            String monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.US);
            String label = monthName + " " + year;

            long[] values = buckets.computeIfAbsent(label, k -> new long[3]);

            if (STATUS_REJECTION.equals(status)) {
                values[0] = count;
            } else if (STATUS_POSSIBLE_REJECTION.equals(status)) {
                values[1] = count;
            } else if (STATUS_NOT_REJECTION.equals(status)) {
                values[2] = count;
            }
        }

        return buckets.entrySet().stream()
            .map(entry -> new MonthlyStatusPointDto(
                entry.getKey(),
                entry.getValue()[0],
                entry.getValue()[1],
                entry.getValue()[2]
            ))
            .toList();
    }

    // Helper method to convert an EmailResultsEntity into a DashboardRejectionItemDto for the dashboard feed.
    private DashboardRejectionItemDto toRejectionItem(EmailResultsEntity entity) {
        return new DashboardRejectionItemDto(
            entity.getId(),
            entity.getCompanyName(),
            entity.getStatus(),
            entity.getSummary(),
            entity.getEmailSubject(),
            formatDateTime(entity.getReceivedAt())
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Unknown" : value.toString();
    }
}
