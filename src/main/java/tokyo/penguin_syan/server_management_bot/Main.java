package tokyo.penguin_syan.server_management_bot;

import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import tokyo.penguin_syan.server_management_bot.jda.DiscordBot;

@Log4j2
public class Main {
    public static void main(String[] args) {

        log.info("Main#main start");

        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        if (Objects.isNull(trustStore)) {
            String defaultTrustStore = System.getProperty("java.home");
            defaultTrustStore += "/lib/security/cacerts";
            log.debug(String.format("Trust store path (Default): %s", defaultTrustStore));
        } else {
            log.debug(String.format("Trust store path: %s", trustStore));
        }

        DiscordBot.initial();

        log.info("Main#main end");
    }
}
