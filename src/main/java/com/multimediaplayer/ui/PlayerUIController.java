package com.multimediaplayer.ui;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.core.api.PlayerController;
import com.multimediaplayer.subtitle.api.I18nService;
import com.multimediaplayer.subtitle.api.SubtitleService;
import com.multimediaplayer.ui.api.PlayerUI;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * UI控制器
 */
public class PlayerUIController implements PlayerUI {
    // FXML组件（与player.fxml中的fx:id对应）
    @FXML private VBox videoContainer;
    @FXML private ProgressBar playProgress;
    @FXML private Button playBtn;
    @FXML private Button pauseBtn;
    @FXML private Button stopBtn;
    @FXML private Button langBtn;
    @FXML private Label stateLabel;
    @FXML private Label subtitleLabel;

    // 工程结构依赖
    private final AppContext appContext;
    private final Logger logger;
    private final PlayerController playerController;
    private final SubtitleService subtitleService;
    private final I18nService i18nService;
    private MediaPlayer mediaPlayer;

    // 构造器：依赖工程结构的AppContext
    public PlayerUIController(AppContext appContext) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        this.playerController = appContext.getModule(PlayerController.class);
        this.subtitleService = appContext.getModule(SubtitleService.class);
        this.i18nService = appContext.getModule(I18nService.class);
    }

    // ---------------------- PlayerUI接口实现 ----------------------
    @Override
    public void show(Stage primaryStage) {
        try {
            // ========== 关键修正：修复FXML路径加载 ==========
            // 1. 打印类加载器路径（调试用）
            URL fxmlUrl = getClass().getResource("/com/multimediaplayer/ui/fxml/player.fxml");
            logger.info("FXML文件路径：{}", fxmlUrl); // 打印路径，排查是否为null

            // 2. 适配模块化环境的资源加载（Java 9+）
            if (fxmlUrl == null) {
                // 备选方案：从类加载器根路径加载
                fxmlUrl = getClass().getClassLoader().getResource("com/multimediaplayer/ui/fxml/player.fxml");
                logger.info("备选FXML路径：{}", fxmlUrl);
            }

            // 3. 路径校验，提前抛错提示
            if (fxmlUrl == null) {
                throw new IOException("FXML文件不存在！请检查路径：/com/multimediaplayer/ui/fxml/player.fxml");
            }

            // 4. 初始化FXMLLoader（确保location不为null）
            FXMLLoader loader = new FXMLLoader(fxmlUrl); // 直接传入非null的URL
            loader.setController(this); // 设置当前类为控制器

            BorderPane root = loader.load(); // 此时location已设置，不会抛错
            Scene scene = new Scene(root);

            // 后续初始化逻辑（不变）
            initI18nText();
            bindButtonEvents();
            bindPlayerStateListener();

            primaryStage.setTitle(i18nService.getMessage("player.title"));
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("UI主窗口显示成功");
        } catch (IOException e) {
            logger.error("加载FXML失败", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void updateProgress(double progress) {
        playProgress.setProgress(progress / 100);
        // 修正1：字幕时间单位（PlayerCore返回的是秒，SubtitleService接收秒）
        long currentTimeSec = (long) (progress / 100 * playerController.getMediaDuration());
        // 修正2：替换getSubtitleText为getCurrentSubtitle
        showSubtitle(subtitleService.getCurrentSubtitle(currentTimeSec));
    }

    @Override
    public void updatePlayState(String state) {
        stateLabel.setText(state);
    }

    @Override
    public void showSubtitle(String text) {
        subtitleLabel.setText(text);
    }

    // ---------------------- 内部初始化方法 ----------------------
    private void initI18nText() {
        playBtn.setText(i18nService.getMessage("btn.play"));
        pauseBtn.setText(i18nService.getMessage("btn.pause"));
        stopBtn.setText(i18nService.getMessage("btn.stop"));
        langBtn.setText(i18nService.getMessage("btn.switch.lang"));
        stateLabel.setText(i18nService.getMessage("state.ready"));
    }

    private void bindButtonEvents() {
        // 播放按钮
        playBtn.setOnAction(e -> {
            playerController.play("test.mp4"); // 测试视频路径，可替换为媒体库选择
            // 修正：使用getMessage获取状态文案
            updatePlayState(i18nService.getMessage("state.playing"));
            // 加载测试字幕
            subtitleService.loadSubtitle("test.srt");
        });

        // 暂停按钮
        pauseBtn.setOnAction(e -> {
            playerController.pause();
            updatePlayState(i18nService.getMessage("state.paused"));
        });

        // 停止按钮
        stopBtn.setOnAction(e -> {
            playerController.stop();
            updatePlayState(i18nService.getMessage("state.stopped"));
            playProgress.setProgress(0);
            showSubtitle("");
        });

        // 切换语言按钮
        langBtn.setOnAction(e -> {
            // 修正：语言切换逻辑（避免索引越界）
            String currentLang = i18nService.getSupportedLanguages().get(0).equals("messages_zh_CN.properties-CN") ? "en-US" : "messages_zh_CN.properties-CN";
            i18nService.setLanguage(currentLang);
            initI18nText(); // 重新初始化控件文本
        });
    }

    private void bindPlayerStateListener() {
        // 监听播放进度
        playerController.registerProgressListener(currentPos -> {
            long totalDuration = playerController.getMediaDuration();
            if (totalDuration > 0) {
                double progress = (double) currentPos / totalDuration * 100;
                updateProgress(progress);
            }
        });

        // 监听播放状态
        playerController.registerStateListener(state -> {
            // 修正：根据PlayState枚举获取对应文案
            updatePlayState(i18nService.getMessage("state." + state.name().toLowerCase()));
        });
    }

    public void playVideo(String videoPath) {
        Media media = new Media(new File(videoPath).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);
        videoContainer.getChildren().add(mediaView); // 绑定到FXML的videoContainer
        mediaPlayer.play();
    }
}
