package com.inspiration.catcher.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class UIConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(UIConfigManager.class);

    private static final String CONFIG_FILE = "inspiration_config.properties";
    private final Properties config = new Properties();

    public void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                    logger.info("加载配置成功");
                }
            }
        } catch (Exception e) {
            logger.error("加载配置失败", e);
        }
    }

    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                config.store(fos, "Inspiration Catcher Configuration");
                logger.info("保存配置成功");
            }
        } catch (Exception e) {
            logger.error("保存配置失败", e);
        }
    }

    public String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    public void setConfig(String key, String value) {
        config.setProperty(key, value);
    }
}