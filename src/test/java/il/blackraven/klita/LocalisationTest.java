package il.blackraven.klita;

import il.blackraven.klita.orm.Locales;
import static il.blackraven.klita.Localisation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalisationTest {
    @BeforeEach
    void setUp() {
        BotConfig.init();
        DataUtils.init();
        Localisation.init();
    }

    @Test
    void getUserLocale() {
        long chatId = 123456789;
        String userLocale = "ru";
        Locales locale = Localisation.getUserLocale(chatId, userLocale);
        assertEquals(locale, Locales.ru_RU);

        chatId = 1234567890;
        userLocale = "en";
        locale = Localisation.getUserLocale(chatId, userLocale);
        assertEquals(locale, Locales.en_US);

        chatId = 12345678900L;
        locale = Localisation.getUserLocale(chatId);
        assertEquals(locale, Locales.en_US);
    }
}
