package il.blackraven.klita.orm;

public enum States {
    NEW_USER,
    BOT_START,
    USE_START,
    USE_CHOOSE_MACHINE,
    USE_CHOOSE_PROGRAM,
    USE_AWAIT_NOTIFICATION,
    USE_NOTIFICATION_SENT,
    USE_END,
    USE_CANCEL_WASH,
    USE_CANCEL_DRY,

    REPORT_START,
    REPORT_SET_ALL,
    REPORT_SET_SPECIFIC_MACHINE,
    REPORT_POLL_SEND,
    REPORT_END,

    SETTINGS,
    UNKNOWN;
}