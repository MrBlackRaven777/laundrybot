package il.blackraven.klita;

import il.blackraven.klita.orm.Locales;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class Localisation {

    private static final Logger log = LogManager.getLogger(Localisation.class);
    private final static String MESSAGES_KEY = "BotLocale";
    private final static Locales DEFAULT_LOCALE = Locales.en_US;
    private static HashMap<Locales, ResourceBundle> bundles;
    private static List<Locales> supportedLocales;
    protected static HashMap<Long, Locales> USER_LOCALES;
    private Localisation() {
    }
    public static void init() {
        bundles = new HashMap<>();
        supportedLocales = Arrays.stream(BotConfig.getProperty("bot.supportedLocales").split(","))
                .map(Locales::valueOf).collect(Collectors.toList());
        for (Locales locale:
                supportedLocales) {
            String[] localeParts = locale.name().split("_");
            ResourceBundle bundle = ResourceBundle.getBundle("localisations/" + MESSAGES_KEY, new Locale(localeParts[0], localeParts[1]));
            bundles.put(locale, bundle);
        }
        USER_LOCALES = new HashMap<>();
    }

    public static void chooseUserLocale(long chatId, String accountLocale){
        if (!USER_LOCALES.containsKey(chatId)) {

        }
    }

    public static Locales getUserLocale(long chatId) {
        return getUserLocale(chatId, DEFAULT_LOCALE.name());
    }
    public static Locales getUserLocale(long chatId, String accountLocale) {
        if (USER_LOCALES.containsKey(chatId)) {
            return USER_LOCALES.get(chatId);
        } else {
            Locales locale = DataUtils.getUserLocale(chatId);
            if (locale == null) locale = supportedLocales.stream().filter(loc -> loc.name().startsWith(accountLocale))
                    .findFirst().orElse(DEFAULT_LOCALE);
            USER_LOCALES.put(chatId, locale);
            return locale;
        }
    }
    public static List<Locales> getAllLocales() {
        return supportedLocales;
    }

    public static void changeUserLocale(long chatId, Locales newLocale) {
        if(USER_LOCALES.containsKey(chatId)) {
            USER_LOCALES.replace(chatId, newLocale);
        } else {
            USER_LOCALES.put(chatId, newLocale);
        }
        DataUtils.userChangeLocale(String.valueOf(chatId), newLocale);
    }

    public static String getMessage(String key, long chatId) {
        return bundles.get(getUserLocale(chatId)).getString(key);
    }

    public static String getMessage(String key,  long chatId, Object ... arguments) {
        return MessageFormat.format(getMessage(key, getUserLocale(chatId)), arguments);
    }
    public static String getMessage(String key, Locales locale) {
        if(!bundles.containsKey(locale)) {
            log.error("No locale " + locale);
        }
        return bundles.get(locale).getString(key);
    }
    public static String getMessage(String key, Locales locale, Object ... arguments) {
        return MessageFormat.format(getMessage(key, locale), arguments);
    }
}
