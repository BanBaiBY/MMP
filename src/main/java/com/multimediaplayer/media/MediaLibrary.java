package com.multimediaplayer.media;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.codec.api.CodecService;
import com.multimediaplayer.media.api.MediaService;
import org.slf4j.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 媒体库实现：扫描本地媒体、解析元数据
 * 依赖CodecService接口，不直接耦合解码实现
 */
public class MediaLibrary implements MediaService {
    private final AppContext appContext;
    private final Logger logger;
    private final CodecService codecService;
    private final PlaylistManager playlistManager; // 关联播放列表

    // 内部私有成员
    private final List<String> mediaLibrary = new ArrayList<>();
    private final List<String> playHistory = new ArrayList<>();

    public MediaLibrary(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        this.codecService = appContext.getModule(CodecService.class);
        this.playlistManager = new PlaylistManager(this); // 初始化播放列表
    }

    @Override
    public List<String> scanLocalMedia(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            logger.warn("扫描路径不是文件夹：{}", folderPath);
            return new ArrayList<>();
        }

        List<String> scannedMedia = new ArrayList<>();
        // 内部遍历：仅支持指定格式
        File[] files = folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp4") || lowerName.endsWith(".flv") || lowerName.endsWith(".mkv");
        });

        if (files != null) {
            for (File file : files) {
                try {
                    // 调用解码接口解析元数据（解耦）
                    codecService.init(file.getPath());
                    scannedMedia.add(file.getPath());
                    mediaLibrary.add(file.getPath());
                    logger.info("扫描到媒体文件：{}", file.getPath());
                } catch (Exception e) {
                    logger.warn("跳过无效媒体文件：{}", file.getPath());
                }
            }
        }

        logger.info("扫描完成，共发现{}个有效媒体", scannedMedia.size());
        return scannedMedia;
    }

    // 播放列表操作：委托给PlaylistManager
    @Override
    public void addToPlaylist(String mediaPath) {
        playlistManager.add(mediaPath);
    }

    @Override
    public void removeFromPlaylist(String mediaPath) {
        playlistManager.remove(mediaPath);
    }

    @Override
    public List<String> getPlaylist() {
        return playlistManager.getPlaylist();
    }

    @Override
    public String getNextMedia() {
        return playlistManager.getNext();
    }

    // 历史记录操作（内部封装）
    @Override
    public void savePlayHistory(String mediaPath, long position) {
        playHistory.add(mediaPath + ":" + position);
        logger.info("保存播放历史：{} @ {}秒", mediaPath, position);
    }

    @Override
    public long getLastPlayPosition(String mediaPath) {
        // 内部查询逻辑（简化）
        return 0;
    }

    // 提供appContext的访问接口
    public AppContext getAppContext() {
        return appContext;
    }

}
