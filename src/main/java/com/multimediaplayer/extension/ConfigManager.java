package com.multimediaplayer.extension;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.extension.api.ConfigService;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 配置管理实现：封装配置读写 + 适配设置页面布局
 */
public class ConfigManager implements ConfigService, AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private final Properties properties = new Properties();
    private final Map<String, String> configMap = new HashMap<>();
    // 配置文件路径（用户目录，避免权限问题）
    private final File configFile = new File(System.getProperty("user.home") + "/.multimediaplayer/player_config.properties");
    // 配置项默认值
    private final Map<String, String> defaultConfigs = new HashMap<>();
    // 配置项校验规则（key-正则表达式）
    private final Map<String, Pattern> validateRules = new HashMap<>();

    public ConfigManager(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        // 初始化默认配置和校验规则
        initDefaultConfigs();
        initValidateRules();
        // 确保配置目录存在
        createConfigDirIfNotExist();
        // 加载配置
        loadConfig();
    }

    /**
     * 初始化默认配置（供设置重置使用）
     */
    private void initDefaultConfigs() {
        defaultConfigs.put("player.volume", "70");
        defaultConfigs.put("player.resolution", "720p");
        defaultConfigs.put("player.playSpeed", "1.0");
        defaultConfigs.put("decoder.mp3.quality", "10");
        defaultConfigs.put("player.fullscreen", "false");
    }

    /**
     * 初始化配置校验规则（供设置修改时验证）
     */
    private void initValidateRules() {
        validateRules.put("player.volume", Pattern.compile("^[0-9]{1,3}$")); // 0-999
        validateRules.put("player.resolution", Pattern.compile("^(720p|1080p|2k|4k)$"));
        validateRules.put("player.playSpeed", Pattern.compile("^0\\.5|1\\.0|1\\.5|2\\.0$"));
        validateRules.put("decoder.mp3.quality", Pattern.compile("^[1-9]|1[0-9]|20$")); // 1-20
        validateRules.put("player.fullscreen", Pattern.compile("^(true|false)$"));
    }

    /**
     * 创建配置目录（避免路径不存在）
     */
    private void createConfigDirIfNotExist() {
        File parentDir = configFile.getParentFile();
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            logger.info(created ? "配置目录创建成功：{}" : "配置目录创建失败", parentDir.getAbsolutePath());
        }
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        if (key == null || key.isEmpty()) {
            logger.warn("配置键不能为空");
            return defaultValue;
        }
        return configMap.getOrDefault(key.trim(), defaultValue);
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfig(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("配置值不是整数，使用默认值：key={}, value={}, default={}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public void setConfig(String key, String value) {
        if (key == null || key.isEmpty()) {
            logger.warn("配置键不能为空");
            return;
        }
        if (value == null) {
            logger.warn("配置值不能为空，键：{}", key);
            return;
        }
        String cleanKey = key.trim();
        String cleanValue = value.trim();
        // 先验证值合法性
        if (validateConfigValue(cleanKey, cleanValue)) {
            configMap.put(cleanKey, cleanValue);
            logger.info("配置更新：{}={}", cleanKey, cleanValue);
            // 自动保存
            saveConfig();
        } else {
            logger.error("配置值不合法，拒绝设置：{}={}", cleanKey, cleanValue);
            throw new IllegalArgumentException("配置值不合法：" + getValidateTip(cleanKey));
        }
    }

    @Override
    public void saveConfig() {
        properties.clear();
        configMap.forEach(properties::setProperty);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Video Player Configuration (Auto-Saved)");
            logger.info("配置已保存到：{}", configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("保存配置失败", e);
            throw new RuntimeException("配置保存失败：" + e.getMessage());
        }
    }

    @Override
    public void loadConfig() {
        if (!configFile.exists()) {
            logger.info("配置文件不存在，初始化默认配置：{}", configFile.getAbsolutePath());
            configMap.putAll(defaultConfigs);
            saveConfig(); // 生成默认配置文件
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
            // 兼容旧配置
            migrateOldConfig();
            // 加载到内存
            properties.forEach((k, v) -> configMap.put((String) k, (String) v));
            logger.info("配置加载成功，共{}项", configMap.size());
        } catch (Exception e) {
            logger.error("加载配置失败，使用默认配置", e);
            configMap.putAll(defaultConfigs);
        }
    }

    /**
     * 兼容旧配置（如旧key迁移）
     */
    private void migrateOldConfig() {
        if (properties.containsKey("volume") && !properties.containsKey("player.volume")) {
            String oldValue = properties.getProperty("volume");
            properties.setProperty("player.volume", oldValue);
            properties.remove("volume");
            logger.info("配置迁移：volume → player.volume，值：{}", oldValue);
        }
    }

    /**
     * 插件初始化（注入配置）
     */
    public <T> T initPlugin(T pluginInstance) {
        if (pluginInstance == null) return null;
        try {
            pluginInstance.getClass().getMethod("init", ConfigService.class).invoke(pluginInstance, this);
            logger.info("插件初始化完成：{}", pluginInstance.getClass().getSimpleName());
        } catch (Exception e) {
            logger.debug("插件无需配置注入：{}", pluginInstance.getClass().getSimpleName());
        }
        return pluginInstance;
    }

    // ==================== 设置页面适配方法 ====================
    @Override
    public Map<String, String> getAllConfigs() {
        // 返回副本，避免外部修改内存配置
        return new HashMap<>(configMap);
    }

    @Override
    public boolean resetConfigToDefault(String key) {
        if (key == null || !defaultConfigs.containsKey(key)) {
            logger.warn("无默认值的配置项：{}", key);
            return false;
        }
        String defaultValue = defaultConfigs.get(key);
        configMap.put(key, defaultValue);
        saveConfig();
        logger.info("配置项已重置为默认值：{}={}", key, defaultValue);
        return true;
    }

    @Override
    public boolean validateConfigValue(String key, String value) {
        if (key == null || value == null) return false;
        // 无校验规则的配置项直接通过
        if (!validateRules.containsKey(key)) return true;
        return validateRules.get(key).matcher(value).matches();
    }

    /**
     * 获取配置项校验提示（设置页面显示用）
     */
    public String getValidateTip(String key) {
        return switch (key) {
            case "player.volume" -> "音量需为0-999的整数";
            case "player.resolution" -> "分辨率仅支持：720p/1080p/2k/4k";
            case "player.playSpeed" -> "播放速度仅支持：0.5/1.0/1.5/2.0";
            case "decoder.mp3.quality" -> "解码质量需为1-20的整数";
            case "player.fullscreen" -> "全屏设置仅支持：true/false";
            default -> "无校验规则";
        };
    }

    /**
     * 获取所有配置项的key列表（供设置页面展示）
     */
    public String[] getConfigKeys() {
        return defaultConfigs.keySet().toArray(new String[0]);
    }

    /**
     * 获取配置项默认值
     */
    public String getDefaultConfig(String key) {
        return defaultConfigs.getOrDefault(key, "未知");
    }

    @Override
    public void close() {
        saveConfig();
        logger.info("配置管理器已释放");
    }
}