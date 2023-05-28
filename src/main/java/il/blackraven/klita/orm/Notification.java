package il.blackraven.klita.orm;

import java.sql.Timestamp;
import java.util.UUID;

public class Notification {

    private UUID id;
    private UUID jobId;
    private long userTgId;
    private int messageId;
    private String message;
    private NotificationType type;
    private int deliveryCount;
    private int maxDeliveryCount;
    private Timestamp nextDeliveryTime;

    public Notification(UUID id, UUID jobId, long userTgId, String message, NotificationType type, int deliveryCount, int maxDeliveryCount, Timestamp nextDeliveryTime) {
        this.id = id;
        this.jobId = jobId;
        this.userTgId = userTgId;
        this.message = message;
        this.type = type;
        this.deliveryCount = deliveryCount;
        this.maxDeliveryCount = maxDeliveryCount;
        this.nextDeliveryTime = nextDeliveryTime;
    }
    public Notification(UUID id, UUID jobId, long userTgId, int messageId, String message, NotificationType type, int deliveryCount, int maxDeliveryCount, Timestamp nextDeliveryTime) {
        this.id = id;
        this.jobId = jobId;
        this.userTgId = userTgId;
        this.messageId = messageId;
        this.message = message;
        this.type = type;
        this.deliveryCount = deliveryCount;
        this.maxDeliveryCount = maxDeliveryCount;
        this.nextDeliveryTime = nextDeliveryTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public long getUserTgId() {
        return userTgId;
    }

    public void setUserTgId(long userTgId) {
        this.userTgId = userTgId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(int deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public int getMaxDeliveryCount() {
        return maxDeliveryCount;
    }

    public void setMaxDeliveryCount(int maxDeliveryCount) {
        this.maxDeliveryCount = maxDeliveryCount;
    }

    public Timestamp getNextDeliveryTime() {
        return nextDeliveryTime;
    }

    public void setNextDeliveryTime(Timestamp nextDeliveryTime) {
        this.nextDeliveryTime = nextDeliveryTime;
    }
}
