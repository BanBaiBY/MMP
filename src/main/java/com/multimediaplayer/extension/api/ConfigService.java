package com.multimediaplayer.extension.api;

import java.util.Map;

/**
 * 配置服务接口：对外提供配置读写+设置交互能力
 */
public interface ConfigService {
    // 获取配置值（无则返回默认值）
    String getConfig(String key, String defaultValue);

    // 获取整数类型配置值
    int getIntConfig(String key, int defaultValue);

    // 设置配置值
    void setConfig(String key, String value);

    // 持久化配置到本地文件
    void saveConfig();

    // 加载本地配置
    void loadConfig();

    // 新增：获取所有配置项（供设置界面展示）
    Map<String, String> getAllConfigs();

    // 新增：重置单个配置到默认值
    boolean resetConfigToDefault(String key);

    // 新增：验证配置值合法性
    boolean validateConfigValue(String key, String value);
}