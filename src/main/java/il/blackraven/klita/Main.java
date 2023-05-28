package il.blackraven.klita;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {


    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Init main");
        BotConfig.init();
        DataUtils.init();
        Localisation.init();

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new KlitaLaundryBot(BotConfig.getProperty("bot.token"), BotConfig.getProperty("bot.username")));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}