package com.multimediaplayer.codec.api;

import javafx.scene.Node;

/**
 * 音视频解码渲染服务接口
 */
public interface CodecService extends AutoCloseable {
    // 初始化媒体资源
    void init(String mediaPath);
    // 启动解码+渲染
    void startDecode();
    // 暂停解码
    void pauseDecode();
    // 停止解码
    void stopDecode();
    // 获取媒体总时长（秒）
    long getMediaDuration();
    // 获取当前播放位置（秒）
    long getCurrentPosition();
    // 获取视频渲染视图（供UI展示）
    Node getVideoRenderView();
}
