package com.multimediaplayer.ui.api;

import javafx.stage.Stage;

/**
 * UI服务接口
 */
public interface PlayerUI {
    // 显示主窗口
    void show(Stage primaryStage);

    // 更新播放进度（0-100）
    void updateProgress(double progress);

    // 更新播放状态文本（如：播放中/暂停）
    void updatePlayState(String state);

    // 显示字幕文本
    void showSubtitle(String text);
}
