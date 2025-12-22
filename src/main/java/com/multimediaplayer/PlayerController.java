package com.multimediaplayer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ScrollPane;

public class PlayerController {
    // 主题变量
    private String selectedTheme = "默认主题";
    private boolean isRememberLastPlay = false;
    private double lastPlaybackProgress = 0.0;
    private String lastPlayFilePath = "";
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "/MultimediaPlayerConfig.properties";

    // 布局控件
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;
    @FXML private Button settingsBtn; // 新增设置按钮

    // 媒体控件
    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask;

    // 基础功能控件
    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Button rewindBtn;   // << 后退30秒
    @FXML private Button forwardBtn;  // >> 快进30秒
    @FXML private Button speedBtn;
    @FXML private Button prevMediaBtn;
    @FXML private Button nextMediaBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;
    @FXML private Text totalTimeLabel;

    // 播放列表控件
    @FXML private ListView<File> playlistView;
    @FXML private Button removeFromPlaylistBtn;
    @FXML private Button clearPlaylistBtn;
    @FXML private VBox playlistContainer;
    @FXML private ToggleButton playlistToggleBtn;
    @FXML private Label playlistCountLabel;
    @FXML private TextField searchField;

    // 播放列表数据模型
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private final FilteredList<File> filteredPlaylist;
    private int currentPlayingIndex = -1;
    private boolean isAutoPlayNext = true;

    // 媒体核心变量
    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false;

    // 倍速相关
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0;
    private ContextMenu speedMenu;

    // 快进/后退时间（秒）
    private static final int SEEK_STEP = 30;

    // 防止快速切换和媒体就绪标记
    private boolean isSwitchingMedia = false;
    private boolean isMediaReady = false;

    // 内置矢量图标
    private final Polygon playIcon;
    private final HBox pauseIcon;
    private final HBox rewindIcon;
    private final HBox forwardIcon;
    private final HBox prevMediaIcon;
    private final HBox nextMediaIcon;

    private StackPane keyboardTipContainer;

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

        // 后退图标（<<）- 两个向左的三角形
        Polygon tri1Left = new Polygon(4.0, 4.0, 4.0, 20.0, 16.0, 12.0);
        Polygon tri2Left = new Polygon(12.0, 4.0, 12.0, 20.0, 24.0, 12.0);
        tri1Left.getPoints().setAll(20.0, 4.0, 20.0, 20.0, 8.0, 12.0);
        tri2Left.getPoints().setAll(12.0, 4.0, 12.0, 20.0, 0.0, 12.0);
        tri1Left.setFill(Color.WHITE);
        tri2Left.setFill(Color.WHITE);
        tri1Left.setSmooth(true);
        tri2Left.setSmooth(true);
        rewindIcon = new HBox(1, tri2Left, tri1Left);
        rewindIcon.setAlignment(Pos.CENTER);
        rewindIcon.setPrefSize(24, 24);

        // 快进图标（>>）- 两个向右的小三角形
        Polygon tri1Right = new Polygon(4.0, 4.0, 4.0, 20.0, 16.0, 12.0);
        Polygon tri2Right = new Polygon(12.0, 4.0, 12.0, 20.0, 24.0, 12.0);
        tri1Right.setFill(Color.WHITE);
        tri2Right.setFill(Color.WHITE);
        tri1Right.setSmooth(true);
        tri2Right.setSmooth(true);
        forwardIcon = new HBox(1, tri1Right, tri2Right);
        forwardIcon.setAlignment(Pos.CENTER);
        forwardIcon.setPrefSize(24, 24);

        // 上一首图标
        Polygon tri3Left = new Polygon(
                12.0, 5.0,
                12.0, 25.0,
                4.0, 15.0
        );
        tri3Left.setFill(Color.WHITE);
        tri3Left.setSmooth(true);
        prevMediaIcon = new HBox(tri3Left);
        prevMediaIcon.setAlignment(Pos.CENTER);
        prevMediaIcon.setPrefSize(24,24);

        // 下一首图标
        Polygon tri3Right = new Polygon(
                8.0, 5.0,
                8.0, 25.0,
                16.0, 15.0
        );
        tri3Right.setFill(Color.WHITE);
        tri3Right.setSmooth(true);
        nextMediaIcon = new HBox(tri3Right);
        nextMediaIcon.setAlignment(Pos.CENTER);
        nextMediaIcon.setPrefSize(24,24);

        // 初始化倍速菜单
        initSpeedMenu();

        // 初始化播放列表过滤列表
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
    }

    @FXML
    public void initialize() {
        initCSS();
        fileNameLabel.setText("未选择文件");

        // 设置按钮图标
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);
        prevMediaBtn.setGraphic(prevMediaIcon);
        nextMediaBtn.setGraphic(nextMediaIcon);

        // 基础功能初始化
        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton();
        initPrevNextButtons();

        // 播放列表功能初始化
        initPlaylist();
        initPlaylistToggle();

        // 初始化时长标签
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");

        Platform.runLater(() -> {
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();

            // 初始化键盘控制
            initializeKeyboardControls();
            showInitialKeyboardTip();
        });

        // 按钮事件绑定
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());
        settingsBtn.setOnAction(e -> openSettingsDialog());

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && isMediaReady) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        // 搜索框监听
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredPlaylist.setPredicate(file -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String searchText = newVal.toLowerCase();
                String fileName = file.getName().toLowerCase();
                return fileName.contains(searchText);
            });
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });

        // 加载所有配置（包括音量、倍速、主题）
        loadPlayConfig();

        // 设置窗口关闭事件以保存配置
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((winObs, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.setOnCloseRequest(event -> {
                            savePlayConfig(); // 关闭时保存所有配置
                        });
                    }
                });
            }
        });

        // 自动恢复上次播放
        if (isRememberLastPlay && !lastPlayFilePath.isEmpty()) {
            File lastFile = new File(lastPlayFilePath);
            if (lastFile.exists()) {
                // 确保文件在播放列表中
                if (!playlist.contains(lastFile)) {
                    playlist.add(lastFile);
                }

                int index = playlist.indexOf(lastFile);
                if (index >= 0) {
                    // 稍后播放，确保UI已加载完成
                    Platform.runLater(() -> {
                        playFromPlaylist(index);
                        if (mediaPlayer != null) {
                            mediaPlayer.setOnReady(() -> mediaPlayer.seek(Duration.seconds(lastPlaybackProgress)));
                        }
                    });
                }
            }
        }

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);

        rootPane.setFocusTraversable(true);
        rootPane.setOnMouseClicked(e -> rootPane.requestFocus());
    }

    // ==================== 键盘控制功能 ====================
    private void initializeKeyboardControls() {
        // 监听场景变化
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardEventHandlers(newScene);
            }
        });

        // 立即设置键盘处理器（如果场景已存在）
        if (rootPane.getScene() != null) {
            setupKeyboardEventHandlers(rootPane.getScene());
        }
    }

    private void setupKeyboardEventHandlers(Scene scene) {
        // 移除旧的事件处理器（避免重复）
        scene.removeEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // 添加新的键盘事件处理器
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // 添加F1帮助键的特殊处理（始终可用）
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F1) {
                showKeyboardShortcutsDialog();
                event.consume();
            }
        });
    }

    private void handleKeyPress(KeyEvent event) {
        // 如果在文本输入框中，除了F1外，忽略其他快捷键
        if (event.getTarget() instanceof TextInputControl) {
            if (event.getCode() != KeyCode.F1) {
                return;
            }
        }

        KeyCode keyCode = event.getCode();
        boolean ctrlDown = event.isControlDown();
        boolean shiftDown = event.isShiftDown();

        switch (keyCode) {
            // 播放/暂停
            case SPACE:
            case K:
                togglePlayPause();
                event.consume();
                break;

            // 进度控制
            case RIGHT:
                if (shiftDown) {
                    seekForward(); // Shift+右箭头：快进30秒
                } else if (ctrlDown) {
                    playNextMedia(); // Ctrl+右箭头：下一首
                } else {
                    seek(5); // 右箭头：快进5秒
                }
                event.consume();
                break;

            case LEFT:
                if (shiftDown) {
                    seekBackward(); // Shift+左箭头：后退30秒
                } else if (ctrlDown) {
                    playPreviousMedia(); // Ctrl+左箭头：上一首
                } else {
                    seek(-5); // 左箭头：后退5秒
                }
                event.consume();
                break;

            // 音量控制
            case UP:
                if (ctrlDown) {
                    setVolume(1.0); // Ctrl+上箭头：最大音量
                } else {
                    adjustVolume(0.1); // 上箭头：增加10%音量
                }
                event.consume();
                break;

            case DOWN:
                if (ctrlDown) {
                    setVolume(0.0); // Ctrl+下箭头：静音
                } else {
                    adjustVolume(-0.1); // 下箭头：减少10%音量
                }
                event.consume();
                break;

            // 全屏控制
            case F:
            case F11:
                toggleFullscreen();
                event.consume();
                break;

            // 倍速控制
            case DIGIT1:
            case NUMPAD1:
                setPlaybackSpeed(1);
                event.consume();
                break;

            case DIGIT2:
            case NUMPAD2:
                setPlaybackSpeed(1.5);
                event.consume();
                break;

            case DIGIT3:
            case NUMPAD3:
                setPlaybackSpeed(2);
                event.consume();
                break;

            case DIGIT0:
            case NUMPAD0:
                setPlaybackSpeed(0.5);
                event.consume();
                break;

            // 静音控制
            case M:
                toggleMute();
                event.consume();
                break;

            default:
                // 其他按键不处理
                break;
        }
    }

    // ==================== 键盘控制辅助方法 ====================
    private void seek(int seconds) {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) {
            return;
        }

        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
        double totalTime = mediaPlayer.getTotalDuration().toSeconds();
        double newTime = Math.max(0, Math.min(totalTime, currentTime + seconds));

        mediaPlayer.seek(Duration.seconds(newTime));
        double progress = newTime / totalTime;
        progressSlider.setValue(progress);
        updateProgressSliderStyle(progress);
        updateTimeDisplay(Duration.seconds(newTime), mediaPlayer.getTotalDuration());

        // 显示临时提示
        showTemporaryTip((seconds > 0 ? "快进 " : "后退 ") + Math.abs(seconds) + " 秒");
    }

    private void adjustVolume(double delta) {
        double currentVolume = volumeSlider.getValue();
        double newVolume = Math.max(0.0, Math.min(1.0, currentVolume + delta));
        volumeSlider.setValue(newVolume);
        if (mediaPlayer != null && isMediaReady) {
            mediaPlayer.setVolume(newVolume);
        }
        showTemporaryTip(String.format("音量: %.0f%%", newVolume * 100));
    }

    private void setVolume(double volume) {
        volumeSlider.setValue(volume);
        if (mediaPlayer != null && isMediaReady) {
            mediaPlayer.setVolume(volume);
        }
        showTemporaryTip(volume > 0 ? "最大音量" : "静音");
    }

    private void toggleMute() {
        if (mediaPlayer != null && isMediaReady) {
            if (mediaPlayer.getVolume() > 0) {
                // 保存当前音量并静音
                volumeSlider.setValue(0);
                mediaPlayer.setVolume(0);
                showTemporaryTip("静音");
            } else {
                // 恢复之前音量（默认为0.5）
                double restoreVolume = volumeSlider.getValue() > 0 ? volumeSlider.getValue() : 0.5;
                volumeSlider.setValue(restoreVolume);
                mediaPlayer.setVolume(restoreVolume);
                showTemporaryTip(String.format("取消静音 (%.0f%%)", restoreVolume * 100));
            }
        }
    }

    private void setPlaybackSpeed(double speed) {
        if (mediaPlayer != null && isMediaReady) {
            currentSpeed = speed;
            mediaPlayer.setRate(currentSpeed);
            updateSpeedButtonText();
            showTemporaryTip(String.format("播放速度: %.1fx", currentSpeed));
        }
    }

    private void toggleFullscreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        // 键盘控制
        boolean isFullscreen = !stage.isFullScreen();
        stage.setFullScreen(isFullscreen);

        if (isFullscreen) {
            // 全屏时显示快捷键提示
            showKeyboardShortcutsOverlay();
        } else {
            // 退出全屏时隐藏提示
            hideKeyboardShortcutsOverlay();
        }
    }

    private void showInitialKeyboardTip() {
        // 创建提示标签
        Label keyboardTipLabel = new Label("💡 按 F1 查看键盘快捷键");
        keyboardTipLabel.setStyle("-fx-background-color: rgba(30, 144, 255, 0.8); " +
                "-fx-text-fill: white; " +
                "-fx-padding: 6px 12px; " +
                "-fx-font-size: 12px; " +
                "-fx-background-radius: 15px; " +
                "-fx-cursor: hand;");
        keyboardTipLabel.setOnMouseClicked(e -> {
            showKeyboardShortcutsDialog();
            hideKeyboardTip();
        });

        keyboardTipContainer = new StackPane(keyboardTipLabel);
        keyboardTipContainer.setAlignment(Pos.TOP_RIGHT);
        keyboardTipContainer.setPadding(new Insets(10));
        keyboardTipContainer.setPickOnBounds(false);
        keyboardTipContainer.setMouseTransparent(true);

        // 添加到根面板
        rootPane.getChildren().add(keyboardTipContainer);

        // 1.5秒后自动隐藏
        Timeline hideTip = new Timeline(
                new KeyFrame(Duration.seconds(1.5), e -> hideKeyboardTip())
        );
        hideTip.play();
    }

    private void hideKeyboardTip() {
        if (keyboardTipContainer != null && rootPane.getChildren().contains(keyboardTipContainer)) {
            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(keyboardTipContainer.opacityProperty(), 0))
            );
            fadeOut.setOnFinished(e -> rootPane.getChildren().remove(keyboardTipContainer));
            fadeOut.play();
        }
    }

    private void showTemporaryTip(String message) {
        Platform.runLater(() -> {
            Label tip = new Label(message);
            tip.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 8px 12px; " +
                    "-fx-font-size: 13px; " +
                    "-fx-background-radius: 6px;");

            StackPane tipContainer = new StackPane(tip);
            tipContainer.setAlignment(Pos.CENTER);
            tipContainer.setMouseTransparent(true);

            rootPane.getChildren().add(tipContainer);

            // 自动隐藏
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1.5), e -> rootPane.getChildren().remove(tipContainer))
            );
            timeline.play();
        });
    }

    private void showKeyboardShortcutsOverlay() {
        GridPane overlay = new GridPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); " +
                "-fx-padding: 20px; " +
                "-fx-background-radius: 10px;");
        overlay.setHgap(20);
        overlay.setVgap(10);

        String[][] shortcuts = {
                {"空格 / K", "播放/暂停"},
                {"← / →", "快退/快进 5秒"},
                {"Shift + ←/→", "快退/快进 30秒"},
                {"↑ / ↓", "音量 +/- 10%"},
                {"Ctrl + ←/→", "上一首/下一首"},
                {"F / F11", "全屏切换"},
                {"ESC", "退出全屏"},
                {"M", "静音切换"}
        };

        int row = 0;
        for (String[] shortcut : shortcuts) {
            Label keyLabel = new Label(shortcut[0]);
            keyLabel.setStyle("-fx-text-fill: #1E90FF; -fx-font-weight: bold;");
            Label descLabel = new Label(shortcut[1]);
            descLabel.setStyle("-fx-text-fill: white;");

            overlay.add(keyLabel, 0, row);
            overlay.add(descLabel, 1, row);
            row++;
        }

        StackPane overlayContainer = new StackPane(overlay);
        overlayContainer.setAlignment(Pos.TOP_CENTER);
        overlayContainer.setPadding(new Insets(20));
        overlayContainer.setMouseTransparent(true);
        overlayContainer.setId("keyboardOverlay");

        rootPane.getChildren().add(overlayContainer);

        // 3秒后自动隐藏
        Timeline hideOverlay = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> rootPane.getChildren().remove(overlayContainer))
        );
        hideOverlay.play();
    }

    private void hideKeyboardShortcutsOverlay() {
        rootPane.getChildren().removeIf(node ->
                node instanceof StackPane && "keyboardOverlay".equals(node.getId()));
    }

    private void showKeyboardShortcutsDialog() {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("键盘快捷键");
            dialog.setHeaderText("多媒体播放器 - 快捷键说明");

            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            // 分类显示快捷键
            String[][][] categories = {
                    {
                            {"播放控制", ""},
                            {"空格 / K", "播放/暂停"},
                            {"ESC", "退出全屏"},
                            {"Ctrl + ← / →", "上一首/下一首"},
                    },
                    {
                            {"进度控制", ""},
                            {"← / →", "快退/快进 5秒"},
                            {"Shift + ← / →", "快退/快进 30秒"},
                    },
                    {
                            {"音量控制", ""},
                            {"↑ / ↓", "音量 +/- 10%"},
                            {"Ctrl + ↑ / ↓", "最大/最小音量"},
                            {"M", "静音切换"}
                    },
                    {
                            {"界面控制", ""},
                            {"F / F11", "全屏切换"},
                            {"F1", "显示帮助"}
                    },
                    {
                            {"功能控制", ""},
                            {"1-4", "切换倍速 (1.0x, 1.5x, 2.0x, 0.5x)"},
                    }
            };

            int col = 0;
            int maxRows = 0;

            for (String[][] category : categories) {
                VBox categoryBox = new VBox(5);
                categoryBox.setPadding(new Insets(0, 15, 0, 0));

                for (String[] item : category) {
                    HBox rowBox = new HBox(10);
                    rowBox.setAlignment(Pos.CENTER_LEFT);

                    Label keyLabel = new Label(item[0]);
                    keyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E90FF; -fx-min-width: 120px;");
                    Label descLabel = new Label(item[1]);
                    descLabel.setStyle("-fx-text-fill: #333;");

                    rowBox.getChildren().addAll(keyLabel, descLabel);
                    categoryBox.getChildren().add(rowBox);

                    if (category.length > maxRows) {
                        maxRows = category.length;
                    }
                }

                grid.add(categoryBox, col, 0);
                col++;
            }

            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);

            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefSize(800, 400);

            dialog.showAndWait();
        });
    }

    // ==================== 配置管理功能 ====================
    // 保存所有配置（新增音量、主题）
    private void savePlayConfig() {
        try {
            Properties props = new Properties();
            // 原有配置
            props.setProperty("isRememberLastPlay", String.valueOf(isRememberLastPlay));
            props.setProperty("isAutoPlayNext", String.valueOf(isAutoPlayNext));
            // 新增：保存默认音量、主题
            props.setProperty("defaultVolume", String.valueOf(volumeSlider.getValue()));
            props.setProperty("selectedTheme", selectedTheme); // 保存主题

            // 播放列表
            StringBuilder playlistStr = new StringBuilder();
            for (int i = 0; i < playlist.size(); i++) {
                playlistStr.append(playlist.get(i).getAbsolutePath());
                if (i < playlist.size() - 1) {
                    playlistStr.append("|");
                }
            }
            props.setProperty("playlist", playlistStr.toString());

            // 上次播放信息 - 只在记忆播放时保存
            if (isRememberLastPlay && currentPlayingIndex >= 0 && currentPlayingIndex < playlist.size()) {
                lastPlayFilePath = playlist.get(currentPlayingIndex).getAbsolutePath();
                lastPlaybackProgress = mediaPlayer != null && isMediaReady ?
                        mediaPlayer.getCurrentTime().toSeconds() : 0.0;
            } else {
                lastPlayFilePath = "";
                lastPlaybackProgress = 0.0;
            }

            props.setProperty("lastPlayFilePath", lastPlayFilePath);
            props.setProperty("lastPlaybackProgress", String.valueOf(lastPlaybackProgress));

            // 写入文件
            File configFile = new File(CONFIG_FILE_PATH);
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            FileWriter writer = new FileWriter(configFile);
            props.store(writer, "Multimedia Player Configuration");
            writer.close();
        } catch (Exception e) {
            System.err.println("保存配置失败：" + e.getMessage());
        }
    }

    private void loadPlayConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            return;
        }

        try {
            Properties props = new Properties();
            props.load(new FileReader(configFile));

            // 原有配置恢复
            isRememberLastPlay = Boolean.parseBoolean(props.getProperty("isRememberLastPlay", "false"));
            isAutoPlayNext = Boolean.parseBoolean(props.getProperty("isAutoPlayNext", "true"));

            // 新增：恢复默认音量、主题
            // 恢复默认音量
            double savedVolume = Double.parseDouble(props.getProperty("defaultVolume", "0.5"));
            volumeSlider.setValue(savedVolume);
            // 恢复主题（预留）
            selectedTheme = props.getProperty("selectedTheme", "默认主题");

            // 播放列表恢复
            String playlistStr = props.getProperty("playlist", "");
            if (!playlistStr.isEmpty()) {
                playlist.clear();
                String[] filePaths = playlistStr.split("\\|");
                for (String path : filePaths) {
                    File file = new File(path);
                    if (file.exists()) {
                        playlist.add(file);
                    }
                }
                playlistView.setItems(FXCollections.observableArrayList(playlist));
            }

            // 上次播放信息恢复
            lastPlayFilePath = props.getProperty("lastPlayFilePath", "");
            lastPlaybackProgress = Double.parseDouble(props.getProperty("lastPlaybackProgress", "0.0"));

        } catch (Exception e) {
            System.err.println("加载配置失败：" + e.getMessage());
        }
    }


    // 设置对话框功能
    private void openSettingsDialog() {
        Dialog<Void> settingsDialog = new Dialog<>();
        settingsDialog.setTitle("播放器设置");
        settingsDialog.setHeaderText("自定义播放器行为");
        settingsDialog.initOwner(rootPane.getScene().getWindow());

        DialogPane dialogPane = settingsDialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #222222; -fx-text-fill: #ffffff;");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox settingsContent = new VBox(15);
        settingsContent.setPadding(new Insets(15));
        settingsContent.setStyle("-fx-background-color: #222222;");

        // 1. 自动播放下一首开关
        CheckBox autoPlayNextCheckBox = new CheckBox("播放结束自动播放下一首");
        autoPlayNextCheckBox.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");
        autoPlayNextCheckBox.setSelected(isAutoPlayNext);

        // 2. 记忆上次内容开关
        CheckBox rememberLastPlayCheckBox = new CheckBox("记忆上次播放内容（列表+播放进度）");
        rememberLastPlayCheckBox.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");
        rememberLastPlayCheckBox.setSelected(isRememberLastPlay);

        // 3. 默认音量设置
        Label volumeLabel = new Label("默认音量：");
        volumeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");
        Slider defaultVolumeSlider = new Slider(0.0, 1.0, volumeSlider.getValue());
        defaultVolumeSlider.setPrefWidth(150);
        HBox volumeBox = new HBox(10, volumeLabel, defaultVolumeSlider);
        volumeBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Double> speedComboBox = new ComboBox<>();
        speedComboBox.getItems().addAll(speedOptions);
        speedComboBox.setValue(currentSpeed);
        speedComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2fx", item));
                    setStyle(
                            "-fx-text-fill: #ffffff; " +
                                    "-fx-background-color: #363636; " +
                                    "-fx-font-family: 'Microsoft YaHei'; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-padding: 5px 10px;"
                    );
                }
                this.hoverProperty().addListener((obs, oldHover, newHover) -> {
                    if (newHover && !empty && item != null) {
                        setStyle(
                                "-fx-text-fill: #ffffff; " +
                                        "-fx-background-color: #4a4a4a; " +
                                        "-fx-font-family: 'Microsoft YaHei'; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-padding: 5px 10px;"
                        );
                    } else if (!empty && item != null) {
                        setStyle(
                                "-fx-text-fill: #ffffff; " +
                                        "-fx-background-color: #363636; " +
                                        "-fx-font-family: 'Microsoft YaHei'; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-padding: 5px 10px;"
                        );
                    }
                });
            }
        });

        speedComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2fx", item));
                    setStyle(
                            "-fx-text-fill: #ffffff; " +
                                    "-fx-background-color: #363636; " +
                                    "-fx-font-family: 'Microsoft YaHei'; " +
                                    "-fx-font-size: 12px;"
                    );
                }
            }
        });

        speedComboBox.setStyle(
                "-fx-background-color: #363636; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-size: 12px; " +
                        "-fx-pref-width: 80px; " +
                        "-fx-control-inner-background: #363636; " +
                        "-fx-selection-bar: #505050; " +
                        "-fx-selection-bar-text: #ffffff;"
        );

        // 5. 主题选择（加载保存的主题）
        Label themeLabel = new Label("播放器主题：");
        themeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");

        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("默认主题", "深色主题", "浅色主题");
        themeComboBox.setValue(selectedTheme);
        themeComboBox.setStyle(
                "-fx-background-color: #363636; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: #999999; " +
                        "-fx-font-size: 12px; " +
                        "-fx-pref-width: 120px; " +
                        "-fx-control-inner-background: #363636; " +
                        "-fx-selection-bar: #505050; " +
                        "-fx-selection-bar-text: #ffffff;"
        );

        HBox themeBox = new HBox(10, themeLabel, themeComboBox);
        themeBox.setAlignment(Pos.CENTER_LEFT);

        // 添加所有设置项
        settingsContent.getChildren().addAll(
                autoPlayNextCheckBox,
                rememberLastPlayCheckBox,
                volumeBox,
                themeBox
        );
        dialogPane.setContent(settingsContent);

        // 处理设置保存
        settingsDialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // 原有设置保存
                isAutoPlayNext = autoPlayNextCheckBox.isSelected();
                double newVolume = defaultVolumeSlider.getValue();
                volumeSlider.setValue(newVolume);
                if (mediaPlayer != null && isMediaReady) {
                    mediaPlayer.setVolume(newVolume);
                }
                currentSpeed = speedComboBox.getValue();
                updateSpeedButtonText();
                if (mediaPlayer != null && isMediaReady) {
                    mediaPlayer.setRate(currentSpeed);
                }
                isRememberLastPlay = rememberLastPlayCheckBox.isSelected();

                // 新增：保存主题选择
                selectedTheme = themeComboBox.getValue();

                // 立即保存所有配置
                savePlayConfig();
            }
            return null;
        });

        settingsDialog.showAndWait();
    }

    // ==================== 播放控制功能 ====================
    // 后退30秒逻辑
    private void seekBackward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) {
            return;
        }

        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
        double newTime = Math.max(0, currentTime - SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        double progress = newTime / mediaPlayer.getTotalDuration().toSeconds();
        progressSlider.setValue(progress);
        updateProgressSliderStyle(progress);
        updateTimeDisplay(Duration.seconds(newTime), mediaPlayer.getTotalDuration());
    }

    // 快进30秒逻辑
    private void seekForward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) {
            return;
        }

        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
        double totalTime = mediaPlayer.getTotalDuration().toSeconds();
        double newTime = Math.min(totalTime, currentTime + SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        double progress = newTime / totalTime;
        progressSlider.setValue(progress);
        updateProgressSliderStyle(progress);
        updateTimeDisplay(Duration.seconds(newTime), mediaPlayer.getTotalDuration());
    }

    // 倍速菜单初始化
    private void initSpeedMenu() {
        speedMenu = new ContextMenu();
        speedMenu.setStyle("-fx-background-color: #363636; -fx-text-fill: white;");

        for (double speed : speedOptions) {
            MenuItem item = new MenuItem(String.format("%.2fx", speed));
            item.setStyle("-fx-text-fill: white; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12px;");

            item.setOnAction(e -> {
                currentSpeed = speed;
                updateSpeedButtonText();
                if (mediaPlayer != null && isMediaReady) {
                    mediaPlayer.setRate(currentSpeed);
                }
            });
            speedMenu.getItems().add(item);
        }
    }

    // 初始化倍速按钮
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

        speedBtn.setOnAction(e -> {
            if (!speedBtn.isDisabled()) {
                speedMenu.show(speedBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        speedBtn.setDisable(true);
    }

    // 初始化上一首/下一首按钮
    private void initPrevNextButtons() {
        // 上一首按钮样式
        prevMediaBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-border-width: 0; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        prevMediaBtn.setPrefWidth(40.0);
        prevMediaBtn.setPrefHeight(30.0);
        prevMediaBtn.setAlignment(Pos.CENTER);

        // 下一首按钮样式
        nextMediaBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-border-width: 0; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        nextMediaBtn.setPrefWidth(40.0);
        nextMediaBtn.setPrefHeight(30.0);
        nextMediaBtn.setAlignment(Pos.CENTER);

        // 绑定事件
        prevMediaBtn.setOnAction(e -> playPreviousMedia());
        nextMediaBtn.setOnAction(e -> playNextMedia());

        // 初始禁用
        updatePrevNextBtnStatus();
    }

    // 上一首媒体逻辑
    private void playPreviousMedia() {
        if (isSwitchingMedia || playlist.isEmpty()) {
            return;
        }
        if (currentPlayingIndex > 0) {
            playFromPlaylist(currentPlayingIndex - 1);
        }
    }

    // 下一首媒体逻辑
    private void playNextMedia() {
        if (isSwitchingMedia || playlist.isEmpty()) {
            return;
        }
        if (currentPlayingIndex < playlist.size() - 1) {
            playFromPlaylist(currentPlayingIndex + 1);
        }
    }

    private void updatePrevNextBtnStatus() {
        // 播放列表为空，两个按钮都禁用
        if (playlist.isEmpty()) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        // 当前无播放索引（未播放任何曲目），两个按钮都禁用
        if (currentPlayingIndex == -1) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        // 当前是第一首，上一首禁用，下一首根据是否有下一首判断
        prevMediaBtn.setDisable(currentPlayingIndex <= 0);
        // 当前是最后一首，下一首禁用，上一首根据是否有上一首判断
        nextMediaBtn.setDisable(currentPlayingIndex >= playlist.size() - 1);
    }

    // 更新倍速按钮文本
    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

    // 播放列表折叠/展开初始化方法
    private void initPlaylistToggle() {
        playlistContainer.setOpacity(0.0);

        playlistToggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                playlistContainer.setVisible(true);
                Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(playlistContainer.opacityProperty(), 1.0))
                );
                fadeIn.play();
            } else {
                Timeline fadeOut = new Timeline(
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(playlistContainer.opacityProperty(), 0.0))
                );
                fadeOut.setOnFinished(e -> playlistContainer.setVisible(false));
                fadeOut.play();
            }
        });

        playlistToggleBtn.setSelected(false);
        playlistContainer.setVisible(false);
    }

    // 初始化播放列表
    private void initPlaylist() {
        playlistView.setItems(filteredPlaylist);

        playlistView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(file.getName());
                    if (playlist.indexOf(file) == currentPlayingIndex) {
                        setStyle("-fx-text-fill: #1E90FF; -fx-font-weight: bold; -fx-background-color: rgba(30, 144, 255, 0.1);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // 点击当前播放项切换播放/暂停，点击其他项播放新文件
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                File selectedFile = playlistView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    int originalIndex = playlist.indexOf(selectedFile);
                    if (originalIndex == currentPlayingIndex && mediaPlayer != null && isMediaReady) {
                        togglePlayPause();
                    } else {
                        playFromPlaylist(originalIndex);
                    }
                }
            }
        });

        playlist.addListener((javafx.collections.ListChangeListener<File>) change -> {
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });

        updatePlaylistCount();
    }

    // 更新播放列表计数
    private void updatePlaylistCount() {
        int totalCount = playlist.size();
        int filteredCount = filteredPlaylist.size();

        if (searchField.getText().isEmpty()) {
            playlistCountLabel.setText(totalCount + " 首");
        } else {
            playlistCountLabel.setText(filteredCount + "/" + totalCount + " 首");
        }
    }

    // 从播放列表播放指定索引的文件
    private void playFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        // 防止快速切换冲突
        if (isSwitchingMedia) {
            return;
        }

        isSwitchingMedia = true;
        isMediaReady = false;

        try {
            File file = playlist.get(index);
            currentPlayingIndex = index;

            // 释放旧的MediaPlayer
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                } catch (Exception e) {
                    System.err.println("释放MediaPlayer时出错: " + e.getMessage());
                }
                mediaPlayer = null;
            }

            // 强制重置倍速
            currentSpeed = 1.0;
            updateSpeedButtonText();

            // 更新UI状态
            fileNameLabel.setText(file.getName());
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);
            currentTimeLabel.setText("00:00");
            totalTimeLabel.setText("00:00");

            // 先显示背景图和蒙版，等待媒体加载
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
            centerPlayIcon.setVisible(false);
            isMediaEnded = false;

            // 刷新列表项样式
            playlistView.refresh();
            setPlaybackButtonsDisabled(true); // 禁用直到媒体准备就绪

            // 创建新的Media对象
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // 设置音量和倍速
            if (volumeSlider.getValue() > 0) {
                mediaPlayer.setVolume(volumeSlider.getValue());
            }
            mediaPlayer.setRate(currentSpeed);

            // 监听媒体准备就绪
            mediaPlayer.setOnReady(() -> Platform.runLater(() -> {
                try {
                    isMediaReady = true;

                    // 再次确认倍速
                    currentSpeed = 1.0;
                    mediaPlayer.setRate(currentSpeed);
                    updateSpeedButtonText();

                    // 检查媒体是否有效
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    if (totalDuration == null || totalDuration.isUnknown()) {
                        throw new RuntimeException("无法获取媒体时长");
                    }

                    // 绑定进度更新
                    bindProgressUpdate();

                    // 更新总时长显示
                    updateTimeDisplay(Duration.ZERO, totalDuration);

                    // 重置进度条
                    progressSlider.setValue(0.0);
                    updateProgressSliderStyle(0.0);

                    // 开始播放
                    mediaPlayer.play();
                    isPlaying = true;
                    playPauseBtn.setGraphic(pauseIcon);

                    // 隐藏背景图和蒙版
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);

                    updateCenterPlayIconVisibility();

                    // 刷新列表项样式
                    playlistView.refresh();
                    setPlaybackButtonsDisabled(false); // 启用播放控件

                    // 更新上一首/下一首按钮状态
                    updatePrevNextBtnStatus();

                    isSwitchingMedia = false;
                } catch (Exception e) {
                    System.err.println("媒体准备就绪时发生错误: " + e.getMessage());
                    handleMediaError(file);
                    isSwitchingMedia = false;
                }
            }));

            // 监听播放结束
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                handleMediaEnd();
                isSwitchingMedia = false;
            }));

            // 监听暂停和播放
            mediaPlayer.setOnPaused(() -> Platform.runLater(this::updateCenterPlayIconVisibility));

            mediaPlayer.setOnPlaying(() -> Platform.runLater(this::updateCenterPlayIconVisibility));

            // 监听错误
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                handleMediaError(file);
                isSwitchingMedia = false;
            }));

            // 添加媒体播放状态监听
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> Platform.runLater(() -> {
                if (newStatus == MediaPlayer.Status.STOPPED ||
                        newStatus == MediaPlayer.Status.HALTED) {
                    isSwitchingMedia = false;
                }
            }));

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            handleMediaError(playlist.get(index));
            isSwitchingMedia = false;
        }
    }

    // 媒体错误处理方法
    private void handleMediaError(File file) {
        System.err.println("媒体播放错误：" + (mediaPlayer != null && mediaPlayer.getError() != null ?
                mediaPlayer.getError().getMessage() : "未知错误"));

        // 重置状态
        isPlaying = false;
        playPauseBtn.setGraphic(playIcon);
        isMediaReady = false;

        // 显示背景图和蒙版
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();

        // 更新UI
        playlistView.refresh();
        updateCenterPlayIconVisibility();
        setPlaybackButtonsDisabled(true);
        updatePrevNextBtnStatus();

        // 从播放列表中移除损坏的文件
        if (file != null && playlist.contains(file)) {
            playlist.remove(file);
            currentPlayingIndex = -1;
            updatePlaylistCount();
        }

        // 显示错误提示
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("播放错误");
        alert.setHeaderText("无法播放文件");
        alert.setContentText("文件格式可能不受支持或已损坏: " +
                (file != null ? file.getName() : "未知文件"));
        alert.showAndWait();
    }

    // 处理媒体播放结束
    private void handleMediaEnd() {
        if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
            // 自动播放前重置倍速
            currentSpeed = 1.0;
            updateSpeedButtonText();
            // 自动播放下一曲
            playFromPlaylist(currentPlayingIndex + 1);
            // 补充：更新按钮状态
            updatePrevNextBtnStatus();
        } else {
            // 重置播放状态
            isPlaying = false;
            isMediaEnded = true;
            playPauseBtn.setGraphic(playIcon);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);
            // 播放结束时显示背景图和蒙版
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();

            if (mediaPlayer != null && isMediaReady) {
                updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
            } else {
                currentTimeLabel.setText("00:00");
                totalTimeLabel.setText("00:00");
            }

            updateCenterPlayIconVisibility();
            playlistView.refresh();
            // 更新按钮状态
            updatePrevNextBtnStatus();
        }
    }

    // 从播放列表移除选中项
    private void removeSelectedFromPlaylist() {
        File selectedFile = playlistView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return;
        }

        int originalIndex = playlist.indexOf(selectedFile);
        boolean isCurrentPlaying = (originalIndex == currentPlayingIndex);

        // 先移除文件
        playlist.remove(selectedFile);

        if (isCurrentPlaying) {
            // 当前正在播放的项被删除
            stopMedia(); // 停止并重置播放器

            // 自动播放下一首（如果开启且存在）
            if (isAutoPlayNext && !playlist.isEmpty()) {
                int nextIndex = originalIndex;
                if (nextIndex >= playlist.size()) {
                    nextIndex = playlist.size() - 1;
                }
                if (nextIndex >= 0) {
                    playFromPlaylist(nextIndex);
                    return;
                }
            }

            // 否则：没有自动播放或列表为空
            currentPlayingIndex = -1;
            fileNameLabel.setText("未选择文件");
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
            setPlaybackButtonsDisabled(true);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);
            currentTimeLabel.setText("00:00");
            totalTimeLabel.setText("00:00");
            centerPlayIcon.setVisible(false);
        } else {
            // 调整当前播放索引（如果被删项在当前项之前）
            if (originalIndex < currentPlayingIndex) {
                currentPlayingIndex--;
            }
        }

        playlistView.refresh();
        updatePlaylistCount();
        updatePrevNextBtnStatus();
    }

    // 清空播放列表
    private void clearPlaylist() {
        // 先停止并彻底清理播放器
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        // 重置状态
        playlist.clear();
        currentPlayingIndex = -1;
        isPlaying = false;
        isMediaEnded = true;
        isSwitchingMedia = false;
        isMediaReady = false;

        // 重置 UI
        fileNameLabel.setText("未选择文件");
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();

        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");
        progressSlider.setValue(0.0);
        updateProgressSliderStyle(0.0);
        playPauseBtn.setGraphic(playIcon);
        centerPlayIcon.setVisible(false);

        // 禁用播放控件
        setPlaybackButtonsDisabled(true);

        // 刷新视图
        playlistView.refresh();
        updatePlaylistCount();
        updatePrevNextBtnStatus();
    }

    // 打开媒体文件
    private void openMediaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择媒体文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("媒体文件", "*.mp4", "*.avi", "*.mkv", "*.mp3", "*.wav", "*.flv", "*.mov", "*.wmv"),
                new FileChooser.ExtensionFilter("视频文件", "*.mp4", "*.avi", "*.mkv", "*.flv", "*.mov", "*.wmv"),
                new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav", "*.aac", "*.flac"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        selectedMediaFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedMediaFile == null) {
            return;
        }

        // 添加前先重置倍速
        currentSpeed = 1.0;
        updateSpeedButtonText();

        // 添加到播放列表
        if (!playlist.contains(selectedMediaFile)) {
            playlist.add(selectedMediaFile);
            currentPlayingIndex = playlist.size() - 1;
        } else {
            currentPlayingIndex = playlist.indexOf(selectedMediaFile);
        }
        searchField.clear();
        playlistView.getSelectionModel().select(currentPlayingIndex);

        // 播放选中的文件
        playFromPlaylist(currentPlayingIndex);
    }

    // 更新按钮禁用状态
    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        speedBtn.setDisable(disabled);
        rewindBtn.setDisable(disabled);
        forwardBtn.setDisable(disabled);
        prevMediaBtn.setDisable(disabled || playlist.isEmpty() || currentPlayingIndex <= 0);
        nextMediaBtn.setDisable(disabled || playlist.isEmpty() || currentPlayingIndex >= playlist.size() - 1);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && isMediaReady && !isPlaying);
    }

    // 切换播放/暂停
    private void togglePlayPause() {
        if (mediaPlayer == null || !isMediaReady) {
            // 如果有播放列表项，播放第一个
            if (!playlist.isEmpty() && currentPlayingIndex == -1) {
                playFromPlaylist(0);
                updatePrevNextBtnStatus();
            }
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseBtn.setGraphic(playIcon);
        } else {
            isMediaEnded = false;
            mediaPlayer.play();
            playPauseBtn.setGraphic(pauseIcon);
        }
        bgImage.setVisible(false);
        blackMask.setVisible(false);
        isPlaying = !isPlaying;
        updateCenterPlayIconVisibility();
    }

    // ==================== 基础UI功能 ====================
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
            if (mediaPlayer != null && isMediaReady) {
                if (e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                    togglePlayPause();
                }
            }
        });
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    private void initCenterPlayIcon() {
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && !isPlaying) {
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
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                // 参数有效性校验
                if (!Double.isNaN(progress) && !Double.isInfinite(progress)) {
                    mediaPlayer.seek(Duration.seconds(seekTime));
                    updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(progress);
                }
            }
        });

        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                double progress = progressSlider.getValue();
                if (!Double.isNaN(progress) && !Double.isInfinite(progress)) {
                    mediaPlayer.seek(Duration.seconds(seekTime));
                    updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(progress);
                }
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
        // 参数有效性校验
        if (Double.isNaN(progress) || Double.isInfinite(progress)) {
            progress = 0.0;
        }
        progress = Math.max(0.0, Math.min(1.0, progress));

        final double finalProgress = progress;

        Platform.runLater(() -> {
            Node track = progressSlider.lookup(".track");
            if (track == null) return;

            double progressPercent = finalProgress * 100;

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
            boolean visible = (mediaPlayer != null && isMediaReady && !isPlaying);
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
            if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null && isMediaReady) {
                double progress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                if (!Double.isNaN(progress) && !Double.isInfinite(progress)) {
                    progress = Math.max(0.0, Math.min(1.0, progress));
                    double finalProgress = progress;
                    Platform.runLater(() -> {
                        progressSlider.setValue(finalProgress);
                        updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                    });
                    updateProgressSliderStyle(progress);

                    // 只在记忆播放时更新进度
                    if (isRememberLastPlay) {
                        updateLastPlayProgress();
                    }
                }
            }
        });
    }

    private void updateLastPlayProgress() {
        if (!isRememberLastPlay || mediaPlayer == null || !isMediaReady) {
            return;
        }
        if (currentPlayingIndex >= 0 && currentPlayingIndex < playlist.size()) {
            lastPlayFilePath = playlist.get(currentPlayingIndex).getAbsolutePath();
            lastPlaybackProgress = mediaPlayer.getCurrentTime().toSeconds();
        }
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
}