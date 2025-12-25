package com.multimediaplayer.subtitle.api;

import java.util.List;

/**
 * 国际化服务接口：定义文案获取、语言切换能力
 */
public interface I18nService {
    /**
     * 获取国际化文案（替换原getI18nString）
     * @param key 文案key
     * @return 本地化文案
     */
    String getMessage(String key);

    /**
     * 获取带参数的国际化文案
     * @param key 文案key
     * @param args 参数列表
     * @return 格式化后的文案
     */
    String getMessage(String key, Object... args);

    /**
     * 获取支持的语言列表
     * @return 语言列表（如["messages_zh_CN.properties-CN", "en-US"]）
     */
    List<String> getSupportedLanguages();

    /**
     * 切换语言
     * @param language 语言标识（如"messages_zh_CN.properties-CN"、"en-US"）
     */
    void setLanguage(String language);
}
