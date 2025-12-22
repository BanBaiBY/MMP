package com.multimediaplayer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.layout.VBox;
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
    // ===================== 整合所有控件变量 =====================
    // 布局控件
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;
    @FXML private ComboBox<String> themeComboBox; // 新增：主题下拉框

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
    @FXML private Button prevMediaBtn; // 上一首
    @FXML private Button nextMediaBtn; // 下一首
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

    // ===================== 整合所有数据模型与状态 =====================
    // 播放列表数据模型
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private final FilteredList<File> filteredPlaylist;
    private int currentPlayingIndex = -1; // 当前播放索引
    private boolean isAutoPlayNext = true; // 自动播放下一首

    // 媒体核心变量
    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false; // 播放结束标记（来自B版）
    private boolean isSwitchingMedia = false; // 防止快速切换（来自A版）
    private boolean isMediaReady = false; // 媒体就绪标记（来自A版）

    // 倍速相关
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0;
    private ContextMenu speedMenu;

    // 快进/后退时间（秒）
    private static final int SEEK_STEP = 30;

    // ===================== 整合所有矢量图标 =====================
    private final Polygon playIcon;
    private final HBox pauseIcon;
    private final HBox rewindIcon;    // << 后退图标
    private final HBox forwardIcon;   // >> 快进图标
    private final HBox prevMediaIcon; // 上一首图标
    private final HBox nextMediaIcon; // 下一首图标

    // 主题管理器（来自B版，需项目中存在ThemeManager类）
    private final ThemeManager themeManager = ThemeManager.getInstance();

    // ===================== 构造方法：初始化所有图标 =====================
    public PlayerController() {
        // 播放三角形
        playIcon = new Polygon(6.0, 2.0, 6.0, 22.0, 22.0, 12.0);
        playIcon.setFill(Color.WHITE);
        playIcon.setSmooth(true);

        // 暂停双矩形
        Rectangle rect1 = new Rectangle(0, 0, 7, 20);
        Rectangle rect2 = new Rectangle(10, 0, 7, 20);
        rect1.setFill(Color.WHITE);
        rect2.setFill(Color.WHITE);
        pauseIcon = new HBox(3, rect1, rect2);
        pauseIcon.setAlignment(Pos.CENTER);
        pauseIcon.setPrefSize(24, 24);

        // 后退图标（<<）- 双左箭头
        Polygon tri1Left = new Polygon(20.0, 4.0, 20.0, 20.0, 8.0, 12.0);
        Polygon tri2Left = new Polygon(12.0, 4.0, 12.0, 20.0, 0.0, 12.0);
        tri1Left.setFill(Color.WHITE);
        tri2Left.setFill(Color.WHITE);
        rewindIcon = new HBox(1, tri2Left, tri1Left);
        rewindIcon.setAlignment(Pos.CENTER);
        rewindIcon.setPrefSize(24, 24);

        // 快进图标（>>）- 双右箭头
        Polygon tri1Right = new Polygon(4.0, 4.0, 4.0, 20.0, 16.0, 12.0);
        Polygon tri2Right = new Polygon(12.0, 4.0, 12.0, 20.0, 24.0, 12.0);
        tri1Right.setFill(Color.WHITE);
        tri2Right.setFill(Color.WHITE);
        forwardIcon = new HBox(1, tri1Right, tri2Right);
        forwardIcon.setAlignment(Pos.CENTER);
        forwardIcon.setPrefSize(24, 24);

        // 上一首图标 - 单左箭头
        Polygon tri3Left = new Polygon(12.0, 5.0, 12.0, 25.0, 4.0, 15.0);
        tri3Left.setFill(Color.WHITE);
        prevMediaIcon = new HBox(tri3Left);
        prevMediaIcon.setAlignment(Pos.CENTER);
        prevMediaIcon.setPrefSize(24, 24);

        // 下一首图标 - 单右箭头
        Polygon tri3Right = new Polygon(8.0, 5.0, 8.0, 25.0, 16.0, 15.0);
        tri3Right.setFill(Color.WHITE);
        nextMediaIcon = new HBox(tri3Right);
        nextMediaIcon.setAlignment(Pos.CENTER);
        nextMediaIcon.setPrefSize(24, 24);

        // 初始化倍速菜单
        initSpeedMenu();
        // 初始化播放列表过滤
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
    }

    // ===================== 初始化方法：整合所有功能 =====================
    @FXML
    public void initialize() {
        // 1. 初始化CSS和主题（优先执行B版逻辑）
        initCSS();
        initThemeComboBox();
        fileNameLabel.setText("未选择文件");

        // 2. 设置所有按钮图标
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);
        prevMediaBtn.setGraphic(prevMediaIcon);
        nextMediaBtn.setGraphic(nextMediaIcon);

        // 3. 初始化基础功能（合并A+B版）
        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton();
        initPrevNextButtons();

        // 4. 初始化播放列表（A版逻辑）
        initPlaylist();
        initPlaylistToggle();

        // 5. 初始化时长标签
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");

        // 6. 延迟初始化背景图和媒体视图绑定（B版优化逻辑）
        Platform.runLater(() -> {
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        });

        // 7. 绑定所有按钮事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());

        // 8. 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && isMediaReady) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        // 9. 搜索框监听
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredPlaylist.setPredicate(file -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                return file.getName().toLowerCase().contains(newVal.toLowerCase());
            });
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // ===================== 主题相关方法（来自B版，完整保留） =====================
    private void initThemeComboBox() {
        themeComboBox.getItems().addAll(themeManager.getThemeDisplayNames());
        themeComboBox.setValue(themeManager.getCurrentTheme().getDisplayName());
        themeComboBox.setOnAction(e -> {
            String selectedName = themeComboBox.getValue();
            ThemeManager.Theme selectedTheme = themeManager.getThemeByDisplayName(selectedName);
            themeManager.switchTheme(selectedTheme, rootPane.getScene());
            updateProgressSliderStyle(progressSlider.getValue());
        });
    }

    private void initCSS() {
        URL baseCssUrl = getClass().getClassLoader().getResource("css/player.css");
        if (baseCssUrl != null) {
            rootPane.getStylesheets().add(baseCssUrl.toExternalForm());
        } else {
            System.err.println("CSS文件 /css/player.css 未找到！");
        }
        themeManager.switchTheme(ThemeManager.Theme.LIGHT, rootPane.getScene());
    }

    // ===================== 倍速相关方法（来自A版，完整保留） =====================
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

    private void initSpeedButton() {
        updateSpeedButtonText();
        speedBtn.setStyle("-fx-background-color: #363636; -fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 0 10px; -fx-border-width: 0; -fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        speedBtn.setOnAction(e -> {
            if (!speedBtn.isDisabled()) {
                speedMenu.show(speedBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
        speedBtn.setDisable(true);
    }

    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

    // ===================== 上下首/快进后退相关方法（来自A版，完整保留） =====================
    private void initPrevNextButtons() {
        prevMediaBtn.setStyle("-fx-background-color: #363636; -fx-border-width: 0; -fx-cursor: hand; -fx-padding: 0; -fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        prevMediaBtn.setPrefSize(40.0, 30.0);
        nextMediaBtn.setStyle(prevMediaBtn.getStyle());
        nextMediaBtn.setPrefSize(40.0, 30.0);
        prevMediaBtn.setOnAction(e -> playPreviousMedia());
        nextMediaBtn.setOnAction(e -> playNextMedia());
        updatePrevNextBtnStatus();
    }

    private void playPreviousMedia() {
        if (isSwitchingMedia || playlist.isEmpty() || currentPlayingIndex <= 0) return;
        playFromPlaylist(currentPlayingIndex - 1);
    }

    private void playNextMedia() {
        if (isSwitchingMedia || playlist.isEmpty() || currentPlayingIndex >= playlist.size() - 1) return;
        playFromPlaylist(currentPlayingIndex + 1);
    }

    private void updatePrevNextBtnStatus() {
        if (playlist.isEmpty() || currentPlayingIndex == -1) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        prevMediaBtn.setDisable(currentPlayingIndex <= 0);
        nextMediaBtn.setDisable(currentPlayingIndex >= playlist.size() - 1);
    }

    private void seekBackward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) return;
        double newTime = Math.max(0, mediaPlayer.getCurrentTime().toSeconds() - SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        updateProgressAndTime(newTime);
    }

    private void seekForward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) return;
        double total = mediaPlayer.getTotalDuration().toSeconds();
        double newTime = Math.min(total, mediaPlayer.getCurrentTime().toSeconds() + SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        updateProgressAndTime(newTime);
    }

    private void updateProgressAndTime(double newTime) {
        double total = mediaPlayer.getTotalDuration().toSeconds();
        double progress = newTime / total;
        progressSlider.setValue(progress);
        updateProgressSliderStyle(progress);
        updateTimeDisplay(Duration.seconds(newTime), mediaPlayer.getTotalDuration());
    }

    // ===================== 播放列表相关方法（来自A版，完整保留） =====================
    private void initPlaylistToggle() {
        playlistContainer.setOpacity(0.0);
        playlistToggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                playlistContainer.setVisible(true);
                new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(playlistContainer.opacityProperty(), 1.0))).play();
            } else {
                Timeline fadeOut = new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(playlistContainer.opacityProperty(), 0.0)));
                fadeOut.setOnFinished(e -> playlistContainer.setVisible(false));
                fadeOut.play();
            }
        });
        playlistToggleBtn.setSelected(false);
        playlistContainer.setVisible(false);
    }

    private void initPlaylist() {
        playlistView.setItems(filteredPlaylist);
        playlistView.setCellFactory(param -> new ListCell<File>() {
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

        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                File selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                int idx = playlist.indexOf(selected);
                if (idx == currentPlayingIndex && mediaPlayer != null && isMediaReady) {
                    togglePlayPause();
                } else {
                    playFromPlaylist(idx);
                }
            }
        });

        playlist.addListener((javafx.collections.ListChangeListener<File>) change -> {
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });
        updatePlaylistCount();
    }

    private void updatePlaylistCount() {
        int total = playlist.size();
        int filtered = filteredPlaylist.size();
        playlistCountLabel.setText(searchField.getText().isEmpty() ? total + " 首" : filtered + "/" + total + " 首");
    }

    private void removeSelectedFromPlaylist() {
        File selected = playlistView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int idx = playlist.indexOf(selected);
        boolean isCurrent = idx == currentPlayingIndex;
        playlist.remove(selected);

        if (isCurrent) {
            stopMedia();
            if (isAutoPlayNext && !playlist.isEmpty()) {
                playFromPlaylist(Math.min(idx, playlist.size() - 1));
            } else {
                currentPlayingIndex = -1;
                fileNameLabel.setText("未选择文件");
                bgImage.setVisible(true);
                blackMask.setVisible(true);
                setPlaybackButtonsDisabled(true);
            }
        } else if (idx < currentPlayingIndex) {
            currentPlayingIndex--;
        }
        playlistView.refresh();
    }

    private void clearPlaylist() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        playlist.clear();
        currentPlayingIndex = -1;
        isPlaying = false;
        isMediaEnded = true;
        isSwitchingMedia = false;
        isMediaReady = false;
        fileNameLabel.setText("未选择文件");
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        playPauseBtn.setGraphic(playIcon);
        setPlaybackButtonsDisabled(true);
        playlistView.refresh();
    }

    // ===================== 媒体播放核心方法（整合A+B版逻辑） =====================
    private void openMediaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择媒体文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("媒体文件", "*.mp4", "*.avi", "*.mkv", "*.mp3", "*.wav", "*.flac", "*.aac"),
                new FileChooser.ExtensionFilter("视频文件", "*.mp4", "*.avi", "*.mkv"),
                new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav", "*.flac", "*.aac")
        );
        selectedMediaFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedMediaFile == null) return;

        // 核心修改：选择文件后加入播放列表
        currentSpeed = 1.0;
        updateSpeedButtonText();
        if (!playlist.contains(selectedMediaFile)) {
            playlist.add(selectedMediaFile);
            currentPlayingIndex = playlist.size() - 1;
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        } else {
            currentPlayingIndex = playlist.indexOf(selectedMediaFile);
            playlistView.getSelectionModel().select(currentPlayingIndex);
        }
        playFromPlaylist(currentPlayingIndex);
    }

    private void playFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size() || isSwitchingMedia) return;
        isSwitchingMedia = true;
        isMediaReady = false;

        File file = playlist.get(index);
        currentPlayingIndex = index;

        // 释放旧媒体资源
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        // 重置UI状态
        fileNameLabel.setText(file.getName());
        isPlaying = false;
        isMediaEnded = false;
        playPauseBtn.setGraphic(playIcon);
        progressSlider.setValue(0.0);
        updateProgressSliderStyle(0.0);
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();
        playlistView.refresh();
        setPlaybackButtonsDisabled(true);

        // 创建新媒体播放器
        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.setRate(currentSpeed);

            // 媒体就绪监听（整合A+B版逻辑）
            mediaPlayer.setOnReady(() -> Platform.runLater(() -> {
                try {
                    isMediaReady = true;
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    if (totalDuration == null || totalDuration.isUnknown()) {
                        throw new RuntimeException("无法获取媒体时长");
                    }
                    bindProgressUpdate();
                    updateTimeDisplay(Duration.ZERO, totalDuration);
                    mediaPlayer.play();
                    isPlaying = true;
                    playPauseBtn.setGraphic(pauseIcon);
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    setPlaybackButtonsDisabled(false);
                    updatePrevNextBtnStatus();
                } catch (Exception e) {
                    handleMediaError(file);
                } finally {
                    isSwitchingMedia = false;
                }
            }));

            // 播放结束监听（核心整合：自动下一首+重置背景图）
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
                    playFromPlaylist(currentPlayingIndex + 1);
                } else {
                    // B版逻辑：重置到开头+显示背景图
                    mediaPlayer.seek(Duration.ZERO);
                    isPlaying = false;
                    isMediaEnded = true;
                    playPauseBtn.setGraphic(playIcon);
                    progressSlider.setValue(0.0);
                    updateProgressSliderStyle(0.0);
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                }
                isSwitchingMedia = false;
            }));

            // 暂停/播放监听（B版优化逻辑）
            mediaPlayer.setOnPaused(() -> Platform.runLater(() -> {
                updateCenterPlayIconVisibility();
                boolean showBg = isMediaEnded;
                bgImage.setVisible(showBg);
                blackMask.setVisible(showBg);
                if (showBg) bgImage.toFront();
            }));
            mediaPlayer.setOnPlaying(() -> Platform.runLater(() -> {
                updateCenterPlayIconVisibility();
                bgImage.setVisible(false);
                blackMask.setVisible(false);
            }));

            // 错误监听
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                handleMediaError(file);
                isSwitchingMedia = false;
            }));

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            handleMediaError(file);
            isSwitchingMedia = false;
        }
    }

    private void handleMediaError(File file) {
        System.err.println("媒体错误：" + (mediaPlayer != null ? mediaPlayer.getError() : "未知错误"));
        isPlaying = false;
        isMediaReady = false;
        playPauseBtn.setGraphic(playIcon);
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        if (file != null && playlist.contains(file)) {
            playlist.remove(file);
            currentPlayingIndex = -1;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("播放错误");
        alert.setHeaderText("无法播放文件");
        alert.setContentText("文件格式不受支持或已损坏：" + file.getName());
        alert.showAndWait();
        playlistView.refresh();
        setPlaybackButtonsDisabled(true);
        updatePrevNextBtnStatus();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isMediaReady) {
            if (!playlist.isEmpty() && currentPlayingIndex == -1) {
                playFromPlaylist(0);
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
            updateCenterPlayIconVisibility();
            updateProgressSliderStyle(0.0);
        }
    }

    // ===================== 进度条、时长、背景图相关方法（整合A+B版） =====================
    private void initProgressSlider() {
        progressSlider.setOnMousePressed(e -> isDraggingProgress = true);
        progressSlider.setOnMouseReleased(e -> {
            isDraggingProgress = false;
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progressSlider.getValue());
            }
        });
        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progressSlider.getValue());
            }
        });
        updateProgressSliderStyle(0.0);
    }

    // 核心整合：进度条样式跟随主题（弃用A版固定颜色）
    private void updateProgressSliderStyle(double progress) {
        if (Double.isNaN(progress) || Double.isInfinite(progress)) progress = 0.0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        final double finalProgress = progress;
        Platform.runLater(() -> {
            Node track = progressSlider.lookup(".track");
            if (track == null) return;

            String primaryColor, trackColor;
            switch (themeManager.getCurrentTheme()) {
                case DARK:
                    primaryColor = "#FF6347";
                    trackColor = "#444444";
                    break;
                case RETRO:
                    primaryColor = "#A52A2A";
                    trackColor = "#DEB887";
                    break;
                default:
                    primaryColor = "#1E90FF";
                    trackColor = "#e0e0e0";
            }
            double progressPercent = finalProgress * 100;
            String gradientStyle = String.format(
                    "-fx-background-color: linear-gradient(to right, %s 0%%, %s %.2f%%, %s %.2f%%, %s 100%%);",
                    primaryColor, primaryColor, progressPercent, trackColor, progressPercent, trackColor
            );
            track.setStyle(gradientStyle);
        });
    }

    private void bindProgressUpdate() {
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null && isMediaReady) {
                double progress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                progress = Math.max(0.0, Math.min(1.0, progress));
                Platform.runLater(() -> {
                    progressSlider.setValue(progress);
                    updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                });
                updateProgressSliderStyle(progress);
            }
        });
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void updateTimeDisplay(Duration current, Duration total) {
        Platform.runLater(() -> {
            currentTimeLabel.setText(formatDuration(current));
            totalTimeLabel.setText(formatDuration(total));
        });
    }

    private void initCenterPlayIcon() {
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && !isPlaying) togglePlayPause();
        });
        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    private void adjustCenterPlayIconSize() {
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();
        if (containerW == 0 || containerH == 0) return;
        double sizeRatio = 0.125;
        double iconWidth = Math.min(Math.max(containerW * sizeRatio, 40), 80);
        double iconHeight = iconWidth * 0.75;
        centerPlayIcon.getPoints().setAll(0.0, 0.0, 0.0, iconHeight, iconWidth, iconHeight / 2);
    }

    private void updateCenterPlayIconVisibility() {
        Platform.runLater(() -> {
            centerPlayIcon.setVisible(mediaPlayer != null && isMediaReady && !isPlaying);
            if (centerPlayIcon.isVisible()) adjustCenterPlayIconSize();
        });
    }

    private void initMediaContainerClick() {
        mediaContainer.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                togglePlayPause();
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
        bgImageObj.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 1.0) {
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
        if (bgImageObj == null || mediaContainer.getWidth() == 0 || mediaContainer.getHeight() == 0) return;
        double imgW = bgImageObj.getWidth();
        double imgH = bgImageObj.getHeight();
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();
        double scale = Math.min(containerW / imgW, containerH / imgH);
        bgImage.setFitWidth(imgW * scale);
        bgImage.setFitHeight(imgH * scale);
        bgImage.setPreserveRatio(true);
        bgImage.setLayoutX((containerW - bgImage.getFitWidth()) / 2);
        bgImage.setLayoutY((containerH - bgImage.getFitHeight()) / 2);
        bgImage.toFront();
    }

    // ===================== 按钮禁用控制（扩展A版，覆盖所有按钮） =====================
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

    // ===================== 资源释放（来自B版，完整保留） =====================
    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }
}
