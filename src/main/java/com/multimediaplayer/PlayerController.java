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
import java.util.List;

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
    private boolean isSwitchingMedia = false; // 防止快速切换

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

        // 初始化播放列表折叠/展开功能
        initPlaylistToggle();

        // 绑定背景图片宽度与高度
        Platform.runLater(() -> {
            initBgImage(); // 延迟加载图片
            bindMediaViewSize(); // 延迟绑定视频尺寸
            // 初始化：未选择媒体文件时显示背景图+蒙版（黑色蒙版不再默认隐藏）
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront(); // 强制置顶
        });

        // 按钮事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());

        // 播放列表按钮事件（移除addToPlaylistBtn相关代码）
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

    // 播放列表折叠/展开初始化方法
    private void initPlaylistToggle() {
        // 初始状态：透明度0，visible由FXML绑定控制（初始false）
        playlistContainer.setOpacity(0.0);

        // 监听按钮选中状态，控制动画和可见性
        playlistToggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // 展开：先设置为可见，再执行淡入动画
                playlistContainer.setVisible(true);
                Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(playlistContainer.opacityProperty(), 1.0))
                );
                fadeIn.play();
            } else {
                // 收起：淡出动画结束后设置为不可见
                Timeline fadeOut = new Timeline(
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(playlistContainer.opacityProperty(), 0.0))
                );
                fadeOut.setOnFinished(e -> playlistContainer.setVisible(false));
                fadeOut.play();
            }
        });

        // 初始状态：未选中，列表不可见
        playlistToggleBtn.setSelected(false);
        playlistContainer.setVisible(false);
    }

    // 初始化播放列表
    private void initPlaylist() {
        // 绑定过滤列表到ListView
        playlistView.setItems(filteredPlaylist);

        // 自定义列表项显示
        playlistView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(file.getName());
                    // 标记当前播放的项
                    if (playlist.indexOf(file) == currentPlayingIndex) {
                        setStyle("-fx-text-fill: #1E90FF; -fx-font-weight: bold; -fx-background-color: rgba(30, 144, 255, 0.1);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // 点击列表项播放 - 修改为单击即可播放
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                File selectedFile = playlistView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    int originalIndex = playlist.indexOf(selectedFile);
                    // 如果点击的是当前正在播放的文件，执行播放/暂停切换
                    if (originalIndex == currentPlayingIndex && mediaPlayer != null) {
                        togglePlayPause();
                    } else {
                        // 否则播放选中的新文件
                        playFromPlaylist(originalIndex);
                    }
                }
            }
        });

        // 监听播放列表变化，更新计数
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

    // 从播放列表播放指定索引的文件 - 修复后的版本
    private void playFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        // 防止快速切换导致的冲突
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
            setPlaybackButtonsDisabled(false);

            // 创建新的Media对象
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // 设置音量
            if (volumeSlider.getValue() > 0) {
                mediaPlayer.setVolume(volumeSlider.getValue());
            }

            // 监听媒体准备就绪
            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    try {
                        // 绑定进度更新
                        bindProgressUpdate();

                        // 更新总时长显示
                        Duration totalDuration = mediaPlayer.getTotalDuration();
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
                        setPlaybackButtonsDisabled(false);

                        isSwitchingMedia = false;
                    } catch (Exception e) {
                        System.err.println("媒体准备就绪时发生错误: " + e.getMessage());
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
                    System.err.println("媒体播放错误：" + (mediaPlayer.getError() != null ?
                            mediaPlayer.getError().getMessage() : "未知错误"));

                    // 重置状态
                    isPlaying = false;
                    playPauseBtn.setGraphic(playIcon);

                    // 显示背景图和蒙版
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront();

                    // 更新UI
                    playlistView.refresh();
                    updateCenterPlayIconVisibility();

                    isSwitchingMedia = false;

                    // 显示错误提示
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("播放错误");
                    alert.setHeaderText("无法播放文件");
                    alert.setContentText("文件格式可能不受支持或已损坏: " + file.getName());
                    alert.show();
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
            e.printStackTrace();

            // 重置状态
            isPlaying = false;
            playPauseBtn.setGraphic(playIcon);

            // 显示背景图和蒙版
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();

            // 更新UI
            playlistView.refresh();
            updateCenterPlayIconVisibility();

            isSwitchingMedia = false;

            // 显示错误提示
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("加载错误");
            alert.setHeaderText("无法加载文件");
            alert.setContentText("文件可能已损坏或格式不支持: " +
                    (playlist.get(index) != null ? playlist.get(index).getName() : "未知文件"));
            alert.show();
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
            // 播放结束时显示背景图和蒙版
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

        // 先移除文件
        playlist.remove(selectedFile);

        if (isCurrentPlaying) {
            // 当前正在播放的项被删除
            stopMedia(); // 停止并重置播放器

            // 自动播放下一首（如果开启且存在）
            if (isAutoPlayNext && !playlist.isEmpty()) {
                int nextIndex = originalIndex; // 因为已删除，原 index 就是下一个（如删第2个，原第3个变成第2个）
                if (nextIndex >= playlist.size()) {
                    nextIndex = playlist.size() - 1; // 防越界
                }
                if (nextIndex >= 0) {
                    playFromPlaylist(nextIndex);
                    return; // 已处理播放，直接返回
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

        // 给居中图标设置手型光标
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

        // 添加到播放列表（去重）
        if (!playlist.contains(selectedMediaFile)) {
            playlist.add(selectedMediaFile);
            currentPlayingIndex = playlist.size() - 1;
            // 清除搜索过滤，确保能选中
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        } else {
            currentPlayingIndex = playlist.indexOf(selectedMediaFile);
            // 清除搜索过滤，确保能选中
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        }

        // 播放选中的文件
        playFromPlaylist(currentPlayingIndex);
    }

    // 绑定进度条实时更新
    private void bindProgressUpdate() {
        // 监听播放进度，更新进度条和时长
        if (mediaPlayer != null) {
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
    }

    // 切换播放/暂停
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
            // 暂停时：保持背景图和蒙版隐藏
            bgImage.setVisible(false);
            blackMask.setVisible(false);
        } else {
            // 播放时：保持背景图和蒙版隐藏
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
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose(); // 释放底层资源
            } catch (Exception e) {
                // 忽略释放过程中的异常
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        isMediaEnded = true;
        playPauseBtn.setGraphic(playIcon);
        isSwitchingMedia = false;
        // 注意：背景图是否显示由调用方决定，这里不强制设 visible
    }

    private void setPlaybackButtonsDisabled(boolean disabled) {
        playPauseBtn.setDisable(disabled);
        progressSlider.setDisable(disabled);
        centerPlayIcon.setVisible(!disabled && mediaPlayer != null && !isPlaying);
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.dispose(); // 释放MediaPlayer资源
            } catch (Exception e) {
                // 忽略清理过程中的异常
            }
        }
    }
}