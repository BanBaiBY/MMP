package com.multimediaplayer.container;

import com.multimediaplayer.codec.api.CodecService;
import com.multimediaplayer.codec.VideoDecoder;
import com.multimediaplayer.core.api.PlayerController;
import com.multimediaplayer.core.PlayerCore;
import com.multimediaplayer.extension.api.ConfigService;
import com.multimediaplayer.extension.ConfigManager;
import com.multimediaplayer.media.api.MediaService;
import com.multimediaplayer.media.MediaLibrary;
import com.multimediaplayer.subtitle.api.SubtitleService;
import com.multimediaplayer.subtitle.SubtitleParser;
import com.multimediaplayer.subtitle.api.I18nService;
import com.multimediaplayer.ui.api.PlayerUI;
import com.multimediaplayer.ui.PlayerUIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块装配器：负责所有模块的初始化、依赖注入、生命周期管理
 */
public class ModuleAssembler implements AppContext {
    // 存储「模块接口 → 实现类实例」的映射
    private final Map<Class<?>, Object> moduleMap = new HashMap<>();
    private final Logger globalLogger = LoggerFactory.getLogger("VideoPlayer_Global");

    /**
     * 初始化所有模块
     */
    public void initModules() {
        // 无依赖模块：extension（配置）、codec（解码）
        ConfigService configService = new ConfigManager(this);
        moduleMap.put(ConfigService.class, configService);

        CodecService codecService = new VideoDecoder(this);
        moduleMap.put(CodecService.class, codecService);

        // 低依赖模块：core（仅依赖codec/extension）
        PlayerController playerCore = new PlayerCore(this);
        moduleMap.put(PlayerController.class, playerCore);

        // 中依赖模块：media（依赖codec）、subtitle（无强依赖）
        MediaService mediaService = new MediaLibrary(this);
        moduleMap.put(MediaService.class, mediaService);

        SubtitleService subtitleService = new SubtitleParser(this);
        moduleMap.put(SubtitleService.class, subtitleService);

        I18nService i18nService = new SubtitleParser(this);

        moduleMap.put(I18nService.class, i18nService);

        // 高依赖模块：ui（依赖core/media/subtitle）
        PlayerUI playerUI = new PlayerUIController(this);
        moduleMap.put(PlayerUI.class, playerUI);

        globalLogger.info("所有模块初始化完成，共加载{}个模块", moduleMap.size());
    }

    /**
     * 销毁所有模块（释放资源）
     */
    public void destroyModules() {
        moduleMap.values().forEach(module -> {
            if (module instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) module).close();
                } catch (Exception e) {
                    globalLogger.error("模块销毁失败", e);
                }
            }
        });
        moduleMap.clear();
        globalLogger.info("所有模块已销毁");
    }

    // 实现AppContext接口：获取模块实例
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getModule(Class<T> moduleInterface) {
        T module = (T) moduleMap.get(moduleInterface);
        if (module == null) {
            throw new RuntimeException("模块未初始化：" + moduleInterface.getName());
        }
        return module;
    }

    // 实现AppContext接口：获取全局日志
    @Override
    public Logger getGlobalLogger() {
        return globalLogger;
    }
}
