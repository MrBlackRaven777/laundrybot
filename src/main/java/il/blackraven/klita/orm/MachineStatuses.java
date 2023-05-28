package il.blackraven.klita.orm;

import java.util.HashMap;
import java.util.Map;


public class MachineStatuses {
    public static final int IDLE = 0;
    public static final int BUSY = 2;
    public static final int OUT_OF_ORDER = 99;

    private static final HashMap<Integer, String> statusNames = new HashMap<>(Map.of(
            IDLE, "IDLE",
            BUSY, "BUSY",
            OUT_OF_ORDER, "OUT_OF_ORDER"
    ));

    private static final HashMap<Integer, String> statusEmoji = new HashMap<>(Map.of(
            IDLE, "✅",
            BUSY, "\uD83D\uDD5B",
            OUT_OF_ORDER, "❌"
    ));

    public static String getStatusName(int status) {
        return "STATUS_" + statusNames.get(status);
    }

    public static String getStatusEmoji(int status) {
        return statusEmoji.getOrDefault(status, "");
    }
}
