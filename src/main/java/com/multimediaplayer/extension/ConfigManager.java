package com.multimediaplayer.extension;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.extension.api.ConfigService;
import org.slf4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 配置管理实现：封装Properties文件读写，对外仅暴露ConfigService接口
 */
public class ConfigManager implements ConfigService, AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private final Properties properties = new Properties();
    private final Map<String, String> configMap = new HashMap<>();
    private final File configFile = new File("player_config.properties");

    public ConfigManager(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        // 初始化加载配置
        loadConfig();
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        return configMap.getOrDefault(key, defaultValue);
    }

    @Override
    public void setConfig(String key, String value) {
        if (key == null || value == null) {
            logger.warn("配置键/值不能为空");
            return;
        }
        configMap.put(key, value);
        logger.info("配置更新：{}={}", key, value);
    }

    @Override
    public void saveConfig() {
        properties.clear();
        configMap.forEach(properties::setProperty);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Video Player Configuration");
            logger.info("配置已保存到：{}", configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("保存配置失败", e);
        }
    }

    @Override
    public void loadConfig() {
        if (!configFile.exists()) {
            logger.info("配置文件不存在，使用默认配置");
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
            properties.forEach((k, v) -> configMap.put((String) k, (String) v));
            logger.info("配置加载成功，共{}项", configMap.size());
        } catch (Exception e) {
            logger.error("加载配置失败", e);
        }
    }

    @Override
    public void close() {
        // 销毁前自动保存配置
        saveConfig();
        logger.info("配置管理器已释放");
    }
}
