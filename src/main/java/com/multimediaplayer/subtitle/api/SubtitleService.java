package com.multimediaplayer.subtitle.api;

/**
 * 字幕服务接口：定义字幕加载、解析、获取核心能力
 */
public interface SubtitleService {
    /**
     * 加载字幕文件
     * @param subtitlePath 字幕文件路径（.srt/.ass等）
     * @return 是否加载成功
     */
    boolean loadSubtitle(String subtitlePath);

    /**
     * 获取当前播放进度对应的字幕文本
     * @param currentTime 播放进度（秒）
     * @return 字幕文本（无则返回空字符串）
     */
    String getCurrentSubtitle(long currentTime);

    /**
     * 清空已加载的字幕
     */
    void clearSubtitle();
}
