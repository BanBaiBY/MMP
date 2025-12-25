package com.multimediaplayer.core.api;

import java.util.function.Consumer;

/**
 * 播放控制对外接口：UI/其他模块仅通过此接口操作播放
 */
public interface PlayerController {
    // 基础控制
    void play(String mediaPath);
    void pause();
    void stop();
    void seek(long seconds); // 进度跳转

    // 状态/进度监听
    void registerStateListener(Consumer<PlayState> listener);
    void registerProgressListener(Consumer<Long> listener);

    // 状态查询
    PlayState getCurrentState();
    long getCurrentPosition();
    long getMediaDuration();
}
