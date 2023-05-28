package il.blackraven.klita;

import com.google.gson.Gson;

public class Event {
    public static final int COMMAND = 1;
    public static final int COMMAND_REPLY = 2;
    public static final int CALLBACK = 10;
    public static final int CALLBACK_REPLY = 20;
    public static final int JOB_CHANGE = 50;
    public static final int NOTIFICATION = 100;
    long chatId;
    long messageId;
    int eventType;
    String message;
    Object eventObject;

    public Event(long chatId, long messageId, int eventType, String message, Object eventObject) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.eventType = eventType;
        this.message = message;
        this.eventObject = eventObject;
    }

    public long getChatId() {
        return chatId;
    }

    public long getMessageId() {
        return messageId;
    }

    public int getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public Object getEventObject() {
        return eventObject;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
