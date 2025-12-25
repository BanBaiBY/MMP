package com.multimediaplayer.subtitle;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.subtitle.api.I18nService;
import com.multimediaplayer.subtitle.api.SubtitleService;
import org.slf4j.Logger;

import java.util.*;

/**
 * 字幕解析 + 国际化实现（双职责封装）
 */
public class SubtitleParser implements SubtitleService, I18nService, AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private ResourceBundle messageBundle; // 国际化资源包
    private String currentLanguage = "messages_zh_CN.properties-CN"; // 默认语言
    private final List<String> supportedLanguages = Arrays.asList("messages_zh_CN.properties-CN", "en-US"); // 支持的语言

    // 字幕解析相关变量
    private final Map<Long, String> subtitleMap = new TreeMap<>(); // 时间戳→字幕映射
    private String currentSubtitle = "";

    public SubtitleParser(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        // 初始化默认语言的资源包
        reloadResourceBundle(currentLanguage);
    }

    // 重新加载资源包（语言切换时调用）
    private void reloadResourceBundle(String language) {
        try {
            Locale locale = Locale.forLanguageTag(language);
            this.messageBundle = ResourceBundle.getBundle(
                    "i18n/messages",
                    locale,
                    getClass().getClassLoader()
            );
            this.currentLanguage = language;
            logger.info("国际化资源包切换为：{}", language);
        } catch (Exception e) {
            logger.error("加载{}语言资源包失败，回退到默认中文", language, e);
            // 回退到中文
            this.messageBundle = ResourceBundle.getBundle("i18n/messages", Locale.CHINA, getClass().getClassLoader());
            this.currentLanguage = "messages_zh_CN.properties-CN";
        }
    }

    // ========== I18nService 接口实现 ==========
    @Override
    public String getMessage(String key) {
        try {
            return messageBundle.getString(key);
        } catch (Exception e) {
            logger.warn("未找到国际化文案：{}", key);
            return "[" + key + "]"; // 兜底默认值
        }
    }

    @Override
    public String getMessage(String key, Object... args) {
        String baseMsg = getMessage(key);
        return String.format(baseMsg, args);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.unmodifiableList(supportedLanguages); // 返回不可修改的列表，避免外部篡改
    }

    @Override
    public void setLanguage(String language) {
        if (supportedLanguages.contains(language)) {
            reloadResourceBundle(language);
        } else {
            logger.warn("不支持的语言：{}，当前支持：{}", language, supportedLanguages);
        }
    }

    // ========== SubtitleService 接口实现 ==========
    @Override
    public boolean loadSubtitle(String subtitlePath) {
        clearSubtitle();
        try {
            // 模拟字幕加载逻辑
            subtitleMap.put(10L, getMessage("subtitle.example.1")); // 10秒字幕
            subtitleMap.put(20L, getMessage("subtitle.example.2")); // 20秒字幕
            logger.info("字幕加载成功：{}", subtitlePath);
            return true;
        } catch (Exception e) {
            logger.error("加载字幕失败", e);
            return false;
        }
    }

    @Override
    public String getCurrentSubtitle(long currentTime) {
        // 查找当前时间对应的字幕（currentTime单位：秒）
        for (Map.Entry<Long, String> entry : subtitleMap.entrySet()) {
            if (entry.getKey() <= currentTime) {
                currentSubtitle = entry.getValue();
            } else {
                break;
            }
        }
        return currentSubtitle;
    }

    @Override
    public void clearSubtitle() {
        subtitleMap.clear();
        currentSubtitle = "";
    }

    @Override
    public void close() {
        clearSubtitle();
        messageBundle = null;
        logger.info("字幕/国际化模块已释放");
    }
}
