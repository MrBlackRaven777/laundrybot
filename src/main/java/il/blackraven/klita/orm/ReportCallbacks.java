package il.blackraven.klita.orm;

import java.util.*;

public enum ReportCallbacks implements BotCallback {
    //TODO ? change enum to class for localisation
    ALL_IDLE,
    SET_ALL,
    SET_SPECIFIC_MACHINE,
    CANCEL_REPORT,
    AGREE_OVERWRITE,
    DISAGREE_OVERWRITE,
    MACHINE_FREE,
    BUSY_JOB_END,
    BUSY_5_MIN,
    BUSY_10_MIN,
    BUSY_20_MIN,
    BUSY_30_MIN,
    BUSY_40_MIN,
    OUT_OF_ORDER;

    private static final LinkedHashSet<ReportCallbacks> machineCallbacks = new LinkedHashSet<>(List.of(
            MACHINE_FREE,
            BUSY_JOB_END,
            BUSY_5_MIN,
            BUSY_10_MIN,
            BUSY_20_MIN,
            BUSY_30_MIN,
            BUSY_40_MIN,
            OUT_OF_ORDER,
            CANCEL_REPORT));

    private static final HashMap<ReportCallbacks, Integer> callbacksDuration = new HashMap<>(Map.of(
            MACHINE_FREE, 0,
            BUSY_JOB_END, 0,
            BUSY_5_MIN, 5,
            BUSY_10_MIN,10,
            BUSY_20_MIN,20,
            BUSY_30_MIN,30,
            BUSY_40_MIN,40,
            OUT_OF_ORDER, 0,
            CANCEL_REPORT, 0));
    public static HashSet<ReportCallbacks> getMachineCallbacks() {
        return machineCallbacks;
    }

    public static int getCallbackDuration(ReportCallbacks callback) {
        return callbacksDuration.get(callback);
    }
}
