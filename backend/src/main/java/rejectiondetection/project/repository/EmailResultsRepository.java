package rejectiondetection.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rejectiondetection.project.entity.EmailResultsEntity;

@Repository
public interface EmailResultsRepository extends JpaRepository<EmailResultsEntity, Long> {

    List<EmailResultsEntity> findByUserId(Long userId);

    // Counts the number of email results for a specific user and status. This is useful for quickly determining
    // how many emails of a certain status (e.g., REJECTION) a user has received.
    long countByUserIdAndStatus(Long userId, String status);

    // Retrieves the top 50 email results for a specific user and status, ordered by the received date in descending order.
    // This is useful for quickly accessing the most recent email results of a certain status (e.g., REJECTION) for a user.
    List<EmailResultsEntity> findTop50ByUserIdAndStatusOrderByReceivedAtDesc(Long userId, String status);
    //lmao I definitly AI'd this query, but it works for aggregating the monthly rejection counts for a user.
    @Query(value = """
        SELECT YEAR(received_at) AS y, MONTH(received_at) AS m, COUNT(*) AS total
        FROM email_results
        WHERE user_id = :userId
          AND status = :status
          AND received_at IS NOT NULL
        GROUP BY YEAR(received_at), MONTH(received_at)
        ORDER BY y ASC, m ASC
        """, nativeQuery = true)

    // The method returns a list of Object arrays, where each array contains three elements:
    // 1. The year (as Integer)
    // 2. The month (as Integer)
    // 3. The total count of email results for that month (as Long)
    // This allows the application to easily aggregate and display monthly rejection counts for a user.
    List<Object[]> findMonthlyRejectionCounts(Long userId, String status);

}
