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

    // 基础功能控件
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

    // 倍速相关
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0;
    private ContextMenu speedMenu;

    // 快进/后退时间（秒）
    private static final int SEEK_STEP = 30;

    // 内置矢量图标
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

        // 设置按钮图标
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);

        // 基础功能初始化
        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton(); // 倍速按钮初始化

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
        });

        // 按钮事件绑定（整合所有功能）
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());

        // 音量绑定
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
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
        });

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // 后退30秒逻辑
    private void seekBackward() {
        if (mediaPlayer == null || mediaPlayer.getTotalDuration() == null) {
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
        if (mediaPlayer == null || mediaPlayer.getTotalDuration() == null) {
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
                if (mediaPlayer != null) {
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
                File selectedFile = playlistView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    int originalIndex = playlist.indexOf(selectedFile);
                    playFromPlaylist(originalIndex);
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

        File file = playlist.get(index);
        currentPlayingIndex = index;

        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.setRate(currentSpeed);
            updateSpeedButtonText();

            fileNameLabel.setText(file.getName());
            bgImage.setVisible(false);
            blackMask.setVisible(false);
            isMediaEnded = false;

            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    bindProgressUpdate();
                    updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
                    updateProgressSliderStyle(0.0);
                    mediaPlayer.play();
                    isPlaying = true;
                    playPauseBtn.setGraphic(pauseIcon);
                    updateCenterPlayIconVisibility();
                    playlistView.refresh();
                    setPlaybackButtonsDisabled(false);
                });
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    handleMediaEnd();
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

            mediaPlayer.setOnPlaying(() -> {
                Platform.runLater(() -> {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    updateCenterPlayIconVisibility();
                });
            });

            mediaPlayer.setOnError(() -> {
                Platform.runLater(() -> {
                    System.err.println("媒体播放错误：" + mediaPlayer.getError().getMessage());
                    setPlaybackButtonsDisabled(true);
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront();
                });
            });

        } catch (Exception e) {
            System.err.println("文件加载失败：" + e.getMessage());
            e.printStackTrace();
            setPlaybackButtonsDisabled(true);
            mediaPlayer = null;
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        }
    }

    // 处理媒体播放结束
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
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
            updateTimeDisplay(Duration.ZERO, mediaPlayer.getTotalDuration());
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
        playlist.remove(selectedFile);

        if (originalIndex == currentPlayingIndex) {
            currentPlayingIndex = -1;
            stopMedia();
            fileNameLabel.setText("未选择文件");
            bgImage.setVisible(true);
            blackMask.setVisible(true);
        } else if (originalIndex < currentPlayingIndex) {
            currentPlayingIndex--;
        }

        playlistView.refresh();
        updatePlaylistCount();
    }

    // 清空播放列表
    private void clearPlaylist() {
        playlist.clear();
        currentPlayingIndex = -1;
        stopMedia();
        fileNameLabel.setText("未选择文件");
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        playlistView.refresh();
        updatePlaylistCount();
    }

    // 打开媒体文件（整合播放列表逻辑）
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

    // 更新按钮禁用状态（整合所有功能按钮）
    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        speedBtn.setDisable(disabled);
        rewindBtn.setDisable(disabled);
        forwardBtn.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    // 切换播放/暂停（整合播放列表空判断）
    private void togglePlayPause() {
        if (mediaPlayer == null) {
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

    // ---- 以下为通用方法，无需修改 ----
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
