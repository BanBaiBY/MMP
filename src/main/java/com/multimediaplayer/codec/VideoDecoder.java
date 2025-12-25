package com.multimediaplayer.codec;

import com.multimediaplayer.codec.api.CodecService;
import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.extension.api.ConfigService;
import javafx.scene.Node;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.slf4j.Logger;
import java.io.File;

/**
 * 视频解码实现
 * 内部封装Media/MediaPlayer，对外仅暴露CodecService接口
 */
public class VideoDecoder implements CodecService {
    private final AppContext appContext;
    private final Logger logger;
    private final ConfigService configService;

    // 内部私有成员（不对外暴露）
    private Media media;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private AudioRenderer audioRenderer; // 关联音频渲染

    public VideoDecoder(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        this.configService = appContext.getModule(ConfigService.class);
        this.audioRenderer = new AudioRenderer(this); // 初始化音频渲染
    }

    @Override
    public void init(String mediaPath) {
        // 内部校验：文件是否存在
        File mediaFile = new File(mediaPath);
        if (!mediaFile.exists()) {
            throw new RuntimeException("媒体文件不存在：" + mediaPath);
        }

        // 初始化JavaFX Media
        this.media = new Media(mediaFile.toURI().toString());
        this.mediaPlayer = new MediaPlayer(media);
        this.mediaView = new MediaView(mediaPlayer);

        // 加载配置：音量、播放速度
        double volume = Double.parseDouble(configService.getConfig("player.volume", "0.8"));
        mediaPlayer.setVolume(volume);
        mediaPlayer.setRate(Double.parseDouble(configService.getConfig("player.speed", "1.0")));

        // 内部异常监听
        mediaPlayer.setOnError(() -> {
            logger.error("解码异常：{}", mediaPlayer.getError().getMessage());
        });

        logger.info("媒体初始化成功：{}，时长：{}秒", mediaPath, getMediaDuration());
    }

    @Override
    public void startDecode() {
        if (mediaPlayer == null) {
            throw new RuntimeException("请先调用init初始化媒体");
        }
        mediaPlayer.play();
        audioRenderer.startAudio(); // 启动音频渲染
        logger.info("开始解码渲染");
    }

    @Override
    public void pauseDecode() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            audioRenderer.pauseAudio();
            logger.info("解码暂停");
        }
    }

    @Override
    public void stopDecode() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            audioRenderer.stopAudio();
            logger.info("解码停止");
        }
    }

    @Override
    public long getMediaDuration() {
        return media == null ? 0 : (long) media.getDuration().toSeconds();
    }

    @Override
    public long getCurrentPosition() {
        return mediaPlayer == null ? 0 : (long) mediaPlayer.getCurrentTime().toSeconds();
    }

    @Override
    public Node getVideoRenderView() {
        return mediaView; // 仅返回渲染节点，不暴露MediaView内部方法
    }

    // 内部方法：供AudioRenderer调用（不对外）
    MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void close() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
        logger.info("解码资源已释放");
    }

    // 提供appContext的访问接口
    public AppContext getAppContext() {
        return appContext;
    }

}
