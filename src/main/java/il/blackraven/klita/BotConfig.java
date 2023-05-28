package il.blackraven.klita;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class BotConfig {

    private static final Logger log = LogManager.getLogger(BotConfig.class);
    private static String BOT_TOKEN;
    private static String BOT_NAME;
    private static long CREATOR_ID;
    private static String DB_HOST;
    private static int DB_PORT;
    private static String DB_NAME;
    private static String DB_USERNAME;
    private static String DB_PASS;

    private static Properties props;

    public static boolean isWasInit() {
        return wasInit;
    }

    private static boolean wasInit = false;

    private BotConfig() {

    }

    public static void init() {
        if (wasInit) return; //TODO may be allow hot reload config
        props = new Properties();

        String configName = System.getProperty("bot.config", "");
        try (InputStream is = "".equals(configName) ? ClassLoader.getSystemResourceAsStream("config/app.config") : new FileInputStream(configName)){
            props.load(is);
            wasInit = true;
            log.info(props.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Properties getConfig() {
        if (!wasInit) init();
        return props;
    }

    public static String getProperty(String propName) {
        if (!wasInit) init();
        if (props.containsKey(propName)) return props.getProperty(propName);
        return "";
    }

    static void deInit() {
        props = null;
        wasInit = false;
    }
}
