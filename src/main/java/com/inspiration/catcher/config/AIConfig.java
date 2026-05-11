package com.inspiration.catcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AIConfig {
    private static final Logger logger = LoggerFactory.getLogger(AIConfig.class);
    private static final Properties props = new Properties();

    // 配置文件路径优先级：
    // 1. 用户主目录的配置文件  2. 项目资源文件
    private static final String USER_CONFIG_PATH =
            System.getProperty("user.home") + "/.inspiration-catcher/ai.properties";
    private static final String RESOURCE_CONFIG_PATH = "com/inspiration/catcher/config/ai.properties";

    static {loadConfig();}
    private static void loadConfig() {
        // 1. 先尝试加载用户配置文件
        if (loadFromFile(USER_CONFIG_PATH)) {logger.info("从用户配置文件加载: {}", USER_CONFIG_PATH);return;}
        // 2. 再尝试加载资源文件
        if (loadFromResource(RESOURCE_CONFIG_PATH)) {
            logger.info("从资源文件加载: {}", RESOURCE_CONFIG_PATH);return;}
        // 3. 如果都没有，使用默认值
        logger.warn("未找到配置文件，使用默认值");
        setDefaults();
    }

    // 从文件系统加载
    private static boolean loadFromFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                props.load(input);
                return true;
            } catch (IOException e) {
                logger.error("加载用户配置文件失败: {}", filePath, e);
            }
        }
        return false;
    }

    // 从资源文件加载
    private static boolean loadFromResource(String resourcePath) {
        try (InputStream input = AIConfig.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input != null) {
                props.load(input);
                return true;
            }
        } catch (IOException e) {
            logger.error("加载资源文件失败: {}", resourcePath, e);
        } catch (NullPointerException e) {
            logger.warn("资源文件不存在: {}", resourcePath);
        }
        return false;
    }

    private static void setDefaults() {
        props.setProperty("ai.api.url", "https://api.deepseek.com/v1/chat/completions");
        props.setProperty("ai.model", "deepseek-chat");
        props.setProperty("ai.max_tokens", "1000");
        props.setProperty("ai.temperature", "0.7");
        props.setProperty("ai.provider", "deepseek");
        props.setProperty("ai.fallback.enabled", "true");
        props.setProperty("ai.fallback.template", "offline");
    }

    // Getters
    public static String getApiUrl() {
        return props.getProperty("ai.api.url", "https://api.deepseek.com/v1/chat/completions");
    }

    public static String getApiKey() {
        return props.getProperty("ai.api.key", "sk-66e4a64d330f45baa9b6f24c39ef68ee");
    }

    public static String getModel() {
        return props.getProperty("ai.model", "deepseek-chat");
    }

    public static int getMaxTokens() {
        try {
            return Integer.parseInt(props.getProperty("ai.max_tokens", "3000"));
        } catch (NumberFormatException e) {
            return 1000;
        }
    }

    public static double getTemperature() {
        try {
            return Double.parseDouble(props.getProperty("ai.temperature", "0.7"));
        } catch (NumberFormatException e) {
            return 0.7;
        }
    }

    public static String getProvider() {
        return props.getProperty("ai.provider", "deepseek");
    }

    public static boolean isFallbackEnabled() {
        return Boolean.parseBoolean(props.getProperty("ai.fallback.enabled", "true"));
    }

    // 保存API密钥到用户配置文件
    public static void saveApiKey(String apiKey) {
        props.setProperty("ai.api.key", apiKey);
        // 创建用户配置目录
        Path userDir = Paths.get(USER_CONFIG_PATH).getParent();
        try {
            Files.createDirectories(userDir);
            // 保存到文件
            try (OutputStream output = Files.newOutputStream(Paths.get(USER_CONFIG_PATH))) {
                props.store(output, "AI Configuration for Inspiration Catcher");
                logger.info("API密钥已保存到: {}", USER_CONFIG_PATH);
            }
        } catch (IOException e) {
            logger.error("保存API密钥失败", e);
        }
    }
}