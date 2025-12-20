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
// 基础JavaFX导入
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

// 其他必要导入
import java.io.File;
import java.net.URL;
import java.util.prefs.Preferences;

        import javafx.application.Platform;
        import javafx.fxml.FXML;
        import javafx.geometry.Insets;
        import javafx.geometry.Pos;
        import javafx.scene.*;
        import javafx.scene.control.*;
        import javafx.scene.paint.Color;
        import javafx.scene.shape.Polygon;
        import javafx.scene.shape.Rectangle;
        import javafx.scene.text.Font;
        import javafx.scene.text.Text;
        import javafx.stage.FileChooser;
        import javafx.stage.Modality;
        import javafx.stage.Stage;
        import javafx.util.Duration;

        import java.io.File;
        import java.net.URL;
        import java.util.prefs.Preferences;

public class PlayerController {
    // 布局控件（原有）
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;
    @FXML private HBox topNavi; // 新增：用于主题样式修改
    @FXML private StackPane bottomNavi; // 新增：用于主题样式修改
    @FXML private Button settingsBtn; // 新增：设置按钮

    // 媒体控件（原有）
    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask;

    // 功能控件（原有）
    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;
    @FXML private Text totalTimeLabel;

    // 原有变量
    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false;

    // 内置矢量图标（原有）
    private final Polygon playIcon;
    private final HBox pauseIcon;

    // 新增：设置相关变量
    private Preferences prefs; // 用于持久化保存设置（JDK自带，无需额外依赖）
    private String themeColor = "black"; // 主题颜色：black/white/gray
    private boolean playMemory = false; // 播放记忆功能开关
    private Stage settingsStage; // 设置弹窗

    // 原有构造方法
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

    // 原有初始化方法（新增设置相关初始化）
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
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        });

        // 原有按钮事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());

        // 音量绑定（原有）
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        // ====================== 新增：设置相关初始化 ======================
        // 初始化偏好设置
        prefs = Preferences.userNodeForPackage(PlayerController.class);
        // 加载保存的设置
        loadSettings();
        // 应用主题样式
        applyTheme();
        // 设置按钮点击事件
        settingsBtn.setOnAction(e -> showSettingsDialog());

        // 原有逻辑
        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // 原有方法（无修改）
    private void initCSS() {
        URL cssUrl = getClass().getClassLoader().getResource("css/player.css");
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("CSS文件 /css/player.css 未找到！");
        }
    }

    // 原有方法（无修改）
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

    // 原有方法（无修改）
    private void initCenterPlayIcon() {
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && !isPlaying) {
                togglePlayPause();
            }
        });
        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
    }

    // 原有方法（无修改）
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

    // 原有方法（新增播放记忆保存逻辑）
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
                // 新增：拖动进度后保存播放状态
                savePlaybackState();
            }
        });

        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);
                // 新增：点击进度后保存播放状态
                savePlaybackState();
            }
        });
        updateProgressSliderStyle(0.0);
    }

    // 原有方法（无修改）
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 原有方法（无修改）
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

    // 原有方法（无修改）
    private void updateTimeDisplay(Duration current, Duration total) {
        Platform.runLater(() -> {
            String currentStr = formatDuration(current);
            String totalStr = formatDuration(total);
            currentTimeLabel.setText(currentStr);
            totalTimeLabel.setText(totalStr);
        });
    }

    // 原有方法（无修改）
    private void updateCenterPlayIconVisibility() {
        Platform.runLater(() -> {
            boolean visible = (mediaPlayer != null && !isPlaying);
            centerPlayIcon.setVisible(visible);
            if (visible) {
                adjustCenterPlayIconSize();
            }
        });
    }

    // 原有方法（无修改）
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

    // 原有方法（无修改）
    private void bindMediaViewSize() {
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());
    }

    // 原有方法（无修改）
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

    // 原有方法（新增播放记忆相关逻辑）
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
                    // 新增：播放结束时保存状态
                    savePlaybackState();
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
                    // 新增：暂停时保存播放状态
                    savePlaybackState();
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

    // 原有方法（新增播放记忆保存逻辑）
    private void bindProgressUpdate() {
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null) {
                double progress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                Platform.runLater(() -> {
                    progressSlider.setValue(progress);
                    updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                });
                updateProgressSliderStyle(progress);
                // 新增：播放进度变化时保存状态
                savePlaybackState();
            }
        });
    }

    // 原有方法（无修改）
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

    // 原有方法（无修改）
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

    // 原有方法（无修改）
    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    // 原有方法（新增播放记忆保存逻辑）
    public void cleanup() {
        // 新增：退出时保存播放状态
        savePlaybackState();
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }

    // ====================== 新增：设置相关核心方法 ======================

    /**
     * 加载本地保存的设置（主题、播放记忆）
     */
    private void loadSettings() {
        // 加载主题颜色（默认black）
        themeColor = prefs.get("themeColor", "black");
        // 加载播放记忆开关（默认false）
        playMemory = prefs.getBoolean("playMemory", false);
        // 如果启用播放记忆，加载上次播放的媒体
        if (playMemory) {
            loadLastPlayedMedia();
        }
    }

    /**
     * 保存设置到本地
     */
    private void saveSettings() {
        prefs.put("themeColor", themeColor);
        prefs.putBoolean("playMemory", playMemory);
    }

    /**
     * 应用主题样式（黑白灰，保证字体辨识度）
     */
    private void applyTheme() {
        Platform.runLater(() -> {
            String bgColor = "";
            String textColor = "";
            String topNaviBg = "";
            String bottomNaviBg = "";

            // 根据主题颜色配置样式
            switch (themeColor) {
                case "white":
                    bgColor = "#FFFFFF"; // 白色背景
                    textColor = "#000000"; // 黑色文字（保证辨识度）
                    topNaviBg = "linear-gradient(to right, #E0E0E0, #F0F0F0)";
                    bottomNaviBg = "linear-gradient(to bottom, #F0F0F0, #E0E0E0)";
                    break;
                case "gray":
                    bgColor = "#808080"; // 灰色背景
                    textColor = "#FFFFFF"; // 白色文字（保证辨识度）
                    topNaviBg = "linear-gradient(to right, #666666, #777777)";
                    bottomNaviBg = "linear-gradient(to bottom, #777777, #666666)";
                    break;
                case "black":
                default:
                    bgColor = "#000000"; // 黑色背景
                    textColor = "#FFFFFF"; // 白色文字（保证辨识度）
                    topNaviBg = "linear-gradient(to right, #444444, #555555)";
                    bottomNaviBg = "linear-gradient(to bottom, #555555, #444444)";
                    break;
            }

            // 应用根面板背景
            rootPane.setStyle("-fx-background-color: " + bgColor + ";");
            // 应用顶部导航栏样式
            topNavi.setStyle("-fx-background-color: " + topNaviBg + "; " +
                    "-fx-background-radius: 0 0 8px 8px; " +
                    "-fx-effect: dropshadow(gaussian, #000000, 5, 0, 0, 2); " +
                    "-fx-border-width: 0 0 1px 0; " +
                    "-fx-border-color: #333333;");
            // 应用底部导航栏样式
            bottomNavi.setStyle("-fx-background-color: " + bottomNaviBg + "; " +
                    "-fx-border-color: #666666 transparent transparent transparent; " +
                    "-fx-border-width: 1px 0 0 0; " +
                    "-fx-effect: innershadow(gaussian, #222222, 5, 0, 0, 2); " +
                    "-fx-drop-shadow: dropshadow(gaussian, #000000, 3, 0, 0, 1);");
            // 应用文字颜色（文件名、时长标签）
            fileNameLabel.setStyle("-fx-text-fill: " + textColor + "; " +
                    "-fx-font-family: 'Microsoft YaHei'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-padding: 0 10px; " +
                    "-fx-text-overrun: ellipsis;");
            currentTimeLabel.setStyle("-fx-fill: " + textColor + "; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-family: 'Microsoft YaHei'; " +
                    "-fx-min-width: 50px; " +
                    "-fx-text-alignment: right;");
            totalTimeLabel.setStyle("-fx-fill: " + textColor + "; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-family: 'Microsoft YaHei'; " +
                    "-fx-min-width: 50px; " +
                    "-fx-text-alignment: left;");
            // 按钮文字颜色适配
            openBtn.setStyle(openBtn.getStyle().replaceAll("-fx-text-fill: #[0-9A-Fa-f]+;", "") + "-fx-text-fill: " + textColor + ";");
            settingsBtn.setStyle(settingsBtn.getStyle().replaceAll("-fx-text-fill: #[0-9A-Fa-f]+;", "") + "-fx-text-fill: " + textColor + ";");
        });
    }

    /**
     * 显示设置弹窗
     */
    private void showSettingsDialog() {
        // 避免重复打开弹窗
        if (settingsStage != null && settingsStage.isShowing()) {
            return;
        }

        // 创建设置弹窗
        settingsStage = new Stage();
        settingsStage.setTitle("播放器设置");
        settingsStage.setWidth(350);
        settingsStage.setHeight(200);
        settingsStage.setResizable(false);
        settingsStage.initModality(Modality.APPLICATION_MODAL); // 模态弹窗（阻塞主窗口）
        settingsStage.initOwner(rootPane.getScene().getWindow()); // 设置父窗口

        // 弹窗主布局
        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(20));
        mainVBox.setAlignment(Pos.CENTER);

        // 1. 主题颜色设置
        Label themeLabel = new Label("主题颜色：");
        themeLabel.setFont(Font.font("Microsoft YaHei", 14));
        ToggleGroup themeGroup = new ToggleGroup();
        RadioButton blackRadio = new RadioButton("黑色主题");
        RadioButton whiteRadio = new RadioButton("白色主题");
        RadioButton grayRadio = new RadioButton("灰色主题");
        // 绑定单选按钮到分组
        blackRadio.setToggleGroup(themeGroup);
        whiteRadio.setToggleGroup(themeGroup);
        grayRadio.setToggleGroup(themeGroup);
        // 设置默认选中项
        switch (themeColor) {
            case "white":
                whiteRadio.setSelected(true);
                break;
            case "gray":
                grayRadio.setSelected(true);
                break;
            default:
                blackRadio.setSelected(true);
                break;
        }
        // 主题切换事件（实时预览）
        themeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == blackRadio) {
                themeColor = "black";
            } else if (newToggle == whiteRadio) {
                themeColor = "white";
            } else if (newToggle == grayRadio) {
                themeColor = "gray";
            }
            applyTheme(); // 实时应用主题
        });
        HBox themeHBox = new HBox(15, blackRadio, whiteRadio, grayRadio);
        themeHBox.setAlignment(Pos.CENTER);

        // 2. 播放记忆功能
        CheckBox memoryCheckBox = new CheckBox("启用播放记忆功能");
        memoryCheckBox.setFont(Font.font("Microsoft YaHei", 14));
        memoryCheckBox.setSelected(playMemory);
        memoryCheckBox.setOnAction(e -> playMemory = memoryCheckBox.isSelected());

        // 3. 保存按钮
        Button saveBtn = new Button("保存设置");
        saveBtn.setPrefWidth(100);
        saveBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-family: 'Microsoft YaHei'; " +
                "-fx-font-size: 13px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            saveSettings(); // 保存设置
            settingsStage.close(); // 关闭弹窗
        });

        // 组装弹窗布局
        mainVBox.getChildren().addAll(themeLabel, themeHBox, memoryCheckBox, saveBtn);

        // 设置弹窗场景
        Scene scene = new Scene(mainVBox);
        settingsStage.setScene(scene);
        settingsStage.show();

        // 弹窗关闭时自动保存设置
        settingsStage.setOnHiding(e -> saveSettings());
    }

    /**
     * 保存播放状态（文件路径+进度）
     */
    private void savePlaybackState() {
        if (!playMemory || selectedMediaFile == null || mediaPlayer == null) {
            return;
        }
        // 保存上次播放的文件路径
        prefs.put("lastMediaPath", selectedMediaFile.getAbsolutePath());
        // 保存上次播放进度（秒）
        prefs.putDouble("lastMediaProgress", mediaPlayer.getCurrentTime().toSeconds());
    }

    /**
     * 加载上次播放的媒体文件和进度
     */
    private void loadLastPlayedMedia() {
        String lastPath = prefs.get("lastMediaPath", null);
        if (lastPath == null) {
            return;
        }
        File lastFile = new File(lastPath);
        if (!lastFile.exists()) {
            return;
        }

        // 加载上次的文件
        selectedMediaFile = lastFile;
        fileNameLabel.setText(selectedMediaFile.getName());

        // 释放旧的MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(selectedMediaFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());

            // 加载上次播放进度
            double lastProgress = prefs.getDouble("lastMediaProgress", 0);
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    bindProgressUpdate();
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(0.0);
                    // 跳转到上次播放进度
                    mediaPlayer.seek(Duration.seconds(lastProgress));
                });
            });

            // 绑定播放事件（和openMediaFile保持一致）
            mediaPlayer.setOnPlaying(() -> {
                Platform.runLater(() -> {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    updateCenterPlayIconVisibility();
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
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront();
                    updateCenterPlayIconVisibility();
                    savePlaybackState();
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
                    savePlaybackState();
                });
            });

            // 进度变化监听
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!isDraggingProgress) {
                    savePlaybackState();
                }
            });

            // 启用播放按钮
            setPlaybackButtonsDisabled(false);
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);

            // 隐藏背景和蒙版
            bgImage.setVisible(false);
            blackMask.setVisible(false);
            isMediaEnded = false;

        } catch (Exception e) {
            System.err.println("加载上次播放文件失败：" + e.getMessage());
            e.printStackTrace();
            setPlaybackButtonsDisabled(true);
            mediaPlayer = null;
            updateCenterPlayIconVisibility();
        }
    }
}