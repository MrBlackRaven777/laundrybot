package il.blackraven.klita;

public class EventLogger {

    private EventLogger(){}
    public static boolean log(long chatId, long messageId, int eventType, String message, Object eventObject) {
        Event event = new Event(chatId, messageId, eventType, message, eventObject);
        return log(event);
    }

    public static boolean log(Event event) {
        return DataUtils.addEventLog(event);
    }
}
