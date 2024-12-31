package tokyo.penguin_syan.server_management_bot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tokyo.penguin_syan.server_management_bot.jda.DiscordBot;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();

        logger.info("Main#main start");

        DiscordBot.initial();

        logger.info("Main#main end");
    }
}
