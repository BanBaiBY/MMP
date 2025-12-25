package com.multimediaplayer.extension.api;

import java.util.Map;

/**
 * 配置服务接口：对外提供配置读写能力
 */
public interface ConfigService {
    // 获取配置值（无则返回默认值）
    String getConfig(String key, String defaultValue);
    // 设置配置值
    void setConfig(String key, String value);
    // 持久化配置到本地文件
    void saveConfig();
    // 加载本地配置
    void loadConfig();
}
