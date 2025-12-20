package com.multimediaplayer;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class PlayerController {
    // 布局控件
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;

    // 媒体控件
    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask;

    // 功能控件
    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;
    @FXML private Text totalTimeLabel;
    // 倍速按钮
    @FXML private Button speedBtn;

    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false;

    // 内置矢量图标
    private final Polygon playIcon;
    private final HBox pauseIcon;

    // 倍速相关 - 修改为支持菜单选择
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0; // 当前选中的倍速
    private ContextMenu speedMenu;     // 倍速选择菜单

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

        // 初始化倍速菜单
        initSpeedMenu();
    }

    @FXML
    public void initialize() {
        initCSS();
        fileNameLabel.setText("未选择文件");

        playPauseBtn.setGraphic(playIcon);

        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();

        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");

        Platform.runLater(() -> {
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        });

        // 按钮事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());

        // 初始化倍速按钮 - 修改为点击弹出菜单
        initSpeedButton();

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // 新增：初始化倍速选择菜单
    private void initSpeedMenu() {
        speedMenu = new ContextMenu();
        speedMenu.setStyle("-fx-background-color: #363636; -fx-text-fill: white;");

        // 为每个倍速选项创建菜单项
        for (double speed : speedOptions) {
            MenuItem item = new MenuItem(String.format("%.2fx", speed));
            item.setStyle("-fx-text-fill: white; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12px;");

            // 菜单项点击事件
            item.setOnAction(e -> {
                currentSpeed = speed;
                // 更新按钮显示
                updateSpeedButtonText();
                // 设置播放器倍速
                if (mediaPlayer != null) {
                    mediaPlayer.setRate(currentSpeed);
                }
            });
            speedMenu.getItems().add(item);
        }
    }

    // 修改：初始化倍速按钮 - 点击弹出菜单
    private void initSpeedButton() {
        updateSpeedButtonText();
        speedBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-family: 'Microsoft YaHei'; " +
                "-fx-font-size: 13px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 10px; " +
                "-fx-border-width: 0; " +
                "-fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");

        // 点击按钮弹出菜单
        speedBtn.setOnAction(e -> {
            if (!speedBtn.isDisabled()) {
                // 在按钮下方显示菜单
                speedMenu.show(speedBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        speedBtn.setDisable(true);
    }

    // 更新倍速按钮文本
    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

    // ---- 以下代码与原逻辑一致，仅修改 openMediaFile 中的倍速初始化 ----
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

        bgImage.setVisible(false);
        blackMask.setVisible(false);
        isMediaEnded = false;

        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(selectedMediaFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());

            // 修改：使用当前选中的倍速初始化播放器
            mediaPlayer.setRate(currentSpeed);
            updateSpeedButtonText();

            mediaPlayer.setOnPlaying(() -> {
                Platform.runLater(() -> {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    updateCenterPlayIconVisibility();
                });
            });

            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    bindProgressUpdate();
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateCenterPlayIconVisibility();
                    updateProgressSliderStyle(0.0);
                });
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.pause();
                    isPlaying = false;
                    isMediaEnded = true;
                    playPauseBtn.setGraphic(playIcon);

                    progressSlider.setValue(0.0);
                    updateProgressSliderStyle(0.0);

                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront();

                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateCenterPlayIconVisibility();
                });
            });

            mediaPlayer.setOnPaused(() -> {
                Platform.runLater(() -> {
                    updateCenterPlayIconVisibility();
                    boolean showBg = isMediaEnded;
                    bgImage.setVisible(showBg);
                    blackMask.setVisible(showBg);
                    if (showBg) {
                        bgImage.toFront();
                    }
                });
            });

            setPlaybackButtonsDisabled(false);
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);

            togglePlayPause();

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            e.printStackTrace();
            setPlaybackButtonsDisabled(true);
            mediaPlayer = null;
            updateCenterPlayIconVisibility();
        }
    }

    // ---- 其余方法（initCSS、initCenterPlayIcon 等）与原代码完全一致，无需修改 ----
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
            if (mediaPlayer != null) {
                if (e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                    togglePlayPause();
                }
            }
        });
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    private void initCenterPlayIcon() {
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && !isPlaying) {
                togglePlayPause();
            }
        });

        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
    }

    private void adjustCenterPlayIconSize() {
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();
        if (containerW == 0 || containerH == 0) return;

        double sizeRatio = 0.125;
        double iconWidth = Math.min(Math.max(containerW * sizeRatio, 40), 80);
        double iconHeight = iconWidth * 0.75;

        centerPlayIcon.getPoints().setAll(
                0.0, 0.0,
                0.0, iconHeight,
                iconWidth, iconHeight / 2
        );
    }

    private void initProgressSlider() {
        progressSlider.setOnMousePressed(e -> isDraggingProgress = true);
        progressSlider.setOnMouseReleased(e -> {
            isDraggingProgress = false;
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);
            }
        });

        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);
            }
        });
        updateProgressSliderStyle(0.0);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateProgressSliderStyle(double progress) {
        Platform.runLater(() -> {
            Node track = progressSlider.lookup(".track");
            if (track == null) return;

            double progressPercent = Math.max(0, Math.min(100, progress * 100));

            String gradientStyle = String.format(
                    "-fx-background-color: linear-gradient(to right, " +
                            "#1E90FF 0%%, " +
                            "#1E90FF %.2f%%, " +
                            "#444444 %.2f%%, " +
                            "#444444 100%%);",
                    progressPercent, progressPercent
            );

            track.setStyle(gradientStyle);
        });
    }

    private void updateTimeDisplay(Duration current, Duration total) {
        Platform.runLater(() -> {
            String currentStr = formatDuration(current);
            String totalStr = formatDuration(total);
            currentTimeLabel.setText(currentStr);
            totalTimeLabel.setText(totalStr);
        });
    }

    private void updateCenterPlayIconVisibility() {
        Platform.runLater(() -> {
            boolean visible = (mediaPlayer != null && !isPlaying);
            centerPlayIcon.setVisible(visible);
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
                mediaContainer.widthProperty().addListener((o, oldW, newW) -> adjustBgImageSize());
                mediaContainer.heightProperty().addListener((o, oldH, newH) -> adjustBgImageSize());
                adjustBgImageSize();

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

    private void adjustBgImageSize() {
        if (bgImageObj == null || mediaContainer.getWidth() == 0 || mediaContainer.getHeight() == 0) {
            return;
        }

        double imgW = bgImageObj.getWidth();
        double imgH = bgImageObj.getHeight();
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();

        double scaleW = containerW / imgW;
        double scaleH = containerH / imgH;
        double scale = Math.min(scaleW, scaleH);

        bgImage.setFitWidth(imgW * scale);
        bgImage.setFitHeight(imgH * scale);
        bgImage.setPreserveRatio(true);

        bgImage.setLayoutX((containerW - bgImage.getFitWidth()) / 2);
        bgImage.setLayoutY((containerH - bgImage.getFitHeight()) / 2);
        bgImage.toFront();
    }

    private void bindProgressUpdate() {
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null) {
                double progress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                Platform.runLater(() -> {
                    progressSlider.setValue(progress);
                    updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                });
                updateProgressSliderStyle(progress);
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseBtn.setGraphic(playIcon);
            bgImage.setVisible(false);
            blackMask.setVisible(false);
        } else {
            isMediaEnded = false;
            mediaPlayer.play();
            playPauseBtn.setGraphic(pauseIcon);
            bgImage.setVisible(false);
            blackMask.setVisible(false);
        }
        isPlaying = !isPlaying;
        updateCenterPlayIconVisibility();
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO);
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);
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
        speedBtn.setDisable(disabled); // 控制倍速按钮禁用状态
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }
}