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
import java.util.Optional;

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

    private MediaPlayer mediaPlayer;
    private File selectedMediaFile;
    private Image bgImageObj;
    private boolean isPlaying = false;
    private boolean isDraggingProgress = false;
    private boolean isMediaEnded = false;

    // 内置矢量图标
    private final Polygon playIcon;
    private final HBox pauseIcon;
    private boolean isSwitchingMedia = false;

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

        // 初始化过滤列表
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
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

        // 初始化播放列表
        initPlaylist();
        initPlaylistToggle();

        // 绑定背景图片宽度与高度
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
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(Math.max(0.0, Math.min(1.0, newVal.doubleValue())));
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
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
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

        // 点击列表项播放
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                File selectedFile = playlistView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    int originalIndex = playlist.indexOf(selectedFile);
                    // 如果点击的是当前正在播放的文件，执行播放/暂停切换
                    if (originalIndex == currentPlayingIndex && mediaPlayer != null) {
                        MediaPlayer.Status status = mediaPlayer.getStatus();
                        if (status == MediaPlayer.Status.PLAYING) {
                            togglePlayPause();
                        } else if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY) {
                            togglePlayPause();
                        }
                    } else {
                        // 否则播放选中的新文件
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

        if (isSwitchingMedia) {
            return;
        }

        isSwitchingMedia = true;

        try {
            File file = playlist.get(index);
            currentPlayingIndex = index;

            // 释放旧的MediaPlayer
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                } catch (Exception e) {
                    // 忽略释放过程中的异常
                }
                mediaPlayer = null;
            }

            // 更新UI状态
            Platform.runLater(() -> {
                fileNameLabel.setText(file.getName());
                isPlaying = false;
                playPauseBtn.setGraphic(playIcon);
                progressSlider.setValue(0.0);
                updateProgressSliderStyle(0.0);
                currentTimeLabel.setText("00:00");
                totalTimeLabel.setText("00:00");

                bgImage.setVisible(true);
                blackMask.setVisible(true);
                bgImage.toFront();
                centerPlayIcon.setVisible(false);
                isMediaEnded = false;

                playlistView.refresh();
                setPlaybackButtonsDisabled(false);
            });

            // 检查文件
            if (!file.exists() || !file.canRead()) {
                throw new RuntimeException("文件不可访问或不存在: " + file.getPath());
            }
            if (file.length() == 0) {
                throw new RuntimeException("文件为空: " + file.getName());
            }

            // 创建新的Media对象
            String mediaUrl = file.toURI().toString();
            System.out.println("加载媒体: " + mediaUrl);

            Media media = new Media(mediaUrl);
            media.setOnError(() -> {
                Platform.runLater(() -> {
                    String errorMsg = "未知错误";
                    if (media.getError() != null) {
                        errorMsg = media.getError().getMessage();
                    }
                    handleMediaError(file, "媒体格式错误: " + errorMsg);
                });
            });

            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());

            // 监听媒体准备就绪
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    try {
                        Duration totalDuration = mediaPlayer.getTotalDuration();
                        if (totalDuration == null || totalDuration.isUnknown() || totalDuration.toSeconds() <= 0) {
                            handleMediaError(file, "无法获取有效媒体时长");
                            return;
                        }

                        bindProgressUpdate();
                        updateTimeDisplay(Duration.ZERO, totalDuration);
                        progressSlider.setValue(0.0);
                        updateProgressSliderStyle(0.0);

                        mediaPlayer.play();
                        isPlaying = true;
                        playPauseBtn.setGraphic(pauseIcon);

                        bgImage.setVisible(false);
                        blackMask.setVisible(false);

                        updateCenterPlayIconVisibility();
                        playlistView.refresh();
                        setPlaybackButtonsDisabled(false);

                        isSwitchingMedia = false;
                        System.out.println("开始播放: " + file.getName());
                    } catch (Exception e) {
                        System.err.println("播放准备错误: " + e.getMessage());
                        handleMediaError(file, "播放准备失败");
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
                    String errorMsg = "未知错误";
                    if (mediaPlayer.getError() != null) {
                        errorMsg = mediaPlayer.getError().getMessage();
                    }
                    System.err.println("播放器错误: " + errorMsg);
                    handleMediaError(file, "播放器错误: " + errorMsg);
                    isSwitchingMedia = false;
                });
            });

            // 媒体播放状态监听 - 兼容JavaFX 11+
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                Platform.runLater(() -> {
                    System.out.println("状态变化: " + oldStatus + " -> " + newStatus);
                    // JavaFX 11+ 中只需要检查 STOPPED 和 HALTED 状态
                    if (newStatus == MediaPlayer.Status.STOPPED || newStatus == MediaPlayer.Status.HALTED) {
                        isSwitchingMedia = false;
                    }

                    if (newStatus == MediaPlayer.Status.HALTED) {
                        handleMediaError(file, "媒体播放被终止");
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("文件加载失败: " + e.getMessage());
            Platform.runLater(() -> {
                handleMediaError(playlist.get(index), "加载失败: " + e.getMessage());
            });
            isSwitchingMedia = false;
        }
    }

    // 处理媒体错误
    private void handleMediaError(File file, String errorMessage) {
        isPlaying = false;
        playPauseBtn.setGraphic(playIcon);

        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();

        playlistView.refresh();
        updateCenterPlayIconVisibility();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("播放错误");
        alert.setHeaderText("无法播放文件");
        alert.setContentText(errorMessage + "\n文件: " + file.getName());
        alert.showAndWait();

        // 如果开启自动播放，尝试播放下一首
        if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
            Timeline delay = new Timeline(
                    new KeyFrame(Duration.millis(1000), e -> {
                        playFromPlaylist(currentPlayingIndex + 1);
                    })
            );
            delay.play();
        }
    }

    // 处理媒体播放结束
    private void handleMediaEnd() {
        if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
            playFromPlaylist(currentPlayingIndex + 1);
        } else {
            isPlaying = false;
            isMediaEnded = true;
            playPauseBtn.setGraphic(playIcon);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);

            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();

            if (mediaPlayer != null) {
                updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
            } else {
                currentTimeLabel.setText("00:00");
                totalTimeLabel.setText("00:00");
            }

            updateCenterPlayIconVisibility();
            playlistView.refresh();
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

        playlist.remove(selectedFile);

        if (isCurrentPlaying) {
            stopMedia();
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
            if (originalIndex < currentPlayingIndex) {
                currentPlayingIndex--;
            }
        }

        playlistView.refresh();
        updatePlaylistCount();
    }

    // 清空播放列表
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

        setPlaybackButtonsDisabled(true);
        playlistView.refresh();
        updatePlaylistCount();
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
            if (mediaPlayer != null) {
                if (e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                    togglePlayPause();
                }
            }
        });
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    // 初始化居中播放图标
    private void initCenterPlayIcon() {
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && !isPlaying) {
                togglePlayPause();
            }
        });

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
                double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                if (totalSeconds > 0) {
                    double seekTime = progressSlider.getValue() * totalSeconds;
                    double progress = progressSlider.getValue();
                    mediaPlayer.seek(Duration.seconds(seekTime));
                    updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(progress);
                }
            }
        });

        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                if (totalSeconds > 0) {
                    double seekTime = progressSlider.getValue() * totalSeconds;
                    double progress = progressSlider.getValue();
                    mediaPlayer.seek(Duration.seconds(seekTime));
                    updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(progress);
                }
            }
        });
        updateProgressSliderStyle(0.0);
    }

    // 格式化时长
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        if (totalSeconds < 0) totalSeconds = 0;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 更新进度条渐变样式 - 修复NaN问题
    private void updateProgressSliderStyle(double progress) {
        Platform.runLater(() -> {
            Node track = progressSlider.lookup(".track");
            if (track == null) return;

            double progressPercent;
            if (Double.isNaN(progress) || Double.isInfinite(progress)) {
                progressPercent = 0.0;
            } else {
                progressPercent = Math.max(0, Math.min(100, progress * 100));
            }

            // 使用更安全的方式格式化
            String progressPercentStr;
            try {
                progressPercentStr = String.format("%.2f", progressPercent);
                // 检查格式化结果
                if (progressPercentStr.contains("NaN") || progressPercentStr.contains("Infinity")) {
                    progressPercentStr = "0.00";
                }
            } catch (Exception e) {
                progressPercentStr = "0.00";
            }

            // 更简单的CSS，避免复杂的字符串格式化
            String gradientStyle = "-fx-background-color: linear-gradient(to right, " +
                    "#1E90FF 0%, " +
                    "#1E90FF " + progressPercentStr + "%, " +
                    "#444444 " + progressPercentStr + "%, " +
                    "#444444 100%);";

            track.setStyle(gradientStyle);
        });
    }

    // 更新时长显示文本
    private void updateTimeDisplay(Duration current, Duration total) {
        Platform.runLater(() -> {
            currentTimeLabel.setText(formatDuration(current));
            totalTimeLabel.setText(formatDuration(total));
        });
    }

    // 控制居中播放图标的显示/隐藏
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

    // 背景图按原比例缩放，居中显示
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

    private void openMediaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择媒体文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("媒体文件",
                        "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv",
                        "*.mp3", "*.wav", "*.aac", "*.m4a")
        );

        selectedMediaFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedMediaFile == null) {
            return;
        }

        if (!selectedMediaFile.exists() || !selectedMediaFile.canRead()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("文件错误");
            alert.setHeaderText("无法访问文件");
            alert.setContentText("文件不存在或没有读取权限: " + selectedMediaFile.getName());
            alert.showAndWait();
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

    // 绑定进度条实时更新
    private void bindProgressUpdate() {
        if (mediaPlayer != null) {
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!isDraggingProgress && mediaPlayer.getTotalDuration() != null) {
                    double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                    if (totalSeconds > 0) {
                        double progress = newTime.toSeconds() / totalSeconds;
                        if (Double.isNaN(progress) || Double.isInfinite(progress)) {
                            progress = 0.0;
                        }

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
    }

    // 切换播放/暂停
    private void togglePlayPause() {
        if (mediaPlayer == null) {
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

    private void stopMedia() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                // 忽略异常
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        isMediaEnded = true;
        playPauseBtn.setGraphic(playIcon);
        isSwitchingMedia = false;
    }

    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.dispose();
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }
}