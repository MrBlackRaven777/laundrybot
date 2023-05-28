package il.blackraven.klita.workers;

import com.github.f4b6a3.ulid.Ulid;
import il.blackraven.klita.DataUtils;
import il.blackraven.klita.Event;
import il.blackraven.klita.EventLogger;
import il.blackraven.klita.Localisation;
import il.blackraven.klita.orm.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class JobWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(JobWorker.class);
    private static final byte NOTIFICATION_ADVANCE = 5; //TODO move to user parameters
    private static final byte NOTIFY_ABOUT_TO_END_JOB_MAX_DELIVERY_COUNT = 1;
    private static final byte NOTIFY_END_JOB_MAX_DELIVERY_COUNT = 5;
    private static final byte WAIT_UNTIL_RELEASE_MACHINE = 30;

    @Override
    public void run() {
        try {
            DataUtils.getConnection().setAutoCommit(false);
            //Check all jobs for about to expiry
            ArrayList<Job> startedJobs = DataUtils.browseAllJobsWithStatus(JobStatus.JOB_STARTED);
            startedJobs.stream().filter(job -> job.getExpiry().getTime() - System.currentTimeMillis() < TimeUnit.MINUTES.toMillis(NOTIFICATION_ADVANCE))
                    .forEach(job -> {
                        Locales userLocale = Localisation.getUserLocale(job.getUserTgId());
                        job.setStatus(JobStatus.JOB_ABOUT_TO_END);
                        DataUtils.jobChangeStatus(job.getId().toString(), JobStatus.JOB_ABOUT_TO_END);

                        String logMsg = String.format("Changed job %s status from 'JOB_STARTED' to 'JOB_ABOUT_TO_END'", job.getId().toString());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);

                        if(job.getUserTgId() == 0) return; //0 userTgId means that job comes from report and no need to notify someone about it

                        Program program = DataUtils.getProgramByUUID(job.getProgramId());
                        Machine machine = DataUtils.getMachineById(job.getMachineId());
                        Notification notification = new Notification(Ulid.fast().toUuid(),
                                job.getId(),
                                job.getUserTgId(),
                                Localisation.getMessage("NOTIFICATION_ABOUT_TO_END",
                                        userLocale,
                                        Localisation.getMessage("VERB_" + machine.getType().toString().toUpperCase(), userLocale),
                                        program.getPublicName(),
                                        machine.getLaundry_id()),
                                NotificationType.JOB_ABOUT_TO_END,
                                0,
                                NOTIFY_ABOUT_TO_END_JOB_MAX_DELIVERY_COUNT,
                                new Timestamp(System.currentTimeMillis()));

                        job.setNotificationId(notification.getId());
                        DataUtils.putNotification(notification);
                        DataUtils.jobChangeNotificationId(job.getId(), notification.getId());
                    });
            DataUtils.getConnection().commit();

            //Check all jobs for expiry
            ArrayList<Job> AtEJobs = DataUtils.browseAllJobsWithStatus(JobStatus.JOB_ABOUT_TO_END);
            AtEJobs.stream().filter(job -> job.getExpiry().getTime() - System.currentTimeMillis() < 0)
                    .forEach(job -> {
                        Locales userLocale = Localisation.getUserLocale(job.getUserTgId());
                        job.setStatus(JobStatus.JOB_END_NOT_CONFIRMED);
                        DataUtils.jobChangeStatus(job.getId().toString(), JobStatus.JOB_END_NOT_CONFIRMED);

                        String logMsg = String.format("Changed job %s status from 'JOB_ABOUT_TO_END' to 'JOB_END_NOT_CONFIRMED'", job.getId().toString());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);

                        if(job.getUserTgId() == 0) return; //0 userTgId means that job comes from report and no need to notify someone about it

                        DataUtils.declineNotification(job.getNotificationId());

                        logMsg = String.format("Declined notification %s for 'JOB_ABOUT_TO_END'", job.getNotificationId());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);

                        Program program = DataUtils.getProgramByUUID(job.getProgramId());
                        Machine machine = DataUtils.getMachineById(job.getMachineId());
                        Notification notification = new Notification(Ulid.fast().toUuid(),
                                job.getId(),
                                job.getUserTgId(),
                                Localisation.getMessage("NOTIFICATION_JOB_END",
                                        userLocale,
                                        Localisation.getMessage("VERB_" + machine.getType().toString().toUpperCase(), userLocale),
                                        program.getPublicName(),
                                        machine.getLaundry_id()),
                                NotificationType.JOB_END,
                                0,
                                NOTIFY_END_JOB_MAX_DELIVERY_COUNT,
                                new Timestamp(System.currentTimeMillis()));
                        job.setNotificationId(notification.getId());
                        DataUtils.putNotification(notification);
                        DataUtils.jobChangeNotificationId(job.getId(), notification.getId());

                        logMsg = String.format("Put notification %s for 'JOB_END_NOT_CONFIRMED'", notification.getId());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);
                    });
            DataUtils.getConnection().commit();

            //Check all jobs for not confirmed jobs. Release all machines tha ends wash more than half hour ago.
            ArrayList<Job> EnCJobs = DataUtils.browseAllJobsWithStatus(JobStatus.JOB_END_NOT_CONFIRMED);
            EnCJobs.stream().filter(job -> job.getExpiry().getTime() +
                            TimeUnit.MINUTES.toMillis(WAIT_UNTIL_RELEASE_MACHINE) <
                            System.currentTimeMillis())
                    .forEach(job -> {
                        job.setStatus(JobStatus.JOB_ENDED);
                        DataUtils.jobChangeStatus(job.getId().toString(), JobStatus.JOB_ENDED);
                        String logMsg = String.format("Changed job %s status from 'JOB_END_NOT_CONFIRMED' to 'JOB_ENDED'", job.getId().toString());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);

                        DataUtils.machineChangeStatus(job.getMachineId(), MachineStatuses.IDLE);
                        logMsg = String.format("Changed machine %s state from 'BUSY' to 'IDLE'", job.getMachineId());
                        log.info(logMsg);
                        EventLogger.log(job.getUserTgId(), 0, Event.JOB_CHANGE, logMsg, job);
                    });
            DataUtils.getConnection().commit();
            DataUtils.getConnection().setAutoCommit(true);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
