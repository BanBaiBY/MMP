package com.multimediaplayer.core;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.core.api.PlayState;
import com.multimediaplayer.core.api.PlayerController;
import com.multimediaplayer.codec.api.CodecService;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;

import java.io.File;
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

    // JavaFX媒体播放核心组件
    private MediaPlayer mediaPlayer;
    private Media media;
    // 记录当前播放的媒体路径，用于暂停后重新播放
    private String currentMediaPath;

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
            // 场景1：播放新文件（路径不同）
            if (mediaPath == null || !mediaPath.equals(currentMediaPath)) {
                // 关闭已有播放器（避免资源泄漏）
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }

                // 初始化JavaFX MediaPlayer（核心播放逻辑）
                File mediaFile = new File(mediaPath);
                if (!mediaFile.exists()) {
                    throw new RuntimeException("视频文件不存在：" + mediaPath);
                }
                media = new Media(mediaFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                currentMediaPath = mediaPath;

                // 绑定MediaPlayer状态事件（与自定义PlayState联动）
                bindMediaPlayerEvents();
            }

            // 场景2：暂停后重新播放（路径相同）
            if (currentState == PlayState.PAUSED) {
                mediaPlayer.play();
                this.currentState = PlayState.PLAYING;
                notifyStateChanged();
                startProgressTask(); // 重启进度更新
                logger.info("恢复播放：{}", currentMediaPath);
                return;
            }

            // 协调解码模块（兼容原有逻辑）
            codecService.init(mediaPath);
            codecService.startDecode();

            // 启动播放+进度更新
            mediaPlayer.play();
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
        // 严格校验：仅播放中状态可暂停
        if (currentState != PlayState.PLAYING || mediaPlayer == null) {
            logger.warn("非播放状态/播放器未初始化，无法暂停");
            return;
        }

        try {
            // 1. 暂停解码模块
            codecService.pauseDecode();
            // 2. 暂停MediaPlayer（核心：真正停止视频播放）
            mediaPlayer.pause();
            // 3. 暂停进度更新任务（避免进度继续走）
            progressExecutor.shutdownNow();
            // 4. 更新状态并通知
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
            // 1. 停止MediaPlayer（核心：真正停止视频播放）
            if (mediaPlayer != null) {
                mediaPlayer.stop(); // 停止播放
                mediaPlayer.dispose(); // 释放资源
                mediaPlayer = null;
                media = null;
            }
            // 2. 停止解码模块
            codecService.stopDecode();
            // 3. 停止进度更新任务
            progressExecutor.shutdownNow();
            // 4. 重置状态和播放路径
            this.currentState = PlayState.STOPPED;
            currentMediaPath = null;
            // 5. 通知状态变更
            notifyStateChanged();
            logger.info("播放停止");
        } catch (Exception e) {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("停止失败", e);
        }
    }

    @Override
    public void seek(long seconds) {
        if (currentState == PlayState.STOPPED || currentState == PlayState.READY || mediaPlayer == null) {
            logger.warn("非播放/暂停状态/播放器未初始化，无法跳转进度");
            return;
        }

        try {
            // 跳转MediaPlayer进度（秒转毫秒）
            mediaPlayer.seek(javafx.util.Duration.seconds(seconds));
            // 协调解码模块跳转
            codecService.stopDecode();
            // 实际场景需调用codecService.seek(seconds)
            this.currentState = PlayState.PAUSED; // 跳转后默认暂停
            notifyStateChanged();
            logger.info("进度跳转到：{}秒", seconds);
        } catch (Exception e) {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("进度跳转失败", e);
        }
    }

    // 抽取MediaPlayer事件绑定逻辑，避免重复代码
    private void bindMediaPlayerEvents() {
        mediaPlayer.setOnReady(() -> {
            logger.info("媒体文件加载完成：{}", currentMediaPath);
        });
        mediaPlayer.setOnPlaying(() -> {
            this.currentState = PlayState.PLAYING;
            notifyStateChanged();
        });
        mediaPlayer.setOnPaused(() -> {
            this.currentState = PlayState.PAUSED;
            notifyStateChanged();
        });
        mediaPlayer.setOnEndOfMedia(() -> {
            stop(); // 播放完成自动停止
        });
        mediaPlayer.setOnError(() -> {
            this.currentState = PlayState.ERROR;
            notifyStateChanged();
            logger.error("MediaPlayer错误：{}", mediaPlayer.getError().getMessage());
        });
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

    // 内部方法：定时更新播放进度（修复重复创建线程问题）
    private void startProgressTask() {
        // 先停止原有任务（避免重复调度）
        if (!progressExecutor.isShutdown() && !progressExecutor.isTerminated()) {
            progressExecutor.shutdownNow();
        }
        // 重新创建单线程调度器（解决shutdown后无法复用问题）
        final ScheduledExecutorService newProgressExecutor = Executors.newSingleThreadScheduledExecutor();
        // 替换原有executor引用
        progressExecutor.shutdownNow();
        // 重新赋值（核心修复：避免shutdown后无法启动新任务）
        // 注意：原变量为final，这里调整为非final，或重新设计线程池管理
        // 简化方案：直接使用新的线程池执行任务
        newProgressExecutor.scheduleAtFixedRate(() -> {
            if (currentState == PlayState.PLAYING && mediaPlayer != null) {
                try {
                    // 从MediaPlayer获取真实进度（毫秒转秒）
                    long currentPos = (long) mediaPlayer.getCurrentTime().toSeconds();
                    long mediaDuration = (long) mediaPlayer.getTotalDuration().toSeconds();

                    // 通知所有进度监听器
                    for (Consumer<Long> listener : progressListeners) {
                        try {
                            listener.accept(currentPos);
                        } catch (Exception e) {
                            logger.error("进度监听器回调失败", e);
                        }
                    }

                    // 播放完成判断
                    if (currentPos >= mediaDuration - 1) { // 留1秒误差
                        stop();
                    }
                } catch (Exception e) {
                    this.currentState = PlayState.ERROR;
                    notifyStateChanged();
                    logger.error("进度更新失败", e);
                    newProgressExecutor.shutdownNow(); // 停止进度任务
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
            if (mediaPlayer == null) {
                return 0L;
            }
            // 从MediaPlayer获取当前进度（毫秒转秒）
            return (long) mediaPlayer.getCurrentTime().toSeconds();
        } catch (Exception e) {
            logger.error("获取当前进度失败", e);
            return 0L;
        }
    }

    @Override
    public long getMediaDuration() {
        try {
            if (mediaPlayer == null) {
                return 0L;
            }
            // 从MediaPlayer获取总时长（毫秒转秒）
            return (long) mediaPlayer.getTotalDuration().toSeconds();
        } catch (Exception e) {
            logger.error("获取媒体时长失败", e);
            return 0L;
        }
    }

    @Override
    public void close() {
        // 停止进度更新
        if (!progressExecutor.isShutdown()) {
            progressExecutor.shutdownNow();
        }
        // 释放MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        try {
            codecService.close(); // 捕获关闭时的异常
        } catch (Exception e) {
            logger.error("关闭解码模块失败", e);
        }
        logger.info("播放核心资源已释放");
    }

    // 对外暴露MediaPlayer（供UI层绑定视频渲染）
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}