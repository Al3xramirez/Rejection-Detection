package rejectiondetection.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rejectiondetection.project.entity.EmailResultsEntity;

@Repository
public interface EmailResultsRepository extends JpaRepository<EmailResultsEntity, Long> {

    List<EmailResultsEntity> findByUserId(Long userId);

}
