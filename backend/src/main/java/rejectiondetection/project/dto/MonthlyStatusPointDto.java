package rejectiondetection.project.dto;

public record MonthlyStatusPointDto(
    String monthLabel,
    long rejectionCount,
    long possibleRejectionCount,
    long acceptanceCount
) {
}
