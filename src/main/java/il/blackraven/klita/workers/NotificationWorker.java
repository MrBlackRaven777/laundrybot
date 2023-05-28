package il.blackraven.klita.workers;

import il.blackraven.klita.*;
import il.blackraven.klita.orm.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;


public class NotificationWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(NotificationWorker.class);
    private final int DEFAULT_RESCHEDULE_INTERVAL = 120000;
    private final KlitaLaundryBot bot;
    public NotificationWorker(KlitaLaundryBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        ArrayList<Notification> upcomingNotifications = DataUtils.browseAllUpcomingNotifications();
        for (Notification notification :
                upcomingNotifications) {
            if (notification.getDeliveryCount() == notification.getMaxDeliveryCount()) {
                notification.setNextDeliveryTime(null);
                DataUtils.declineNotification(notification.getId());
                continue;
            }
            Job job = DataUtils.jobBrowse(notification.getJobId());
            Machine machine = DataUtils.getMachineById(job.getMachineId());
            SendMessage message = new SendMessage();
            message.setChatId(notification.getUserTgId());
            message.setText(notification.getMessage());
            if (notification.getType().equals(NotificationType.JOB_END)) {
                message.setReplyMarkup(KeyboardFactory.endJobNotification(job.getId(), machine.getType(), Localisation.getUserLocale(notification.getUserTgId())));
            }
            if (notification.getMessageId() != 0) {
                bot.silent().execute(new DeleteMessage(String.valueOf(notification.getUserTgId()),
                        notification.getMessageId()));

                String logMsg = String.format("Delete from user %s message %s", notification.getUserTgId(), notification.getMessageId());
                log.info(logMsg);
                EventLogger.log(job.getUserTgId(), 0, Event.NOTIFICATION, logMsg, notification);
            }
            Message sentMessage = new Message();
            try {
                sentMessage = bot.sender().execute(message);

                String logMsg = String.format("Sent user %s notification %s", notification.getUserTgId(), notification.getId().toString());
                log.info(logMsg);
                EventLogger.log(job.getUserTgId(), 0, Event.NOTIFICATION, logMsg, notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int messageId = sentMessage.getMessageId();
            notification.setMessageId(messageId);
            DataUtils.changeNotificationMessageId(notification.getId(), messageId);
            DataUtils.rescheduleNotification(notification.getId(), DEFAULT_RESCHEDULE_INTERVAL);
        }
    }
}
