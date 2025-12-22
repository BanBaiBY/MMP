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
    // 布局控件
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;

    // 媒体控件
    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask;

    // 基础功能控件（保留倍速、快进、快退）
    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Button rewindBtn;   // << 后退30秒
    @FXML private Button forwardBtn;  // >> 快进30秒
    @FXML private Button speedBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;
    @FXML private Text totalTimeLabel;

    // 播放列表控件（适配新FXML结构）
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
    private int currentPlayingIndex = -1; // 当前播放的列表项索引
    private boolean isAutoPlayNext = true; // 是否自动播放下一曲

    // 媒体核心变量
    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false;

    // 倍速相关（完全保留原有功能）
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0;
    private ContextMenu speedMenu;

    // 快进/后退时间（秒）
    private static final int SEEK_STEP = 30;

    // 新增：防止快速切换和媒体就绪标记
    private boolean isSwitchingMedia = false;
    private boolean isMediaReady = false;

    // 内置矢量图标（保留所有原有图标）
    private final Polygon playIcon;
    private final HBox pauseIcon;
    private final HBox rewindIcon;    // << 后退图标
    private final HBox forwardIcon;   // >> 快进图标

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

        // 初始化倍速菜单
        initSpeedMenu();

        // 初始化播放列表过滤列表
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
    }

    @FXML
    public void initialize() {
        initCSS();
        fileNameLabel.setText("未选择文件");

        // 设置按钮图标（保留所有按钮图标）
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);

        // 基础功能初始化
        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton(); // 倍速按钮初始化

        // 播放列表功能初始化（升级后的逻辑）
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
        });

        // 按钮事件绑定（保留快进/快退/倍速事件）
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && isMediaReady) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        // 搜索框监听（升级后的逻辑）
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
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // 后退30秒逻辑（保留，增加isMediaReady判断）
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

    // 快进30秒逻辑（保留，增加isMediaReady判断）
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

    // 倍速菜单初始化（完全保留原有功能）
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

    // 初始化倍速按钮（完全保留原有功能）
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

    // 更新倍速按钮文本（完全保留原有功能）
    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

    // 播放列表折叠/展开初始化方法（升级后的逻辑）
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

    // 初始化播放列表（核心升级逻辑）
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

        // 升级：点击当前播放项切换播放/暂停，点击其他项播放新文件
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
        });

        updatePlaylistCount();
    }

    // 更新播放列表计数（保留）
    private void updatePlaylistCount() {
        int totalCount = playlist.size();
        int filteredCount = filteredPlaylist.size();

        if (searchField.getText().isEmpty()) {
            playlistCountLabel.setText(totalCount + " 首");
        } else {
            playlistCountLabel.setText(filteredCount + "/" + totalCount + " 首");
        }
    }

    // 从播放列表播放指定索引的文件（核心升级逻辑，增加防快速切换和错误处理）
    private void playFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        // 新增：防止快速切换冲突
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
            updateSpeedButtonText();

            // 监听媒体准备就绪
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    try {
                        isMediaReady = true;

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

                        isSwitchingMedia = false;
                    } catch (Exception e) {
                        System.err.println("媒体准备就绪时发生错误: " + e.getMessage());
                        handleMediaError(file);
                        isSwitchingMedia = false;
                    }
                });
            });

            // 监听播放结束
            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    handleMediaEnd();
                    isSwitchingMedia = false;
                });
            });

            // 监听暂停和播放
            mediaPlayer.setOnPaused(() -> {
                Platform.runLater(() -> {
                    updateCenterPlayIconVisibility();
                });
            });

            mediaPlayer.setOnPlaying(() -> {
                Platform.runLater(() -> {
                    updateCenterPlayIconVisibility();
                });
            });

            // 监听错误
            mediaPlayer.setOnError(() -> {
                Platform.runLater(() -> {
                    handleMediaError(file);
                    isSwitchingMedia = false;
                });
            });

            // 添加媒体播放状态监听
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                Platform.runLater(() -> {
                    if (newStatus == MediaPlayer.Status.STOPPED ||
                            newStatus == MediaPlayer.Status.HALTED) {
                        isSwitchingMedia = false;
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            handleMediaError(playlist.get(index));
            isSwitchingMedia = false;
        }
    }

    // 新增：媒体错误处理方法
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

    // 处理媒体播放结束（升级后的逻辑）
    private void handleMediaEnd() {
        if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
            // 自动播放下一曲
            playFromPlaylist(currentPlayingIndex + 1);
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
        }
    }

    // 从播放列表移除选中项（升级后的逻辑）
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
    }

    // 清空播放列表（升级后的逻辑，彻底释放资源）
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
    }

    // 打开媒体文件（升级后的逻辑，扩展文件格式支持）
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

        // 添加到播放列表（去重）
        if (!playlist.contains(selectedMediaFile)) {
            playlist.add(selectedMediaFile);
            currentPlayingIndex = playlist.size() - 1;
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        } else {
            currentPlayingIndex = playlist.indexOf(selectedMediaFile);
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        }

        // 播放选中的文件
        playFromPlaylist(currentPlayingIndex);
    }

    // 更新按钮禁用状态（保留，包含倍速/快进/快退按钮）
    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        speedBtn.setDisable(disabled);
        rewindBtn.setDisable(disabled);
        forwardBtn.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && isMediaReady && !isPlaying);
    }

    // 切换播放/暂停（保留，增加isMediaReady判断）
    private void togglePlayPause() {
        if (mediaPlayer == null || !isMediaReady) {
            // 如果有播放列表项，播放第一个
            if (!playlist.isEmpty() && currentPlayingIndex == -1) {
                playFromPlaylist(0);
            }
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

    // ---- 以下为通用方法，保留并少量优化 ----
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
                // 新增：参数有效性校验
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
        // 新增：参数有效性校验
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
                // 新增：参数有效性校验
                if (!Double.isNaN(progress) && !Double.isInfinite(progress)) {
                    progress = Math.max(0.0, Math.min(1.0, progress));
                    double finalProgress = progress;
                    Platform.runLater(() -> {
                        progressSlider.setValue(finalProgress);
                        updateTimeDisplay(newTime, mediaPlayer.getTotalDuration());
                    });
                    updateProgressSliderStyle(progress);
                }
            }
        });
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

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }
}
