package tokyo.penguin_syan.server_management_bot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertiesReader {
    private static final String SETTING_FILE_PATH = "/etc/penguin_syan/smb/application.properties";
    private Properties properties;
    private Logger logger = LogManager.getLogger();

    public PropertiesReader() {
        logger.info("PropertiesReader#<init> start");
        properties = new Properties();

        try {
            logger.debug(String.format("Load setting file: %s", SETTING_FILE_PATH));
            InputStream inputStream = new FileInputStream(SETTING_FILE_PATH);
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("PropertiesReader#<init> end");
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

}
