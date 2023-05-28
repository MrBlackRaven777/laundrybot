package il.blackraven.klita;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class Settings {
    private static final String LANGUAGE = "LANGUAGE";
    private static final String NOTIFICATIONS = "NOTIFICATIONS";
    private static final String BACK = "BACK";
    private static final String CLOSE = "CLOSE";

    private static final List<String> callbacks = List.of(LANGUAGE);//, NOTIFICATIONS);
    protected static void handleCommand(Update update) {
        //TODO
    }

    protected static List<String> getCallbacks() {
        return callbacks;
    }

    protected static void handleCallbacks(String[] cbArray, Update update) {

    }
}
