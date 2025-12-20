package com.multimediaplayer;

import javafx.scene.Scene;
import java.net.URL;
import java.util.*;

/**
 * 主题管理器：单例模式，负责切换播放器样式主题
 */
public class ThemeManager {
    // 单例实例
    private static ThemeManager instance;
    // 主题枚举（定义所有支持的主题）
    public enum Theme {
        LIGHT("浅色主题", "css/themelight.css"),
        DARK("深色主题", "css/themedark.css"),
        RETRO("复古主题", "css/themeretro.css");

        private final String displayName; // 显示名称
        private final String cssPath;     // CSS文件路径

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }
    }

    private Theme currentTheme; // 当前主题

    private ThemeManager() {
        currentTheme = Theme.LIGHT; // 默认浅色主题
    }

    // 获取单例
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // 切换主题并更新场景样式
    public void switchTheme(Theme newTheme, Scene scene) {
        if (newTheme == currentTheme) return;

        // 移除旧主题CSS
        scene.getStylesheets().removeIf(stylesheet ->
                Arrays.stream(Theme.values()).anyMatch(t -> stylesheet.endsWith(t.getCssPath()))
        );

        // 加载新主题CSS
        URL cssUrl = getClass().getClassLoader().getResource(newTheme.getCssPath());
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            currentTheme = newTheme;
            System.out.println("切换到" + newTheme.getDisplayName());
        } else {
            System.err.println("主题CSS文件未找到：" + newTheme.getCssPath());
        }
    }

    // 获取所有主题的显示名称（用于下拉框）
    public List<String> getThemeDisplayNames() {
        List<String> names = new ArrayList<>();
        for (Theme theme : Theme.values()) {
            names.add(theme.getDisplayName());
        }
        return names;
    }

    // 通过显示名称获取主题枚举
    public Theme getThemeByDisplayName(String displayName) {
        for (Theme theme : Theme.values()) {
            if (theme.getDisplayName().equals(displayName)) {
                return theme;
            }
        }
        return Theme.LIGHT; // 默认返回浅色
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }
}