package com.multimediaplayer;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 主题管理器（单例模式）
 * 负责主题的加载、保存、切换，以及主题对应的CSS样式文件的管理
 * 支持深色/浅色两种主题切换，会持久化保存用户上次选择的主题，启动时自动加载
 */
public class ThemeManager {
    // 日志对象：用于记录主题管理相关运行状态和异常信息
    private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());

    /**
     * 主题枚举：定义支持的主题类型，包含主题显示名称和对应的CSS样式文件路径
     */
    public enum Theme {
        DARK("深色主题", "/css/themedark.css"),  // 深色主题及对应CSS路径
        LIGHT("浅色主题", "/css/themelight.css"); // 浅色主题及对应CSS路径

        private final String displayName; // 主题显示名称（用于界面展示）
        private final String cssPath;     // 主题对应的CSS样式文件路径

        /**
         * 主题枚举构造方法
         * @param displayName 主题显示名称
         * @param cssPath 主题CSS文件路径
         */
        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        // 获取主题显示名称
        public String getDisplayName() { return displayName; }
        // 获取主题CSS文件路径
        public String getCssPath() { return cssPath; }
    }

    // 单例实例（volatile保证多线程可见性）
    private static volatile ThemeManager instance;
    // 当前使用的主题（默认深色主题）
    private Theme currentTheme = Theme.DARK;
    // 偏好设置对象：用于持久化保存用户上次选择的主题
    private final Preferences prefs;
    // 偏好存储中主题对应的键名
    private static final String THEME_PREF_KEY = "last_selected_theme";

    /**
     * 私有构造方法：确保单例模式，初始化偏好设置并加载上次保存的主题
     */
    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        loadLastTheme(); // 启动时自动加载用户上次选择的主题
    }

    /**
     * 获取单例实例（双重校验锁实现，保证线程安全且高效）
     * @return ThemeManager 单例对象
     */
    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    /**
     * 从偏好存储加载上次保存的主题
     * 若加载失败（如存储值无效），则使用默认深色主题并重置存储
     */
    private void loadLastTheme() {
        // 从偏好存储中获取上次主题名称，默认使用当前主题（深色）
        String lastThemeName = prefs.get(THEME_PREF_KEY, currentTheme.name());
        logger.info("【偏好存储】读取上次主题名称：" + lastThemeName);

        try {
            // 将存储的主题名称转换为Theme枚举
            currentTheme = Theme.valueOf(lastThemeName);
            logger.info("【偏好存储】成功加载上次主题：" + currentTheme.getDisplayName());
        } catch (IllegalArgumentException e) {
            // 存储值无效时，使用默认深色主题并保存
            logger.warning("【偏好存储】加载失败，使用默认深色主题：" + e.getMessage());
            currentTheme = Theme.DARK;
            saveCurrentTheme(); // 重置无效的偏好存储
        }
    }

    /**
     * 将当前主题保存到偏好存储中，实现持久化记忆
     */
    private void saveCurrentTheme() {
        prefs.put(THEME_PREF_KEY, currentTheme.name());
        logger.info("【偏好存储】已保存当前主题：" + currentTheme.getDisplayName() + "（存储键：" + THEME_PREF_KEY + "）");
    }

    /**
     * 切换主题：替换场景中的主题CSS文件，更新当前主题并保存
     * @param newTheme 要切换的新主题
     * @param scene 需要应用主题的JavaFX场景
     */
    public void switchTheme(Theme newTheme, Scene scene) {
        // 若新主题与当前主题一致，无需重复切换
        if (newTheme == currentTheme) {
            logger.info("【主题切换】当前已为" + newTheme.getDisplayName() + "，无需重复切换");
            return;
        }
        // 场景为空时无法切换，记录错误日志
        if (scene == null) {
            logger.severe("【主题切换】场景对象为空，无法执行主题切换操作！");
            return;
        }

        // 先移除所有旧的主题CSS文件，避免样式冲突
        removeAllThemeCss(scene);
        // 添加新主题的CSS文件，添加成功后更新当前主题并保存
        if (addThemeCss(newTheme, scene)) {
            currentTheme = newTheme;
            saveCurrentTheme();
        }
    }

    /**
     * 强制加载当前主题的CSS文件（启动时调用，忽略主题是否一致）
     * 用于确保程序启动时主题样式正确加载
     * @param scene 需要应用主题的JavaFX场景
     */
    public void forceLoadCurrentTheme(Scene scene) {
        // 场景为空时无法加载，记录错误日志
        if (scene == null) {
            logger.severe("【强制加载主题】场景对象为空，无法加载主题样式！");
            return;
        }

        // 先移除所有旧的主题CSS文件，避免样式残留
        removeAllThemeCss(scene);
        // 强制添加当前主题的CSS文件
        boolean success = addThemeCss(currentTheme, scene);
        if (success) {
            logger.info("【强制加载主题】成功加载当前主题CSS：" + currentTheme.getDisplayName());
        } else {
            logger.severe("【强制加载主题】加载失败，CSS文件路径：" + currentTheme.getCssPath());
        }
    }

    /**
     * 移除场景中所有与主题相关的CSS文件
     * @param scene 需要清理CSS的JavaFX场景
     */
    public void removeAllThemeCss(Scene scene) {
        // 获取所有主题对应的CSS文件路径列表
        List<String> themeCssPaths = Arrays.stream(Theme.values())
                .map(Theme::getCssPath)
                .collect(Collectors.toList());

        // 移除场景中所有匹配主题CSS路径的样式文件
        scene.getStylesheets().removeIf(stylesheet -> {
            for (String themeCssPath : themeCssPaths) {
                if (stylesheet.endsWith(themeCssPath)) {
                    logger.info("【移除主题CSS】已移除样式文件：" + themeCssPath);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 为场景添加指定主题的CSS样式文件
     * @param theme 要添加的主题
     * @param scene 需要应用CSS的JavaFX场景
     * @return boolean 添加成功返回true，失败返回false
     */
    private boolean addThemeCss(Theme theme, Scene scene) {
        String cssPath = theme.getCssPath();
        // 获取CSS资源的URL对象
        URL resource = getClass().getResource(cssPath);

        // 资源不存在时，记录错误并返回失败
        if (resource == null) {
            logger.severe("【添加主题CSS】CSS文件不存在，路径：" + cssPath);
            return false;
        }

        // 转换为CSS文件的外部访问路径
        String cssUrl = resource.toExternalForm();
        // 避免重复添加相同的CSS文件
        if (!scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
            logger.info("【添加主题CSS】成功加载样式文件：" + cssUrl);
        } else {
            logger.info("【添加主题CSS】样式文件已存在，无需重复添加：" + cssUrl);
        }
        return true;
    }

    // -------------------------- Getter方法：提供外部访问接口 --------------------------
    /**
     * 获取当前使用的主题
     * @return Theme 当前主题
     */
    public Theme getCurrentTheme() { return currentTheme; }

    /**
     * 获取所有主题的显示名称数组（用于界面下拉框展示）
     * @return String[] 主题显示名称数组
     */
    public String[] getThemeDisplayNames() {
        return Arrays.stream(Theme.values())
                .map(Theme::getDisplayName)
                .toArray(String[]::new);
    }

    /**
     * 根据主题显示名称获取对应的Theme枚举
     * 若未找到对应主题，返回当前主题并记录警告日志
     * @param displayName 主题显示名称
     * @return Theme 对应的主题枚举
     */
    public Theme getThemeByDisplayName(String displayName) {
        for (Theme theme : Theme.values()) {
            if (theme.getDisplayName().equals(displayName)) {
                return theme;
            }
        }
        logger.warning("【主题查询】未找到对应主题：" + displayName + "，返回当前主题");
        System.out.println("OK");
        return currentTheme;
    }
}
