package com.multimediaplayer.media;

import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 播放列表实现：封装列表CRUD、循环/随机逻辑
 * 内部逻辑不对外暴露
 */
public class PlaylistManager {
    private final MediaLibrary mediaLibrary;
    private final Logger logger;
    private final List<String> playlist = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRandom = false; // 随机播放开关

    public PlaylistManager(MediaLibrary mediaLibrary) {
        this.mediaLibrary = mediaLibrary;
        this.logger = mediaLibrary.getAppContext().getGlobalLogger();

    }

    public void add(String mediaPath) {
        if (!playlist.contains(mediaPath)) {
            playlist.add(mediaPath);
            logger.info("添加到播放列表：{}", mediaPath);
        }
    }

    public void remove(String mediaPath) {
        playlist.remove(mediaPath);
        logger.info("从播放列表移除：{}", mediaPath);
    }

    public List<String> getPlaylist() {
        return new ArrayList<>(playlist); // 返回副本，避免外部修改
    }

    public String getNext() {
        if (playlist.isEmpty()) {
            return null;
        }

        if (isRandom) {
            currentIndex = new Random().nextInt(playlist.size());
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }

        String nextMedia = playlist.get(currentIndex);
        logger.info("下一首：{}", nextMedia);
        return nextMedia;
    }
}
