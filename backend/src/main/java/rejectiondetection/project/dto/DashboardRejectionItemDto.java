package rejectiondetection.project.dto;

public record DashboardRejectionItemDto(
    Long id,
    String companyName,
    String status,
    String summary,
    String emailSubject,
    String receivedAt
) {
}
