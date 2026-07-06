package rejectiondetection.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailSyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSyncScheduler.class);

    /**
     * Hourly scaffold for pulling incoming emails per user.
     * Real Gmail sync logic will be implemented here after OAuth token persistence is added.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyEmailSync() {
        LOGGER.info("Hourly email sync scaffold executed. Implement Gmail pull logic here.");
    }
}
