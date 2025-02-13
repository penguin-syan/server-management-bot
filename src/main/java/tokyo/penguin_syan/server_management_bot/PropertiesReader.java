package tokyo.penguin_syan.server_management_bot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PropertiesReader {
    private static final String SETTING_FILE_PATH = "/etc/penguin_syan/smb/application.properties";
    private Properties properties;

    public PropertiesReader() {
        log.info("PropertiesReader#<init> start");
        properties = new Properties();

        try {
            log.debug(String.format("Load setting file: %s", SETTING_FILE_PATH));
            InputStream inputStream = new FileInputStream(SETTING_FILE_PATH);
            properties.load(inputStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.info("PropertiesReader#<init> end");
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

}
