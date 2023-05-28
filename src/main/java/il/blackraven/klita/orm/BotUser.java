package il.blackraven.klita.orm;

import java.sql.Timestamp;

public class BotUser {
    private final long tgId;
    private final String name;
    private final Timestamp regDate;
    private Locales locale;

    public BotUser(long tgId, String name, Locales locale) {
        this.tgId = tgId;
        this.name = name;
        this.regDate = new Timestamp(System.currentTimeMillis());
        this.locale = locale;
    }

    public BotUser(long tgId, String name, Timestamp regDate, Locales locale) {
        this.tgId = tgId;
        this.name = name;
        this.regDate = regDate;
        this.locale = locale;
    }

    public long getTgId() {
        return tgId;
    }

    public String getName() {
        return name;
    }

    public Timestamp getRegDate() {
        return regDate;
    }

    public void setLocale(Locales locale) {
        this.locale = locale;
    }

    public Locales getLocale() {
        return locale;
    }
}
