package com.multimediaplayer.core;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.core.api.PlayState;
import com.multimediaplayer.core.api.PlayerController;
import com.multimediaplayer.codec.api.CodecService;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 播放核心实现：管理状态机、协调解码模块、发布事件
 * 内部逻辑完全封装，对外仅暴露PlayerController接口
 */
public class PlayerCore implements PlayerController, AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private final CodecService codecService;

    // 内部私有状态
    private PlayState currentState = PlayState.READY;
    private final List<Consumer<PlayState>> stateListeners = new ArrayList<>();
    private final List<Consumer<Long>> progressListeners = new ArrayList<>();
    private final ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();

    public PlayerCore(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        this.codecService = appContext.getModule(CodecService.class);
    }

    @Override
    public void play(String mediaPath) {
        // 状态校验：异常状态禁止播放
        if (currentState == PlayState.ERROR) {
            logger.warn("当前为异常状态，无法播放");
            return;
        }

        try {
            // 协调解码模块初始化
            codecService.init(mediaPath); // 可能抛出Exception
            codecService.startDecode();   // 可能抛出Exception

            // 更新状态+通知监听器
            this.currentState = PlayState.PLAYING;
            notifyStateChanged();

            // 启动进度更新任务（内部线程）
            startProgressTask();
            logger.info("开始播放：{}", mediaPath);
        } catch (Exception e) {
            // 捕获所有异常，更新为错误状态
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("播放失败", e);
        }
    }

    @Override
    public void pause() {
        if (currentState != PlayState.PLAYING) {
            logger.warn("非播放状态，无法暂停");
            return;
        }

        try {
            codecService.pauseDecode(); // 可能抛出Exception
            this.currentState = PlayState.PAUSED;
            notifyStateChanged();
            logger.info("播放暂停");
        } catch (Exception e) {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("暂停失败", e);
        }
    }

    @Override
    public void stop() {
        try {
            codecService.stopDecode(); // 可能抛出Exception
            this.currentState = PlayState.STOPPED;
            notifyStateChanged();
            progressExecutor.shutdownNow();
            logger.info("播放停止");
        } catch (Exception e) {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("停止失败", e);
        }
    }

    @Override
    public void seek(long seconds) {
        if (currentState == PlayState.STOPPED || currentState == PlayState.READY) {
            logger.warn("非播放/暂停状态，无法跳转进度");
            return;
        }

        try {
            // 协调解码模块跳转进度（内部逻辑）
            codecService.stopDecode(); // 可能抛出Exception
            // 此处简化，实际需调用解码模块的进度跳转（如codecService.seek(seconds)）
            this.currentState = PlayState.PAUSED;
            notifyStateChanged();
            logger.info("进度跳转到：{}秒", seconds);
        } catch (Exception e) {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("进度跳转失败", e);
        }
    }

    // 内部方法：发布状态变更事件
    private void notifyStateChanged() {
        for (Consumer<PlayState> listener : stateListeners) {
            try {
                listener.accept(currentState);
            } catch (Exception e) {
                logger.error("状态监听器回调失败", e);
            }
        }
    }

    // 内部方法：定时更新播放进度
    private void startProgressTask() {
        progressExecutor.scheduleAtFixedRate(() -> {
            if (currentState == PlayState.PLAYING) {
                try {
                    long currentPos = codecService.getCurrentPosition(); // 可能抛出Exception
                    long mediaDuration = codecService.getMediaDuration(); // 可能抛出Exception

                    // 通知所有进度监听器
                    for (Consumer<Long> listener : progressListeners) {
                        try {
                            listener.accept(currentPos);
                        } catch (Exception e) {
                            logger.error("进度监听器回调失败", e);
                        }
                    }

                    // 播放完成判断
                    if (currentPos >= mediaDuration) {
                        stop();
                    }
                } catch (Exception e) {
                    this.currentState = PlayState.ERROR;
                    notifyStateChanged();
                    logger.error("进度更新失败", e);
                    progressExecutor.shutdownNow(); // 停止进度任务
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    // 实现监听器注册（仅暴露注册入口，不暴露列表）
    @Override
    public void registerStateListener(Consumer<PlayState> listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    @Override
    public void registerProgressListener(Consumer<Long> listener) {
        if (listener != null && !progressListeners.contains(listener)) {
            progressListeners.add(listener);
        }
    }

    // 状态查询方法
    @Override
    public PlayState getCurrentState() {
        return currentState;
    }

    @Override
    public long getCurrentPosition() {
        try {
            return codecService.getCurrentPosition(); // 捕获可能的异常
        } catch (Exception e) {
            logger.error("获取当前进度失败", e);
            return 0L;
        }
    }

    @Override
    public long getMediaDuration() {
        try {
            return codecService.getMediaDuration(); // 捕获可能的异常
        } catch (Exception e) {
            logger.error("获取媒体时长失败", e);
            return 0L;
        }
    }

    @Override
    public void close() {
        progressExecutor.shutdownNow();
        try {
            codecService.close(); // 捕获关闭时的异常
        } catch (Exception e) {
            logger.error("关闭解码模块失败", e);
        }
        logger.info("播放核心资源已释放");
    }
}
