package rejectiondetection.project.dto;

import java.util.List;

public record DashboardResponseDto(
    long rejectionCount,
    long possibleRejectionCount,
    long acceptanceCount,
    List<MonthlyStatusPointDto> monthlyResults,
    List<DashboardRejectionItemDto> results
) {
}
