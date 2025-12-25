package com.multimediaplayer.codec;

import org.slf4j.Logger;

/**
 * 音频渲染实现（依赖VideoDecoder的MediaPlayer）
 * 内部封装音频逻辑，对外无暴露
 */
public class AudioRenderer {
    private final VideoDecoder videoDecoder;
    private final Logger logger;

    public AudioRenderer(VideoDecoder videoDecoder) {
        this.videoDecoder = videoDecoder;
        this.logger = videoDecoder.getAppContext().getGlobalLogger();

    }

    public void startAudio() {
        logger.info("音频渲染启动");
    }

    public void pauseAudio() {
        logger.info("音频渲染暂停");
    }

    public void stopAudio() {
        logger.info("音频渲染停止");
    }
}
