package il.blackraven.klita;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BotConfigTest {

    @Test
    public void botConfigDefaultInitTest() {
        BotConfig.init();
        assertTrue(BotConfig.isWasInit());
    }

    @Test
    public void botConfigCustomInitTest() {
        System.setProperty("bot.config", "./src/test/resources/config/custom.config");
        BotConfig.init();
        assertTrue(BotConfig.isWasInit());
        assertEquals("qwertyuiop", BotConfig.getProperty("bot.token"));
        System.clearProperty("bot.config");
    }

    @Test
    public void botConfigGetPropertyTest() {
        BotConfig.init();
        assertEquals("abcdefg:1234567890", BotConfig.getProperty("bot.token"));
        assertEquals("TestYourBot", BotConfig.getProperty("bot.username"));
        assertEquals("123456789", BotConfig.getProperty("bot.creatorId"));
        assertEquals("217.25.88.252", BotConfig.getProperty("db.host"));
        assertEquals("5432", BotConfig.getProperty("db.port"));
        assertEquals("klitalaundrybot-test", BotConfig.getProperty("db.name"));
        assertEquals("bot", BotConfig.getProperty("db.user"));
        assertEquals("qwe123", BotConfig.getProperty("db.password"));
    }

    @Test
    public void botConfigGetConfigTest() {
        BotConfig.init();
        Properties props = BotConfig.getConfig();
        assertEquals("abcdefg:1234567890", props.getProperty("bot.token"));
        assertEquals("TestYourBot", props.getProperty("bot.username"));
        assertEquals("123456789", props.getProperty("bot.creatorId"));
        assertEquals("217.25.88.252", props.getProperty("db.host"));
        assertEquals("5432", props.getProperty("db.port"));
        assertEquals("klitalaundrybot-test", props.getProperty("db.name"));
        assertEquals("bot", props.getProperty("db.user"));
        assertEquals("qwe123", props.getProperty("db.password"));
    }

    @AfterEach
    public void clean() {
        BotConfig.deInit();
    }
}
