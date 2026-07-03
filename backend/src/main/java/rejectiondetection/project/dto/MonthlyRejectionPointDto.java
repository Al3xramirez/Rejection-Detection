package rejectiondetection.project.dto;

public record MonthlyRejectionPointDto(
    String monthLabel,
    long count
) {
}
