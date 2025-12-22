package com.multimediaplayer;

import javafx.geometry.Insets;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Properties;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class PlayerController {
    // 新增：主题变量（预留记忆）
    private String selectedTheme = "默认主题"; // 默认主题

    // 原有变量保持不变
    @FXML private BorderPane rootPane;
    @FXML private StackPane mediaContainer;
    @FXML private Label fileNameLabel;
    @FXML private Button settingsBtn;

    private boolean isRememberLastPlay = false;
    private double lastPlaybackProgress = 0.0;
    private String lastPlayFilePath = "";
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "/MultimediaPlayerConfig.properties";

    @FXML private MediaView mediaView;
    @FXML private ImageView bgImage;
    @FXML private Polygon centerPlayIcon;
    @FXML private Rectangle blackMask;

    @FXML private Button openBtn;
    @FXML private Button playPauseBtn;
    @FXML private Button rewindBtn;
    @FXML private Button forwardBtn;
    @FXML private Button speedBtn;
    @FXML private Button prevMediaBtn;
    @FXML private Button nextMediaBtn;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Text currentTimeLabel;
    @FXML private Text totalTimeLabel;

    @FXML private ListView<File> playlistView;
    @FXML private Button removeFromPlaylistBtn;
    @FXML private Button clearPlaylistBtn;
    @FXML private VBox playlistContainer;
    @FXML private ToggleButton playlistToggleBtn;
    @FXML private Label playlistCountLabel;
    @FXML private TextField searchField;

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

    private final List<Double> speedOptions = Arrays.asList(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
    private double currentSpeed = 1.0; // 类成员变量，已在loadPlayConfig中初始化
    private ContextMenu speedMenu;

    private static final int SEEK_STEP = 30;

    private boolean isSwitchingMedia = false;
    private boolean isMediaReady = false;

    private final Polygon playIcon;
    private final HBox pauseIcon;
    private final HBox rewindIcon;
    private final HBox forwardIcon;
    private final HBox prevMediaIcon;
    private final HBox nextMediaIcon;

    public PlayerController() {
        // 原有图标初始化逻辑不变
        playIcon = new Polygon(
                6.0, 2.0,
                6.0, 22.0,
                22.0, 12.0
        );
        playIcon.setFill(Color.WHITE);
        playIcon.setSmooth(true);

        Rectangle rect1 = new Rectangle(0, 0, 7, 20);
        Rectangle rect2 = new Rectangle(10, 0, 7, 20);
        rect1.setFill(Color.WHITE);
        rect2.setFill(Color.WHITE);
        rect1.setSmooth(true);
        rect2.setSmooth(true);
        pauseIcon = new HBox(3, rect1, rect2);
        pauseIcon.setAlignment(Pos.CENTER);
        pauseIcon.setPrefSize(24, 24);

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

        Polygon tri1Right = new Polygon(4.0, 4.0, 4.0, 20.0, 16.0, 12.0);
        Polygon tri2Right = new Polygon(12.0, 4.0, 12.0, 20.0, 24.0, 12.0);
        tri1Right.setFill(Color.WHITE);
        tri2Right.setFill(Color.WHITE);
        tri1Right.setSmooth(true);
        tri2Right.setSmooth(true);
        forwardIcon = new HBox(1, tri1Right, tri2Right);
        forwardIcon.setAlignment(Pos.CENTER);
        forwardIcon.setPrefSize(24, 24);

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

        initSpeedMenu();
        filteredPlaylist = new FilteredList<>(playlist, p -> true);
    }

    @FXML
    public void initialize() {
        initCSS();
        fileNameLabel.setText("未选择文件");

        // 原有初始化逻辑不变
        playPauseBtn.setGraphic(playIcon);
        rewindBtn.setGraphic(rewindIcon);
        forwardBtn.setGraphic(forwardIcon);
        prevMediaBtn.setGraphic(prevMediaIcon);
        nextMediaBtn.setGraphic(nextMediaIcon);

        initCenterPlayIcon();
        initMediaContainerClick();
        initProgressSlider();
        initSpeedButton();
        initPrevNextButtons();
        initPlaylist();
        initPlaylistToggle();

        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");

        Platform.runLater(() -> {
            initBgImage();
            bindMediaViewSize();
            bgImage.setVisible(true);
            blackMask.setVisible(true);
            bgImage.toFront();
        });

        openBtn.setOnAction(e -> openMediaFile());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        rewindBtn.setOnAction(e -> seekBackward());
        forwardBtn.setOnAction(e -> seekForward());
        removeFromPlaylistBtn.setOnAction(e -> removeSelectedFromPlaylist());
        clearPlaylistBtn.setOnAction(e -> clearPlaylist());
        settingsBtn.setOnAction(e -> openSettingsDialog());

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && isMediaReady) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

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

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((winObs, oldWin, newWin) -> {
                    if (newWin != null) {
                        ((Stage) newWin).setOnCloseRequest(event -> {
                            savePlayConfig(); // 关闭时保存所有配置
                        });
                    }
                });
            }
        });

        // 自动恢复上次播放
        if (isRememberLastPlay && !lastPlayFilePath.isEmpty()) {
            File lastFile = new File(lastPlayFilePath);
            if (lastFile.exists() && playlist.contains(lastFile)) {
                int index = playlist.indexOf(lastFile);
                if (index >= 0) {
                    playFromPlaylist(index);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnReady(() -> {
                            mediaPlayer.seek(Duration.seconds(lastPlaybackProgress));
                        });
                    }
                }
            }
        }

        setPlaybackButtonsDisabled(true);
        updateTimeDisplay(Duration.ZERO, Duration.ZERO);
    }

    // 保存所有配置（新增音量、倍速、主题）
    private void savePlayConfig() {
        if (!isRememberLastPlay) {
            return;
        }

        try {
            Properties props = new Properties();
            // 原有配置
            props.setProperty("isRememberLastPlay", String.valueOf(isRememberLastPlay));
            props.setProperty("isAutoPlayNext", String.valueOf(isAutoPlayNext));
            // 新增：保存默认音量、倍速、主题
            props.setProperty("defaultVolume", String.valueOf(volumeSlider.getValue()));
            props.setProperty("defaultSpeed", String.valueOf(currentSpeed));
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

            // 上次播放信息
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

    // 加载所有配置（新增音量、倍速、主题）
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

            // 新增：恢复默认音量、倍速、主题
            // 恢复默认音量
            double savedVolume = Double.parseDouble(props.getProperty("defaultVolume", "0.5"));
            volumeSlider.setValue(savedVolume);
            // 恢复默认倍速
            currentSpeed = Double.parseDouble(props.getProperty("defaultSpeed", "1.0"));
            updateSpeedButtonText();
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

    // 原有方法：更新播放进度（不变）
    private void updateLastPlayProgress() {
        if (!isRememberLastPlay || mediaPlayer == null || !isMediaReady) {
            return;
        }
        if (currentPlayingIndex >= 0 && currentPlayingIndex < playlist.size()) {
            lastPlayFilePath = playlist.get(currentPlayingIndex).getAbsolutePath();
            lastPlaybackProgress = mediaPlayer.getCurrentTime().toSeconds();
        }
    }

    // 设置弹窗：保存主题选择（新增）
    private void openSettingsDialog() {
        Dialog<Void> settingsDialog = new Dialog<>();
        settingsDialog.setTitle("播放器设置");
        settingsDialog.setHeaderText("自定义播放器行为");
        settingsDialog.initOwner(((Stage) rootPane.getScene().getWindow()));

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

        // 4. 倍速默认值设置
        Label speedLabel = new Label("默认播放倍速：");
        speedLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");

        ComboBox<Double> speedComboBox = new ComboBox<>();
        speedComboBox.getItems().addAll(speedOptions);
        speedComboBox.setValue(currentSpeed); // 恢复保存的倍速
        speedComboBox.setCellFactory(list -> new ListCell<Double>() {
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

        speedComboBox.setButtonCell(new ListCell<Double>() {
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

        HBox speedBox = new HBox(10, speedLabel, speedComboBox);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        // 5. 主题选择（加载保存的主题）
        Label themeLabel = new Label("播放器主题：");
        themeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13px;");

        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("默认主题", "深色主题", "浅色主题"); // 示例主题选项
        themeComboBox.setValue(selectedTheme); // 恢复保存的主题
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
                speedBox,
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

    private void initPrevNextButtons() {
        prevMediaBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-border-width: 0; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        prevMediaBtn.setPrefWidth(40.0);
        prevMediaBtn.setPrefHeight(30.0);
        prevMediaBtn.setAlignment(Pos.CENTER);

        nextMediaBtn.setStyle("-fx-background-color: #363636; " +
                "-fx-border-width: 0; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, #000000, 2, 0, 0, 1);");
        nextMediaBtn.setPrefWidth(40.0);
        nextMediaBtn.setPrefHeight(30.0);
        nextMediaBtn.setAlignment(Pos.CENTER);

        prevMediaBtn.setOnAction(e -> playPreviousMedia());
        nextMediaBtn.setOnAction(e -> playNextMedia());

        updatePrevNextBtnStatus();
    }

    private void playPreviousMedia() {
        if (isSwitchingMedia || playlist.isEmpty()) {
            return;
        }
        if (currentPlayingIndex > 0) {
            playFromPlaylist(currentPlayingIndex - 1);
        }
    }

    private void playNextMedia() {
        if (isSwitchingMedia || playlist.isEmpty()) {
            return;
        }
        if (currentPlayingIndex < playlist.size() - 1) {
            playFromPlaylist(currentPlayingIndex + 1);
        }
    }

    private void updatePrevNextBtnStatus() {
        if (playlist.isEmpty()) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        if (currentPlayingIndex == -1) {
            prevMediaBtn.setDisable(true);
            nextMediaBtn.setDisable(true);
            return;
        }
        prevMediaBtn.setDisable(currentPlayingIndex <= 0);
        nextMediaBtn.setDisable(currentPlayingIndex >= playlist.size() - 1);
    }

    private void updateSpeedButtonText() {
        speedBtn.setText(String.format("%.2fx", currentSpeed));
    }

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

    private void updatePlaylistCount() {
        int totalCount = playlist.size();
        int filteredCount = filteredPlaylist.size();

        if (searchField.getText().isEmpty()) {
            playlistCountLabel.setText(totalCount + " 首");
        } else {
            playlistCountLabel.setText(filteredCount + "/" + totalCount + " 首");
        }
    }

    // ========== 修复：移除props引用，改用类成员变量currentSpeed ==========
    private void playFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        if (isSwitchingMedia) {
            return;
        }

        isSwitchingMedia = true;
        isMediaReady = false;

        try {
            File file = playlist.get(index);
            currentPlayingIndex = index;

            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                } catch (Exception e) {
                    System.err.println("释放MediaPlayer时出错: " + e.getMessage());
                }
                mediaPlayer = null;
            }

            // 修复：改用类成员变量currentSpeed（已在loadPlayConfig中初始化）
            updateSpeedButtonText();

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
            setPlaybackButtonsDisabled(true);

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            if (volumeSlider.getValue() > 0) {
                mediaPlayer.setVolume(volumeSlider.getValue());
            }
            mediaPlayer.setRate(currentSpeed); // 使用类成员变量

            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    try {
                        isMediaReady = true;

                        // 修复：移除props引用，直接使用currentSpeed
                        mediaPlayer.setRate(currentSpeed);
                        updateSpeedButtonText();

                        Duration totalDuration = mediaPlayer.getTotalDuration();
                        if (totalDuration == null || totalDuration.isUnknown()) {
                            throw new RuntimeException("无法获取媒体时长");
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

                        updatePrevNextBtnStatus();

                        isSwitchingMedia = false;
                    } catch (Exception e) {
                        System.err.println("媒体准备就绪时发生错误: " + e.getMessage());
                        handleMediaError(file);
                        isSwitchingMedia = false;
                    }
                });
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    handleMediaEnd();
                    isSwitchingMedia = false;
                });
            });

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

            mediaPlayer.setOnError(() -> {
                Platform.runLater(() -> {
                    handleMediaError(file);
                    isSwitchingMedia = false;
                });
            });

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

                    updateLastPlayProgress();
                }
            }
        });
    }

    private void handleMediaError(File file) {
        System.err.println("媒体播放错误：" + (mediaPlayer != null && mediaPlayer.getError() != null ?
                mediaPlayer.getError().getMessage() : "未知错误"));

        isPlaying = false;
        playPauseBtn.setGraphic(playIcon);
        isMediaReady = false;

        bgImage.setVisible(true);
        blackMask.setVisible(true);
        bgImage.toFront();

        playlistView.refresh();
        updateCenterPlayIconVisibility();
        setPlaybackButtonsDisabled(true);
        updatePrevNextBtnStatus();

        if (file != null && playlist.contains(file)) {
            playlist.remove(file);
            currentPlayingIndex = -1;
            updatePlaylistCount();
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("播放错误");
        alert.setHeaderText("无法播放文件");
        alert.setContentText("文件格式可能不受支持或已损坏: " +
                (file != null ? file.getName() : "未知文件"));
        alert.showAndWait();
    }

    // ========== 修复：移除props引用，改用类成员变量currentSpeed ==========
    private void handleMediaEnd() {
        if (isAutoPlayNext && currentPlayingIndex < playlist.size() - 1) {
            // 修复：移除props引用，直接使用currentSpeed
            updateSpeedButtonText();
            playFromPlaylist(currentPlayingIndex + 1);
            updatePrevNextBtnStatus();
        } else {
            isPlaying = false;
            isMediaEnded = true;
            playPauseBtn.setGraphic(playIcon);
            progressSlider.setValue(0.0);
            updateProgressSliderStyle(0.0);
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
            updatePrevNextBtnStatus();
        }
    }

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
        updatePrevNextBtnStatus();
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        isPlaying = false;
        isMediaReady = false;
        playPauseBtn.setGraphic(playIcon);
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
        updatePrevNextBtnStatus();
    }

    // ========== 修复：移除props引用，改用类成员变量currentSpeed ==========
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

        // 修复：移除props引用，直接使用currentSpeed（已在loadPlayConfig中初始化）
        updateSpeedButtonText();

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

        playFromPlaylist(currentPlayingIndex);
    }

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

    private void togglePlayPause() {
        if (mediaPlayer == null || !isMediaReady) {
            if (!playlist.isEmpty() && currentPlayingIndex == -1) {
                playFromPlaylist(0);
                updatePrevNextBtnStatus();
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
}