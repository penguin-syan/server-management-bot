package tokyo.penguin_syan.server_management_bot;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tokyo.penguin_syan.server_management_bot.jda.DiscordBot;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();

        logger.info("Main#main start");

        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        if (Objects.isNull(trustStore)) {
            String defaultTrustStore = System.getProperty("java.home");
            defaultTrustStore += "/lib/security/cacerts";
            logger.debug(String.format("Trust store path (Default): %s", defaultTrustStore));
        } else {
            logger.debug(String.format("Trust store path: %s", trustStore));
        }

        DiscordBot.initial();

        logger.info("Main#main end");
    }
}
