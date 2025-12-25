package com.multimediaplayer.extension;

import com.multimediaplayer.container.AppContext;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件加载器：动态加载外部JAR插件，封装类加载逻辑
 */
public class PluginLoader implements AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private final Map<String, URLClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();
    private final List<String> loadedPlugins = new ArrayList<>();

    public PluginLoader(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
    }

    /**
     * 加载插件（对外暴露的核心方法）
     * @param pluginPath 插件JAR路径
     * @param pluginInterface 插件需实现的接口
     * @return 插件实例
     */
    @SuppressWarnings("unchecked")
    public <T> T loadPlugin(String pluginPath, Class<T> pluginInterface) {
        File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            logger.warn("插件文件不存在：{}", pluginPath);
            return null;
        }

        try {
            // 内部创建类加载器（隐藏实现）
            URL pluginUrl = pluginFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{pluginUrl}, getClass().getClassLoader());

            // 读取插件配置（内部约定：META-INF/plugin.properties）
            Properties pluginProps = new Properties();
            // 【补充优化】判空InputStream，避免空指针
            try (InputStream propsStream = classLoader.getResourceAsStream("META-INF/plugin.properties")) {
                if (propsStream == null) {
                    throw new IOException("插件配置文件不存在：META-INF/plugin.properties");
                }
                pluginProps.load(propsStream); // 现在可正常识别load方法
            }

            // 读取插件类名和ID
            String pluginClass = pluginProps.getProperty("plugin.class"); // 可正常识别getProperty
            String pluginId = pluginProps.getProperty("plugin.id");       // 可正常识别getProperty

            // 校验配置项非空
            if (pluginClass == null || pluginId == null) {
                throw new RuntimeException("插件配置缺失：plugin.class 或 plugin.id");
            }

            // 加载并实例化插件
            Class<?> clazz = classLoader.loadClass(pluginClass);
            T pluginInstance = (T) clazz.getDeclaredConstructor().newInstance();

            // 校验是否实现指定接口
            if (!pluginInterface.isInstance(pluginInstance)) {
                throw new RuntimeException("插件未实现接口：" + pluginInterface.getName());
            }

            // 缓存插件
            pluginClassLoaders.put(pluginId, classLoader);
            loadedPlugins.add(pluginId);
            logger.info("插件加载成功：{}", pluginId);
            return pluginInstance;
        } catch (Exception e) {
            logger.error("加载插件失败", e);
            return null;
        }
    }

    @Override
    public void close() {
        pluginClassLoaders.values().forEach(classLoader -> {
            try {
                classLoader.close();
            } catch (Exception e) {
                logger.error("关闭插件类加载器失败", e);
            }
        });
        logger.info("插件加载器已释放");
    }
}
