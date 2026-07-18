package rejectiondetection.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailSyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSyncScheduler.class);
    private final GmailSyncService gmailSyncService;

    public EmailSyncScheduler(GmailSyncService gmailSyncService) {
        this.gmailSyncService = gmailSyncService;
    }

    /**
     * Scheduler calls gmalSyncService every hour, a simple calling class to keep the scheduling logic separate from the actual sync logic.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyEmailSync() {
        LOGGER.info("Hourly email sync trigger fired.");
        try {
            gmailSyncService.runHourlySync();
        } catch (Exception ex) {
            LOGGER.error("Hourly email sync failed.", ex);
        }
    }
}
