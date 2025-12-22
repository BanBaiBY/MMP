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
import javafx.scene.Scene;
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
import java.util.logging.Logger;

/**
 * 多媒体播放器核心控制器
 * 负责界面交互绑定、媒体播放管理、播放列表维护、主题切换、UI样式更新等全量业务逻辑
 * 支持视频/音频播放、倍速调节、快进后退、上一首/下一首切换、播放列表搜索过滤等功能
 */
public class PlayerController {
    // 日志对象：用于记录程序运行状态和异常信息
    private static final Logger logger = Logger.getLogger(PlayerController.class.getName());

    // -------------------------- 布局控件：界面基础布局容器与显示控件 --------------------------
    @FXML private BorderPane rootPane;       // 根布局（BorderPane）
    @FXML private StackPane mediaContainer;  // 媒体播放容器
    @FXML private Label fileNameLabel;       // 当前播放文件名显示标签
    @FXML private ComboBox<String> themeComboBox; // 主题切换下拉选择框

    // -------------------------- 媒体控件：与媒体播放显示相关的控件 --------------------------
    @FXML private MediaView mediaView;       // 媒体视图（显示视频画面）
    @FXML private ImageView bgImage;         // 无媒体时的背景图片
    @FXML private Polygon centerPlayIcon;    // 媒体区域中央的大型播放按钮
    @FXML private Rectangle blackMask;       // 视觉优化遮罩（增强背景图对比度）

    // -------------------------- 基础功能控件：播放控制相关按钮与滑块 --------------------------
    @FXML private Button openBtn;            // 打开媒体文件按钮
    @FXML private Button playPauseBtn;       // 播放/暂停切换按钮
    @FXML private Button rewindBtn;          // 后退30秒按钮
    @FXML private Button forwardBtn;         // 快进30秒按钮
    @FXML private Button speedBtn;           // 播放倍速调节按钮
    @FXML private Button prevMediaBtn;       // 上一首切换按钮
    @FXML private Button nextMediaBtn;       // 下一首切换按钮
    @FXML private Slider volumeSlider;       // 音量调节滑块
    @FXML private Slider progressSlider;     // 播放进度调节滑块
    @FXML private Text currentTimeLabel;     // 当前播放时长显示文本
    @FXML private Text totalTimeLabel;       // 媒体总时长显示文本

    // -------------------------- 播放列表控件：播放列表管理相关控件 --------------------------
    @FXML private ListView<File> playlistView;          // 播放列表视图
    @FXML private Button removeFromPlaylistBtn;        // 移除选中列表项按钮
    @FXML private Button clearPlaylistBtn;             // 清空播放列表按钮
    @FXML private VBox playlistContainer;              // 播放列表容器（支持折叠/展开）
    @FXML private ToggleButton playlistToggleBtn;      // 播放列表折叠/展开切换按钮
    @FXML private Label playlistCountLabel;            // 播放列表数量统计标签
    @FXML private TextField searchField;               // 播放列表搜索过滤输入框

    // -------------------------- 播放列表数据模型：维护播放列表数据 --------------------------
    private final ObservableList<File> playlist = FXCollections.observableArrayList(); // 原始播放列表数据
    private final FilteredList<File> filteredPlaylist; // 过滤后的播放列表（支持搜索）
    private int currentPlayingIndex = -1; // 当前播放媒体在列表中的索引
    private boolean isAutoPlayNext = true; // 是否开启播放结束后自动播放下一首

    // -------------------------- 媒体核心变量：控制媒体播放状态 --------------------------
    private MediaPlayer mediaPlayer;       // 媒体播放器核心对象
    private File selectedMediaFile;        // 当前选中的媒体文件
    private Image bgImageObj;              // 背景图片对象
    private boolean isPlaying = false;     // 是否处于播放状态
    private boolean isDraggingProgress = false; // 是否正在拖动进度条
    private boolean isMediaEnded = false;  // 媒体是否播放结束
    private boolean isSwitchingMedia = false; // 是否正在切换媒体（防止快速重复切换）
    private boolean isMediaReady = false;  // 媒体是否加载就绪

    // -------------------------- 倍速相关：播放倍速配置 --------------------------
    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0); // 支持的倍速选项
    private double currentSpeed = 1.0;     // 当前播放倍速（默认1.0倍）
    private ContextMenu speedMenu;         // 倍速选择右键菜单

    // -------------------------- 常量配置：固定参数定义 --------------------------
    private static final int SEEK_STEP = 30; // 快进/后退单次调整时间（秒）

    // -------------------------- 图标对象：按钮上的自定义图形图标 --------------------------
    private final Polygon playIcon;        // 播放图标（三角形）
    private final HBox pauseIcon;          // 暂停图标（双矩形）
    private final HBox rewindIcon;         // 后退图标（双左箭头）
    private final HBox forwardIcon;        // 快进图标（双右箭头）
    private final HBox prevMediaIcon;      // 上一首图标（单左箭头）
    private final HBox nextMediaIcon;      // 下一首图标（单右箭头）

    // -------------------------- 主题管理器：负责主题切换与样式管理 --------------------------
    private final ThemeManager themeManager = ThemeManager.getInstance();

    /**
     * 构造方法：初始化各类图标、倍速菜单和播放列表过滤对象
     */
    public PlayerController() {
        // 初始化播放图标（三角形）
        playIcon = new Polygon(6.0, 2.0, 6.0, 22.0, 22.0, 12.0);
        playIcon.setFill(Color.WHITE);
        playIcon.setSmooth(true);

        // 初始化暂停图标（双矩形）
        Rectangle rect1 = new Rectangle(0, 0, 7, 20);
        Rectangle rect2 = new Rectangle(10, 0, 7, 20);
        rect1.setFill(Color.WHITE);
        rect2.setFill(Color.WHITE);
        pauseIcon = new HBox(3, rect1, rect2);
        pauseIcon.setAlignment(Pos.CENTER);
        pauseIcon.setPrefSize(24, 24);

        // 初始化后退图标（双左箭头）
        Polygon tri1Left = new Polygon(20.0, 4.0, 20.0, 20.0, 8.0, 12.0);
        Polygon tri2Left = new Polygon(12.0, 4.0, 12.0, 20.0, 0.0, 12.0);
        tri1Left.setFill(Color.WHITE);
        tri2Left.setFill(Color.WHITE);
        rewindIcon = new HBox(1, tri2Left, tri1Left);
        rewindIcon.setAlignment(Pos.CENTER);
        rewindIcon.setPrefSize(24, 24);

        // 初始化快进图标（双右箭头）
        Polygon tri1Right = new Polygon(4.0, 4.0, 4.0, 20.0, 16.0, 12.0);
        Polygon tri2Right = new Polygon(12.0, 4.0, 12.0, 20.0, 24.0, 12.0);
        tri1Right.setFill(Color.WHITE);
        tri2Right.setFill(Color.WHITE);
        forwardIcon = new HBox(1, tri1Right, tri2Right);
        forwardIcon.setAlignment(Pos.CENTER);
        forwardIcon.setPrefSize(24, 24);

        // 初始化上一首图标（单左箭头）
        Polygon tri3Left = new Polygon(12.0, 5.0, 12.0, 25.0, 4.0, 15.0);
        tri3Left.setFill(Color.WHITE);
        prevMediaIcon = new HBox(tri3Left);
        prevMediaIcon.setAlignment(Pos.CENTER);
        prevMediaIcon.setPrefSize(24, 24);

        // 初始化下一首图标（单右箭头）
        Polygon tri3Right = new Polygon(8.0, 5.0, 8.0, 25.0, 16.0, 15.0);
        tri3Right.setFill(Color.WHITE);
        nextMediaIcon = new HBox(tri3Right);
        nextMediaIcon.setAlignment(Pos.CENTER);
        nextMediaIcon.setPrefSize(24, 24);

        // 初始化倍速选择菜单
        initSpeedMenu();
        // 初始化播放列表过滤对象（默认显示所有列表项）
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
    }

    /**
     * 初始化方法：FXML加载完成后自动调用，完成控制器初始化工作
     * 包括样式加载、控件绑定、事件注册、初始状态设置等
     */
    @FXML
    public void initialize() {
        // 延迟初始化CSS和主题下拉框，确保界面元素加载完成
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                initCSS();
                initThemeComboBox();
                updateSpeedButtonStyle();
                logger.info("【初始化】主题与下拉框已完成初始化");
            });
        });

        // 设置初始状态：未选择文件
        fileNameLabel.setText("未选择文件");

        // 为功能按钮绑定自定义图标
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);
        prevMediaBtn.setGraphic(prevMediaIcon);
        nextMediaBtn.setGraphic(nextMediaIcon);

        // 初始化各类功能模块
        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton();
        initPrevNextButtons();
        initPlaylist();
        initPlaylistToggle();

        // 初始化时长标签状态
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");
        updateTimeLabelColor();

        // 延迟初始化背景图和媒体视图尺寸绑定，避免界面加载异常
        Platform.runLater(() -> {
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        });

        // 绑定按钮点击事件
        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());

        // 绑定音量调节事件：滑块值变化同步更新媒体音量
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && isMediaReady) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        // 绑定播放列表搜索事件：输入内容实时过滤列表项
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            final String searchText = newVal;
            filteredPlaylist.setPredicate(file -> {
                if (searchText == null || searchText.trim().isEmpty()) {
                    return true;
                }
                return file.getName().toLowerCase().contains(searchText.toLowerCase());
            });
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });

        // 设置初始状态：所有播放控制按钮禁用
        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    /**
     * 初始化主题下拉框：添加主题选项、设置默认值、绑定切换事件
     */
    private void initThemeComboBox() {
        // 添加主题显示名称到下拉框
        themeComboBox.getItems().addAll(themeManager.getThemeDisplayNames());

        // 设置默认值为上次保存的主题
        ThemeManager.Theme savedTheme = themeManager.getCurrentTheme();
        String savedThemeName = savedTheme.getDisplayName();
        themeComboBox.setValue(savedThemeName);
        logger.info("【下拉框初始化】默认选中上次保存的主题：" + savedThemeName);

        // 绑定主题切换事件：同步更新各类UI样式
        themeComboBox.setOnAction(e -> {
            final String selectedName = themeComboBox.getValue();
            final ThemeManager.Theme selectedTheme = themeManager.getThemeByDisplayName(selectedName);
            Scene scene = rootPane.getScene();
            if (scene != null) {
                themeManager.switchTheme(selectedTheme, scene);
                updateProgressSliderStyle(progressSlider.getValue());
                updateSpeedMenuStyle();
                updateSpeedButtonStyle();
                updateTimeLabelColor();
                updateTimeDisplay(
                        mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO,
                        mediaPlayer != null ? mediaPlayer.getTotalDuration() : Duration.ZERO
                );
                logger.info("【下拉框事件】已切换至主题：" + selectedName);
            }
        });
    }

    /**
     * 初始化CSS样式：加载基础通用样式和当前主题样式
     */
    private void initCSS() {
        Scene scene = rootPane.getScene();
        if (scene == null) {
            logger.severe("【CSS加载】场景为空，无法加载样式！");
            return;
        }

        // 清空原有主题CSS，避免样式冲突
        themeManager.removeAllThemeCss(scene);

        // 加载基础通用样式（不含主题色调）
        URL baseCssUrl = getClass().getResource("/css/player.css");
        if (baseCssUrl != null) {
            String baseCssUrlStr = baseCssUrl.toExternalForm();
            if (!scene.getStylesheets().contains(baseCssUrlStr)) {
                scene.getStylesheets().add(baseCssUrlStr);
                logger.info("【CSS加载】成功加载基础样式：" + baseCssUrlStr);
            } else {
                logger.info("【CSS加载】基础样式已存在：" + baseCssUrlStr);
            }
        } else {
            logger.severe("【CSS加载】基础样式文件不存在：/css/player.css");
        }

        // 加载当前主题样式（优先级高于基础样式）
        themeManager.forceLoadCurrentTheme(scene);
        logger.info("【CSS加载】成功加载保存的主题：" + themeManager.getCurrentTheme().getDisplayName());
    }

    /**
     * 初始化倍速选择菜单：添加倍速选项并绑定选择事件
     */
    private void initSpeedMenu() {
        speedMenu = new ContextMenu();

        // 为每个倍速选项创建菜单项并绑定事件
        for (double speed : speedOptions) {
            final double finalSpeed = speed;
            MenuItem item = new MenuItem(String.format("%.2fx", finalSpeed));
            item.getStyleClass().add("speed-menu-item");
            item.setOnAction(e -> {
                currentSpeed = finalSpeed;
                updateSpeedButtonText();
                if (mediaPlayer != null && isMediaReady) {
                    mediaPlayer.setRate(currentSpeed);
                }
            });
            speedMenu.getItems().add(item);
        }
    }

    /**
     * 更新倍速菜单样式：根据当前主题切换菜单样式类
     */
    private void updateSpeedMenuStyle() {
        ThemeManager.Theme currentTheme = themeManager.getCurrentTheme();
        logger.info("【倍速菜单样式更新】当前主题：" + currentTheme.getDisplayName());

        // 清空原有样式类，避免冲突
        speedMenu.getStyleClass().clear();
        // 根据主题添加对应样式类
        if (currentTheme == ThemeManager.Theme.DARK) {
            speedMenu.getStyleClass().add("speed-menu-dark");
        } else {
            speedMenu.getStyleClass().add("speed-menu-light");
        }
    }

    /**
     * 更新倍速按钮样式：根据当前主题设置按钮背景、文字颜色等样式
     */
    private void updateSpeedButtonStyle() {
        ThemeManager.Theme currentTheme = themeManager.getCurrentTheme();
        String bgColor, textColor, effectColor;

        // 根据主题配置样式参数
        if (currentTheme == ThemeManager.Theme.DARK) {
            bgColor = "#363636";
            textColor = "#ffffff";
            effectColor = "#000000";
        } else {
            bgColor = "#e0e0e0";
            textColor = "#333333";
            effectColor = "#cccccc";
        }

        // 应用按钮样式
        speedBtn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: 'Microsoft YaHei'; " +
                        "-fx-font-size: 13px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 0 10px; " +
                        "-fx-border-width: 0; -fx-effect: dropshadow(gaussian, %s, 2, 0, 0, 1);",
                bgColor, textColor, effectColor
        ));
    }

    /**
     * 更新时长标签颜色：根据当前主题设置时长文本颜色
     */
    private void updateTimeLabelColor() {
        ThemeManager.Theme currentTheme = themeManager.getCurrentTheme();
        Color textColor = currentTheme == ThemeManager.Theme.DARK ? Color.WHITE : Color.BLACK;
        currentTimeLabel.setFill(textColor);
        totalTimeLabel.setFill(textColor);
    }

    /**
     * 初始化倍速按钮：设置初始文本和点击事件
     */
    private void initSpeedButton() {
        updateSpeedButtonText();
        speedBtn.setOnAction(e -> {
            if (!speedBtn.isDisabled()) {
                updateSpeedMenuStyle();
                speedMenu.show(speedBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
        speedBtn.setDisable(true);
    }

    /**
     * 更新倍速按钮文本：显示当前播放倍速
     */
    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

    /**
     * 初始化上一首/下一首按钮：设置样式和点击事件
     */
    private void initPrevNextButtons() {
        // 设置按钮样式
        prevMediaBtn.setStyle("-fx-background-color: #363636; -fx-border-width: 0; -fx-cursor: hand; -fx-padding: 0; -fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        prevMediaBtn.setPrefSize(40.0, 30.0);
        nextMediaBtn.setStyle(prevMediaBtn.getStyle());
        nextMediaBtn.setPrefSize(40.0, 30.0);

        // 绑定点击事件
        prevMediaBtn.setOnAction(e -> playPreviousMedia());
        nextMediaBtn.setOnAction(e -> playNextMedia());

        // 更新按钮可用状态
        updatePrevNextBtnStatus();
    }

    /**
     * 播放上一首媒体：切换到播放列表中当前项的前一项
     */
    private void playPreviousMedia() {
        if (isSwitchingMedia || playlist.isEmpty() || currentPlayingIndex <= 0) return;
        playFromPlaylist(currentPlayingIndex - 1);
    }

    /**
     * 播放下一首媒体：切换到播放列表中当前项的后一项
     */
    private void playNextMedia() {
        if (isSwitchingMedia || playlist.isEmpty() || currentPlayingIndex >= playlist.size() - 1) return;
        playFromPlaylist(currentPlayingIndex + 1);
    }

    /**
     * 更新上一首/下一首按钮可用状态：根据当前播放索引和列表长度判断
     */
    private void updatePrevNextBtnStatus() {
        if (playlist.isEmpty() || currentPlayingIndex == -1) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        prevMediaBtn.setDisable(currentPlayingIndex <= 0);
        nextMediaBtn.setDisable(currentPlayingIndex >= playlist.size() - 1);
    }

    /**
     * 后退30秒：将播放进度向前调整指定时长
     */
    private void seekBackward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) return;
        double newTime = Math.max(0, mediaPlayer.getCurrentTime().toSeconds() - SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        updateProgressAndTime(newTime);
    }

    /**
     * 快进30秒：将播放进度向后调整指定时长
     */
    private void seekForward() {
        if (mediaPlayer == null || !isMediaReady || mediaPlayer.getTotalDuration() == null) return;
        double total = mediaPlayer.getTotalDuration().toSeconds();
        double newTime = Math.min(total, mediaPlayer.getCurrentTime().toSeconds() + SEEK_STEP);
        mediaPlayer.seek(Duration.seconds(newTime));
        updateProgressAndTime(newTime);
    }

    /**
     * 更新播放进度和时长显示：同步进度条值和时长文本
     * @param newTime 新的播放时间（秒）
     */
    private void updateProgressAndTime(double newTime) {
        final double finalNewTime = newTime;
        final double total = mediaPlayer.getTotalDuration().toSeconds();
        final double progress = finalNewTime / total;
        progressSlider.setValue(progress);
        updateProgressSliderStyle(progress);
        updateTimeDisplay(Duration.seconds(finalNewTime), mediaPlayer.getTotalDuration());
    }

    /**
     * 初始化播放列表折叠/展开功能：绑定切换按钮事件，添加淡入淡出动画
     */
    private void initPlaylistToggle() {
        playlistContainer.setOpacity(0.0);

        // 绑定切换按钮选中状态变化事件
        playlistToggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // 展开：显示容器并播放淡入动画
                playlistContainer.setVisible(true);
                new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(playlistContainer.opacityProperty(), 1.0))).play();
            } else {
                // 折叠：播放淡出动画，结束后隐藏容器
                Timeline fadeOut = new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(playlistContainer.opacityProperty(), 0.0)));
                fadeOut.setOnFinished(e -> playlistContainer.setVisible(false));
                fadeOut.play();
            }
        });

        // 设置初始状态：未选中（折叠状态）
        playlistToggleBtn.setSelected(false);
        playlistContainer.setVisible(false);
    }

    /**
     * 初始化播放列表：绑定数据、设置单元格样式、绑定点击事件和列表变化监听
     */
    private void initPlaylist() {
        // 绑定过滤后的列表到视图
        playlistView.setItems(filteredPlaylist);

        // 设置列表单元格自定义样式
        playlistView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(file.getName());
                    final int currentIdx = playlist.indexOf(file);
                    // 当前播放项高亮显示
                    if (currentIdx == currentPlayingIndex) {
                        setStyle("-fx-text-fill: #1E90FF; -fx-font-weight: bold; -fx-background-color: rgba(30, 144, 255, 0.1);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // 绑定列表项点击事件：单击播放或暂停对应媒体
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                File selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                final int idx = playlist.indexOf(selected);
                if (idx == currentPlayingIndex && mediaPlayer != null && isMediaReady) {
                    togglePlayPause();
                } else {
                    playFromPlaylist(idx);
                }
            }
        });

        // 绑定列表变化监听：更新列表数量和按钮状态
        playlist.addListener((javafx.collections.ListChangeListener<File>) change -> {
            updatePlaylistCount();
            updatePrevNextBtnStatus();
        });

        // 初始化列表数量显示
        updatePlaylistCount();
    }

    /**
     * 更新播放列表数量统计：显示总数量或过滤后数量/总数量
     */
    private void updatePlaylistCount() {
        final int total = playlist.size();
        final int filtered = filteredPlaylist.size();
        playlistCountLabel.setText(searchField.getText().isEmpty() ? total + " 首" : filtered + "/" + total + " 首");
    }

    /**
     * 从播放列表中移除选中项：删除当前选中的媒体文件并处理后续状态
     */
    private void removeSelectedFromPlaylist() {
        File selected = playlistView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        final int idx = playlist.indexOf(selected);
        final boolean isCurrent = idx == currentPlayingIndex;
        playlist.remove(selected);

        // 处理当前播放项被移除的逻辑
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
            // 调整当前播放索引（前序项被移除）
            currentPlayingIndex--;
        }
        playlistView.refresh();
    }

    /**
     * 清空播放列表：删除所有媒体文件并重置播放状态
     */
    private void clearPlaylist() {
        // 释放媒体资源
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        // 重置数据和状态
        playlist.clear();
        currentPlayingIndex = -1;
        isPlaying = false;
        isMediaEnded = true;
        isSwitchingMedia = false;
        isMediaReady = false;

        // 重置UI状态
        fileNameLabel.setText("未选择文件");
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        playPauseBtn.setGraphic(playIcon);
        setPlaybackButtonsDisabled(true);
        playlistView.refresh();
    }

    /**
     * 打开媒体文件：通过文件选择器选择文件并添加到播放列表
     */
    private void openMediaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择媒体文件");
        // 设置支持的媒体格式
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("媒体文件", "*.mp4", "*.avi", "*.mkv", "*.mp3", "*.wav", "*.flac", "*.aac"),
                new FileChooser.ExtensionFilter("视频文件", "*.mp4", "*.avi", "*.mkv"),
                new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav", "*.flac", "*.aac")
        );

        // 弹出文件选择器
        selectedMediaFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedMediaFile == null) return;

        // 重置倍速并更新按钮文本
        currentSpeed = 1.0;
        updateSpeedButtonText();

        // 将文件添加到播放列表（避免重复添加）
        if (!playlist.contains(selectedMediaFile)) {
            playlist.add(selectedMediaFile);
            currentPlayingIndex = playlist.size() - 1;
            searchField.clear();
            playlistView.getSelectionModel().select(currentPlayingIndex);
        } else {
            currentPlayingIndex = playlist.indexOf(selectedMediaFile);
            playlistView.getSelectionModel().select(currentPlayingIndex);
        }

        // 播放选中的媒体文件
        playFromPlaylist(currentPlayingIndex);
    }

    /**
     * 从播放列表指定索引播放媒体：加载并播放对应位置的媒体文件
     * @param index 播放列表中的索引
     */
    private void playFromPlaylist(int index) {
        // 重置倍速并更新样式
        currentSpeed = 1.0;
        updateSpeedButtonText();
        updateSpeedButtonStyle();

        // 校验索引有效性和切换状态
        if (index < 0 || index >= playlist.size() || isSwitchingMedia) return;
        isSwitchingMedia = true;
        isMediaReady = false;

        // 获取目标媒体文件并更新当前播放索引
        final File file = playlist.get(index);
        currentPlayingIndex = index;

        // 同步更新播放列表选中状态并滚动到对应项
        Platform.runLater(() -> {
            playlistView.getSelectionModel().clearSelection();
            playlistView.getSelectionModel().select(index);
            playlistView.scrollTo(index);
        });

        // 释放旧媒体资源
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        // 重置UI初始状态
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
        mediaView.toFront();
        bgImage.toFront();
        Platform.runLater(() -> playlistView.refresh());
        setPlaybackButtonsDisabled(true);

        // 创建并初始化新媒体播放器
        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.toFront();
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.setRate(currentSpeed);

            // 媒体就绪监听：初始化播放状态和UI
            mediaPlayer.setOnReady(() -> Platform.runLater(() -> {
                try {
                    isMediaReady = true;
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    if (totalDuration == null || totalDuration.isUnknown()) {
                        throw new RuntimeException("无法获取媒体时长");
                    }

                    // 绑定进度更新、更新时长显示并开始播放
                    bindProgressUpdate();
                    updateTimeDisplay(Duration.ZERO, totalDuration);
                    mediaPlayer.play();
                    isPlaying = true;
                    playPauseBtn.setGraphic(pauseIcon);

                    // 隐藏背景图和遮罩，确保媒体视图置顶
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    bgImage.toBack();
                    mediaView.toFront();

                    // 启用播放控制按钮并更新上/下一首状态
                    setPlaybackButtonsDisabled(false);
                    updatePrevNextBtnStatus();
                } catch (Exception e) {
                    handleMediaError(file);
                } finally {
                    isSwitchingMedia = false;
                }
            }));

            // 媒体播放结束监听：处理自动播放下一首或重置状态
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
                    playFromPlaylist(currentPlayingIndex + 1);
                } else {
                    // 重置播放状态和UI
                    mediaPlayer.pause();
                    mediaPlayer.seek(Duration.ZERO);
                    isPlaying = false;
                    isMediaEnded = true;
                    playPauseBtn.setGraphic(playIcon);
                    progressSlider.setValue(0.0);
                    updateProgressSliderStyle(0.0);

                    // 确保时长显示有效
                    Duration validTotalDuration = mediaPlayer.getTotalDuration() != null
                            ? mediaPlayer.getTotalDuration()
                            : Duration.ZERO;
                    updateTimeDisplay(Duration.ZERO, validTotalDuration);

                    // 显示背景图和遮罩，媒体视图置底
                    bgImage.setVisible(true);
                    blackMask.setVisible(true);
                    bgImage.toFront();
                    mediaView.toBack();
                    logger.info("【媒体播放结束】已重置到开头，显示背景图，停止进度更新");
                }
                isSwitchingMedia = false;
            }));

            // 媒体暂停监听：更新UI显示状态
            mediaPlayer.setOnPaused(() -> Platform.runLater(() -> {
                updateCenterPlayIconVisibility();
                final boolean showBg = isMediaEnded;
                bgImage.setVisible(showBg);
                blackMask.setVisible(showBg);
                if (showBg) {
                    bgImage.toFront();
                    mediaView.toBack();
                }
            }));

            // 媒体播放监听：更新UI显示状态
            mediaPlayer.setOnPlaying(() -> Platform.runLater(() -> {
                updateCenterPlayIconVisibility();
                bgImage.setVisible(false);
                blackMask.setVisible(false);
                bgImage.toBack();
                mediaView.toFront();
            }));

            // 媒体错误监听：处理播放异常
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

    /**
     * 处理媒体播放错误：弹出提示框并重置相关状态
     * @param file 出错的媒体文件
     */
    private void handleMediaError(File file) {
        System.err.println("媒体错误：" + (mediaPlayer != null ? mediaPlayer.getError() : "未知错误"));
        isPlaying = false;
        isMediaReady = false;
        playPauseBtn.setGraphic(playIcon);
        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();
        mediaView.toBack();

        // 移除出错的文件并重置播放索引
        if (file != null && playlist.contains(file)) {
            playlist.remove(file);
            currentPlayingIndex = -1;
        }

        // 弹出错误提示框
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("播放错误");
        alert.setHeaderText("无法播放文件");
        alert.setContentText("文件格式不受支持或已损坏：" + file.getName());
        alert.showAndWait();

        // 刷新列表并更新按钮状态
        playlistView.refresh();
        setPlaybackButtonsDisabled(true);
        updatePrevNextBtnStatus();
    }

    /**
     * 切换播放/暂停状态：根据当前状态切换媒体播放或暂停
     */
    private void togglePlayPause() {
        if (mediaPlayer == null || !isMediaReady) {
            if (!playlist.isEmpty() && currentPlayingIndex == -1) {
                playFromPlaylist(0);
            }
            return;
        }

        if (isPlaying) {
            // 暂停逻辑：暂停媒体并更新UI
            mediaPlayer.pause();
            playPauseBtn.setGraphic(playIcon);
            final boolean showBg = isMediaEnded;
            bgImage.setVisible(showBg);
            blackMask.setVisible(showBg);
            if (showBg) {
                bgImage.toFront();
                mediaView.toBack();
            }
        } else {
            // 播放逻辑：播放媒体并更新UI
            isMediaEnded = false;
            mediaPlayer.play();
            playPauseBtn.setGraphic(pauseIcon);
            bgImage.setVisible(false);
            blackMask.setVisible(false);
            mediaView.toFront();
            bgImage.toBack();
        }
        isPlaying = !isPlaying;
        updateCenterPlayIconVisibility();
    }

    /**
     * 停止媒体播放：重置播放状态和UI显示
     */
    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO);
            isPlaying = false;
            isMediaEnded = true;
            playPauseBtn.setGraphic(playIcon);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);

            // 确保时长显示有效
            Duration validTotalDuration = mediaPlayer.getTotalDuration() != null
                    ? mediaPlayer.getTotalDuration()
                    : Duration.ZERO;
            updateTimeDisplay(Duration.ZERO, validTotalDuration);

            // 显示背景图和遮罩，媒体视图置底
            final boolean showBg = isMediaEnded;
            bgImage.setVisible(showBg);
            blackMask.setVisible(showBg);
            if (showBg) {
                bgImage.toFront();
                mediaView.toBack();
            }
            updateCenterPlayIconVisibility();
        }
    }

    /**
     * 初始化进度条：绑定鼠标事件，处理进度拖动和点击调整
     */
    private void initProgressSlider() {
        // 鼠标按下：标记为拖动状态
        progressSlider.setOnMousePressed(e -> isDraggingProgress = true);

        // 鼠标释放：结束拖动并更新播放进度
        progressSlider.setOnMouseReleased(e -> {
            isDraggingProgress = false;
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                final double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                final double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);

                // 确保播放状态下画面正常显示
                if (isPlaying) {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    mediaView.toFront();
                    bgImage.toBack();
                }
            }
        });

        // 鼠标点击：直接调整到点击位置的进度
        progressSlider.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && mediaPlayer.getTotalDuration() != null) {
                final double seekTime = progressSlider.getValue() * mediaPlayer.getTotalDuration().toSeconds();
                final double progress = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(seekTime));
                updateTimeDisplay(mediaPlayer.getCurrentTime(), mediaPlayer.getTotalDuration());
                updateProgressSliderStyle(progress);

                // 确保播放状态下画面正常显示
                if (isPlaying) {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    mediaView.toFront();
                    bgImage.toBack();
                }
            }
        });

        // 初始化进度条样式
        updateProgressSliderStyle(0.0);
    }

    /**
     * 更新进度条样式：根据当前主题和播放进度设置进度条渐变样式
     * @param progress 播放进度（0.0 ~ 1.0）
     */
    private void updateProgressSliderStyle(double progress) {
        // 进度值合法性校验
        if (Double.isNaN(progress) || Double.isInfinite(progress)) progress = 0.0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        final double finalProgress = progress;

        // 异步更新样式，避免UI阻塞
        Platform.runLater(() -> {
            Node track = progressSlider.lookup(".track");
            if (track == null) {
                logger.warning("【进度条样式】未找到track节点，样式设置失败");
                return;
            }

            // 根据主题获取样式颜色
            String primaryColor, trackColor;
            ThemeManager.Theme currentTheme = themeManager.getCurrentTheme();
            switch (currentTheme) {
                case DARK:
                    primaryColor = "#FF6347"; // 深色主题主色
                    trackColor = "#444444";   // 深色主题轨道色
                    break;
                case LIGHT:
                default:
                    primaryColor = "#1E90FF"; // 浅色主题主色
                    trackColor = "#e0e0e0";   // 浅色主题轨道色
            }

            // 构建渐变样式并应用
            final double progressPercent = finalProgress * 100;
            String gradientStyle = String.format(
                    "-fx-background-color: linear-gradient(to right, %s 0%%, %s %.2f%%, %s %.2f%%, %s 100%%) !important;" +
                            "-fx-background-radius: 0 !important;" +
                            "-fx-padding: 4px !important;",
                    primaryColor, primaryColor, progressPercent, trackColor, progressPercent, trackColor
            );
            track.setStyle(gradientStyle);
            logger.finest("【进度条样式】已更新，进度：%.2f%%，主题：%s".formatted(progressPercent, currentTheme.getDisplayName()));
        });
    }

    /**
     * 绑定进度更新：监听媒体播放时间变化，同步更新进度条和时长显示
     */
    private void bindProgressUpdate() {
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            // 播放结束或拖动进度时不更新
            if (!isDraggingProgress && !isMediaEnded && mediaPlayer.getTotalDuration() != null && isMediaReady) {
                double tempProgress = newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                tempProgress = Math.max(0.0, Math.min(1.0, tempProgress));
                final double finalProgress = tempProgress;
                final Duration finalNewTime = newTime;

                // 异步更新UI
                Platform.runLater(() -> {
                    progressSlider.setValue(finalProgress);
                    updateTimeDisplay(finalNewTime, mediaPlayer.getTotalDuration());
                });

                // 实时更新进度条样式
                updateProgressSliderStyle(finalProgress);
            }
        });
    }

    /**
     * 格式化时长：将Duration对象转换为"MM:SS"格式的字符串
     * @param duration 待格式化的时长
     * @return 格式化后的时长字符串
     */
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSeconds = (int) Math.floor(Math.max(0, duration.toSeconds()));
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 更新时长显示：同步当前播放时长和总时长的文本显示
     * @param current 当前播放时长
     * @param total 媒体总时长
     */
    private void updateTimeDisplay(Duration current, Duration total) {
        final Duration finalCurrent = current;
        final Duration finalTotal = total;
        Platform.runLater(() -> {
            currentTimeLabel.setText(formatDuration(finalCurrent));
            totalTimeLabel.setText(formatDuration(finalTotal));
            updateTimeLabelColor();
        });
    }

    /**
     * 初始化中央播放按钮：绑定点击事件，监听容器尺寸变化调整按钮大小
     */
    private void initCenterPlayIcon() {
        // 绑定点击事件：触发播放/暂停
        centerPlayIcon.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && !isPlaying) {
                togglePlayPause();
                // 确保画面正常显示
                bgImage.setVisible(false);
                blackMask.setVisible(false);
                mediaView.toFront();
                bgImage.toBack();
            }
        });

        // 监听容器尺寸变化，自动调整按钮大小
        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> adjustCenterPlayIconSize());

        // 设置鼠标样式为手型
        centerPlayIcon.setCursor(Cursor.HAND);
    }

    /**
     * 调整中央播放按钮大小：根据媒体容器尺寸按比例调整按钮尺寸
     */
    private void adjustCenterPlayIconSize() {
        double containerW = mediaContainer.getWidth();
        double containerH = mediaContainer.getHeight();
        if (containerW == 0 || containerH == 0) return;

        // 按比例计算按钮尺寸，限制最小和最大值
        double sizeRatio = 0.125;
        double iconWidth = Math.min(Math.max(containerW * sizeRatio, 40), 80);
        double iconHeight = iconWidth * 0.75;

        // 更新按钮形状坐标
        centerPlayIcon.getPoints().setAll(0.0, 0.0, 0.0, iconHeight, iconWidth, iconHeight / 2);
    }

    /**
     * 更新中央播放按钮可见性：根据播放状态和媒体就绪状态判断是否显示
     */
    private void updateCenterPlayIconVisibility() {
        Platform.runLater(() -> {
            centerPlayIcon.setVisible(mediaPlayer != null && isMediaReady && !isPlaying);
            if (centerPlayIcon.isVisible()) adjustCenterPlayIconSize();
        });
    }

    /**
     * 初始化媒体容器点击事件：点击容器空白区域触发播放/暂停
     */
    private void initMediaContainerClick() {
        mediaContainer.setOnMouseClicked(e -> {
            if (mediaPlayer != null && isMediaReady && e.getTarget() != centerPlayIcon && !centerPlayIcon.isHover()) {
                togglePlayPause();
                // 确保播放状态下画面正常显示
                if (!isPlaying) {
                    bgImage.setVisible(false);
                    blackMask.setVisible(false);
                    mediaView.toFront();
                    bgImage.toBack();
                }
            }
        });
    }

    /**
     * 初始化背景图：加载背景图片资源，监听加载完成事件并绑定尺寸调整
     */
    private void initBgImage() {
        URL bgImageUrl = getClass().getClassLoader().getResource("bg.jpg");
        if (bgImageUrl == null) {
            System.err.println("背景图 bg.jpg 未找到！");
            return;
        }

        // 加载背景图片（异步加载）
        bgImageObj = new Image(bgImageUrl.toExternalForm(), true);
        bgImageObj.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 1.0) {
                // 图片加载完成后设置并绑定尺寸调整
                bgImage.setImage(bgImageObj);
                mediaContainer.widthProperty().addListener((o, oldW, newW) -> adjustBgImageSize());
                mediaContainer.heightProperty().addListener((o, oldH, newH) -> adjustBgImageSize());
                adjustBgImageSize();

                // 设置初始可见性
                final boolean noMedia = selectedMediaFile == null;
                bgImage.setVisible(noMedia);
                blackMask.setVisible(noMedia);
                bgImage.toFront();
            }
        });
    }

    /**
     * 绑定媒体视图尺寸：将媒体视图尺寸与容器尺寸绑定，实现自适应
     */
    private void bindMediaViewSize() {
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());
    }

    /**
     * 调整背景图大小：根据容器尺寸按比例缩放背景图，保持居中显示
     */
    private void adjustBgImageSize() {
        if (bgImageObj == null || mediaContainer.getWidth() == 0 || mediaContainer.getHeight() == 0) return;

        // 计算缩放比例，保持图片比例不变
        final double imgW = bgImageObj.getWidth();
        final double imgH = bgImageObj.getHeight();
        final double containerW = mediaContainer.getWidth();
        final double containerH = mediaContainer.getHeight();
        final double scale = Math.min(containerW / imgW, containerH / imgH);

        // 设置图片尺寸和位置（居中显示）
        bgImage.setFitWidth(imgW * scale);
        bgImage.setFitHeight(imgH * scale);
        bgImage.setPreserveRatio(true);
        bgImage.setLayoutX((containerW - bgImage.getFitWidth()) / 2);
        bgImage.setLayoutY((containerH - bgImage.getFitHeight()) / 2);
        bgImage.toFront();
    }

    /**
     * 设置播放控制按钮禁用状态：批量更新各类播放相关控件的可用状态
     * @param disabled 是否禁用
     */
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
}
