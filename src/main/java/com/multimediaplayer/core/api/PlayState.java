package com.multimediaplayer.core.api;

/**
 * 播放状态枚举
 */
public enum PlayState {
    READY("就绪"), PLAYING("播放中"), PAUSED("暂停"), STOPPED("停止"), ERROR("异常");

    private final String desc;

    PlayState(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
