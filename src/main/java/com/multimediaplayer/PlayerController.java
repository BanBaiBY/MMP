package com.multimediaplayer;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.net.URL;


public class PlayerController {
    // 布局控件
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel; // 文件名显示标签

    // 媒体控件
    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask; // 黑色兜底蒙版

    // 功能控件
    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;  // 已播放时长
    @FXML private Text totalTimeLabel;    // 总时长

    private MediaPlayer mediaPlayer;
    private File selectedMediaFile; // 选中的媒体文件
    private Image bgImageObj; // 缓存背景图片
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    // 标记视频是否播放结束
    private boolean isMediaEnded = false;

    // 内置矢量图标
    private final Polygon playIcon;  // 播放三角形
    private final HBox pauseIcon;    // 暂停双矩形

    public PlayerController() {
        // 播放三角形
        playIcon = new Polygon(
                6.0, 2.0,
                6.0, 22.0,
                22.0, 12.0
        );
        playIcon.setFill(Color.WHITE);
        playIcon.setSmooth(true);

        // 暂停双矩形
        Rectangle rect1 = new Rectangle(0, 0, 7, 20);
        Rectangle rect2 = new Rectangle(10, 0, 7, 20);
        rect1.setFill(Color.WHITE);
        rect2.setFill(Color.WHITE);
        rect1.setSmooth(true);
        rect2.setSmooth(true);

        pauseIcon = new HBox(3, rect1, rect2);
        pauseIcon.setAlignment(Pos.CENTER);
        pauseIcon.setPrefSize(24, 24);
    }


    @FXML
    public void initialize() {
        initCSS();
        fileNameLabel.setText("未选择文件");

        playPauseBtn.setGraphic(playIcon);

        initCenterPlayIcon();
        initMediaContainerClick();

        initProgressSlider();

        // 初始化时长标签
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");

        // 绑定背景图片宽度与高度
        Platform.runLater(() -> {
            initBgImage(); // 延迟加载图片
            bindMediaViewSize(); // 延迟绑定视频尺寸
            // 初始化：未选择媒体文件时显示背景图+蒙版
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront(); // 强制置顶
        });

        // 按钮事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    private void initCSS() {
        URL cssUrl = getClass().getClassLoader().getResource("css/player.css");
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("CSS文件 /css/player.css 未找到！");
        }
    }

    private void initMediaContainerClick() {
        mediaContainer.setOnMouseClicked(e -> {
            // 只有存在媒体文件时才响应点击
            if (mediaPlayer != null) {
                // 避免点击居中播放图标时重复触发
                if (e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                    togglePlayPause();
                }
            }
        });

        // 给居中图标设置手型光标（补充之前缺失的光标设置）
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    // 初始化居中播放图标
    private void initCenterPlayIcon() {
        // 点击居中图标触发播放/暂停
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && !isPlaying) {
                togglePlayPause();
            }
        });

        // 窗口大小变化时，调整图标大小
        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustCenterPlayIconSize();
        });
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            adjustCenterPlayIconSize();
        });
    }

    // 调整居中图标大小
    private void adjustCenterPlayIconSize() {
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();
        if (containerW == 0 || containerH == 0) return;

        double sizeRatio = 0.125;
        double iconWidth = Math.min(Math.max(containerW * sizeRatio, 40), 80);
        double iconHeight = iconWidth * 0.75; // 保持三角形比例（宽80，高60）

        centerPlayIcon.getPoints().setAll(
                0.0, 0.0,
                0.0, iconHeight,
                iconWidth, iconHeight / 2
        );
    }

    private void initProgressSlider() {
        // 拖动进度条时暂停更新，避免冲突
        progressSlider.setOnMousePressed(e -> isDraggingProgress = true);
        progressSlider.setOnMouseReleased(e -> {
            isDraggingProgress = false;
            // 拖动结束后跳转到指定位置
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);
            }
        });

        // 点击进度条跳转
        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);
            }
        });
        // 初始化进度条样式
        updateProgressSliderStyle(0.0);
    }

    // 格式化时长
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 更新进度条渐变样式
    private void updateProgressSliderStyle(double progress) {
        // progress范围：0.0 ~ 1.0
        Platform.runLater(() -> {
            // 获取进度条轨道节点
            Node track = progressSlider.lookup(".track");
            if (track == null) return;

            // 计算蓝色区域百分比（0~100）
            double progressPercent = Math.max(0, Math.min(100, progress * 100));

            String gradientStyle = String.format(
                    "-fx-background-color: linear-gradient(to right, " +
                            "#1E90FF 0%%, " +          // 蓝色起始
                            "#1E90FF %.2f%%, " +      // 蓝色结束（进度位置）
                            "#444444 %.2f%%, " +      // 灰色起始（进度位置）
                            "#444444 100%%);",         // 灰色结束
                    progressPercent, progressPercent
            );

            track.setStyle(gradientStyle);
        });
    }

    // 更新时长显示文本
    private void updateTimeDisplay(Duration current, Duration total) {
        Platform.runLater(() -> {
            String currentStr = formatDuration(current);
            String totalStr = formatDuration(total);

            // 分别更新两个标签
            currentTimeLabel.setText(currentStr);
            totalTimeLabel.setText(totalStr);
        });
    }

    // 控制居中播放图标的显示/隐藏
    private void updateCenterPlayIconVisibility() {
        Platform.runLater(() -> {
            // 有媒体文件且未播放时显示，否则隐藏
            boolean visible = (mediaPlayer != null && !isPlaying);
            centerPlayIcon.setVisible(visible);
            // 显示时调整一次大小
            if (visible) {
                adjustCenterPlayIconSize();
            }
        });
    }

    private void initBgImage() {
        URL bgImageUrl = getClass().getClassLoader().getResource("bg.jpg");
        if (bgImageUrl == null) {
            System.err.println("背景图 bg.jpg 未找到！");
            return;
        }

        bgImageObj = new Image(bgImageUrl.toExternalForm(), true);
        bgImageObj.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() == 1.0) {
                bgImage.setImage(bgImageObj);
                // 绑定容器尺寸变化
                mediaContainer.widthProperty().addListener((o, oldW, newW) -> adjustBgImageSize());
                mediaContainer.heightProperty().addListener((o, oldH, newH) -> adjustBgImageSize());
                // 首次调整大小
                adjustBgImageSize();

                // 未选择媒体文件时：显示背景图 + 黑色蒙版
                boolean noMedia = selectedMediaFile == null;
                bgImage.setVisible(noMedia);
                blackMask.setVisible(noMedia);
                bgImage.toFront();
            }
        });
    }

    private void bindMediaViewSize() {
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());
    }

    // 背景图按原比例缩放，居中显示
    private void adjustBgImageSize() {
        if (bgImageObj == null || mediaContainer.getWidth() == 0 || mediaContainer.getHeight() == 0) {
            return;
        }

        // 恢复按原比例缩放，保证图片不变形
        double imgW = bgImageObj.getWidth();
        double imgH = bgImageObj.getHeight();
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();

        // 计算缩放比例：保证图片完整显示在容器内
        double scaleW = containerW / imgW;
        double scaleH = containerH / imgH;
        double scale = Math.min(scaleW, scaleH);

        // 设置背景图尺寸
        bgImage.setFitWidth(imgW * scale);
        bgImage.setFitHeight(imgH * scale);
        bgImage.setPreserveRatio(true); // 开启比例保持

        // 居中显示背景图
        bgImage.setLayoutX((containerW - bgImage.getFitWidth()) / 2);
        bgImage.setLayoutY((containerH - bgImage.getFitHeight()) / 2);
        bgImage.toFront(); // 保持背景图在蒙版上层
    }

    private void openMediaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择媒体文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("媒体文件", "*.mp4", "*.avi", "*.mkv", "*.mp3", "*.wav")
        );

        selectedMediaFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedMediaFile == null) {
            return;
        }

        String fileName = selectedMediaFile.getName();
        fileNameLabel.setText(fileName);

        // 选择媒体文件后：隐藏背景图 + 隐藏黑色蒙版
        bgImage.setVisible(false);
        blackMask.setVisible(false);
        isMediaEnded = false; // 重置播放结束标记

        //释放旧的MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        //创建新的Media和MediaPlayer
        try {
            // 将文件路径转为URI避免中文/特殊字符路径报错
            Media media = new Media(selectedMediaFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            //显示视频
            mediaView.setMediaPlayer(mediaPlayer);

            //初始化音量
            mediaPlayer.setVolume(volumeSlider.getValue());

            // 等待视频渲染
            mediaPlayer.setOnPlaying(() -> {
                Platform.runLater(() -> {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    updateCenterPlayIconVisibility();
                });
            });

            // 监听媒体时长加载完成，初始化进度条最大值
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    bindProgressUpdate();
                    // 初始化总时长显示
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateCenterPlayIconVisibility();
                    // 初始化进度条样式
                    updateProgressSliderStyle(0.0);
                });
            });

            // 播放结束后重置
            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.pause();
                    isPlaying = false;
                    isMediaEnded = true; // 标记为播放结束
                    playPauseBtn.setGraphic(playIcon);

                    progressSlider.setValue(0.0);
                    updateProgressSliderStyle(0.0);

                    // 播放结束：显示背景图 + 显示黑色蒙版（兜底）
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront(); // 背景图在蒙版上层

                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateCenterPlayIconVisibility();
                });
            });

            // 暂停时显示居中图标
            mediaPlayer.setOnPaused(() -> {
                Platform.runLater(() -> {
                    updateCenterPlayIconVisibility();
                    // 仅当播放结束后的暂停，才显示背景图 + 黑色蒙版
                    boolean showBg = isMediaEnded;
                    bgImage.setVisible(showBg);
                    blackMask.setVisible(showBg);
                    if (showBg) {
                        bgImage.toFront();
                    }
                });
            });

            //启用播放/暂停按钮
            setPlaybackButtonsDisabled(false);
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);

            togglePlayPause();

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            e.printStackTrace();
            // 异常时禁用按钮，避免空指针
            setPlaybackButtonsDisabled(true);
            mediaPlayer = null;
            updateCenterPlayIconVisibility();
        }
    }

    // 绑定进度条实时更新
    private void bindProgressUpdate() {
        // 监听播放进度，更新进度条和时长
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            // 拖动时不更新进度条，避免冲突
            if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null) {
                double progress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                Platform.runLater(() -> {
                    progressSlider.setValue(progress);
                    // 实时更新时长显示
                    updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                });
                updateProgressSliderStyle(progress);
            }
        });
    }

    // 切换播放/暂停
    private void togglePlayPause() {
        if (mediaPlayer == null) {
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseBtn.setGraphic(playIcon);
            // 暂停时：隐藏背景图 + 隐藏黑色蒙版
            bgImage.setVisible(false);
            blackMask.setVisible(false);
        } else {
            // 播放时：隐藏背景图 + 隐藏黑色蒙版
            isMediaEnded = false;
            mediaPlayer.play();
            playPauseBtn.setGraphic(pauseIcon);
            bgImage.setVisible(false);
            blackMask.setVisible(false);
        }
        isPlaying = !isPlaying;
        updateCenterPlayIconVisibility(); // 切换状态后更新图标显示
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO); // 回到开头
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);
            // 仅播放结束时显示背景图 + 蒙版
            boolean showBg = isMediaEnded;
            bgImage.setVisible(showBg);
            blackMask.setVisible(showBg);
            if (showBg) {
                bgImage.toFront();
            }
            updateCenterPlayIconVisibility();
            updateProgressSliderStyle(0.0);
        }
    }

    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose(); // 释放MediaPlayer资源
        }
    }
}
