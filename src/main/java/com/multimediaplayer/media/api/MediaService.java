package com.multimediaplayer.media.api;

import java.util.List;

/**
 * 媒体资源服务接口：播放列表、媒体扫描、历史记录
 */
public interface MediaService {
    // 播放列表操作
    void addToPlaylist(String mediaPath);
    void removeFromPlaylist(String mediaPath);
    List<String> getPlaylist();
    String getNextMedia(); // 获取下一首

    // 媒体扫描
    List<String> scanLocalMedia(String folderPath);

    // 历史记录
    void savePlayHistory(String mediaPath, long position);
    long getLastPlayPosition(String mediaPath);
}
