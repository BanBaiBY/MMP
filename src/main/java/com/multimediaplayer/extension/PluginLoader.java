package com.multimediaplayer.extension;

import com.multimediaplayer.container.AppContext;
import com.multimediaplayer.extension.api.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件加载器：主程序 + 集成式设置页面布局
 */
public class PluginLoader implements AutoCloseable {
    private final AppContext appContext;
    private final Logger logger;
    private final ConfigManager configManager;
    private final Map<String, URLClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();
    private final List<String> loadedPlugins = new ArrayList<>();
    // 静态日志（供main方法使用）
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(PluginLoader.class);
    // 主程序核心组件
    private JFrame mainFrame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    // 设置页面组件
    private JPanel settingsPanel;
    private JTable configTable;
    private JTextField newValueField;
    private JLabel validateTipLabel;

    public PluginLoader(AppContext appContext, ConfigManager configManager) {
        this.appContext = appContext;
        this.logger = appContext.getGlobalLogger();
        this.configManager = configManager;
        // 初始化主界面（含设置页面）
        initMainUI();
    }

    /**
     * 加载插件（核心逻辑不变）
     */
    @SuppressWarnings("unchecked")
    public <T> T loadPlugin(String pluginPath, Class<T> pluginInterface) {
        if (pluginPath == null || pluginPath.isEmpty()) {
            logger.warn("插件路径不能为空");
            return null;
        }
        if (pluginInterface == null) {
            logger.warn("插件接口类不能为空");
            return null;
        }

        File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            logger.warn("插件文件不存在：{}", pluginPath);
            return null;
        }

        try {
            URL pluginUrl = pluginFile.toURI().toURL();
            URLClassLoader tempClassLoader = new URLClassLoader(new URL[]{pluginUrl}, getClass().getClassLoader());
            Properties pluginProps = new Properties();
            try (InputStream propsStream = tempClassLoader.getResourceAsStream("META-INF/plugin.properties")) {
                if (propsStream == null) {
                    throw new IOException("插件配置文件不存在：META-INF/plugin.properties");
                }
                pluginProps.load(propsStream);
            }

            String pluginClass = pluginProps.getProperty("plugin.class");
            String pluginId = pluginProps.getProperty("plugin.id");

            if (pluginClass == null || pluginClass.isEmpty()) {
                throw new RuntimeException("插件配置缺失：plugin.class");
            }
            if (pluginId == null || pluginId.isEmpty()) {
                throw new RuntimeException("插件配置缺失：plugin.id");
            }

            if (loadedPlugins.contains(pluginId)) {
                logger.warn("插件已加载，无需重复加载：{}", pluginId);
                return null;
            }

            URLClassLoader classLoader = new URLClassLoader(new URL[]{pluginUrl}, null);
            Class<?> clazz = classLoader.loadClass(pluginClass);
            T pluginInstance = (T) clazz.getDeclaredConstructor().newInstance();

            if (!pluginInterface.isInstance(pluginInstance)) {
                throw new RuntimeException("插件未实现接口：" + pluginInterface.getName());
            }

            pluginInstance = configManager.initPlugin(pluginInstance);

            pluginClassLoaders.put(pluginId, classLoader);
            loadedPlugins.add(pluginId);
            logger.info("插件加载成功：{}（ID：{}）", pluginClass, pluginId);
            return pluginInstance;
        } catch (Exception e) {
            logger.error("加载插件失败", e);
            return null;
        }
    }

    public void unloadPlugin(String pluginId) {
        if (pluginId == null || !loadedPlugins.contains(pluginId)) {
            logger.warn("插件未加载，无需卸载：{}", pluginId);
            return;
        }
        URLClassLoader classLoader = pluginClassLoaders.remove(pluginId);
        if (classLoader != null) {
            try {
                classLoader.close();
                logger.info("插件类加载器已关闭：{}", pluginId);
            } catch (Exception e) {
                logger.error("关闭插件类加载器失败", e);
            }
        }
        loadedPlugins.remove(pluginId);
        logger.info("插件卸载成功：{}", pluginId);
    }

    public List<String> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    // ==================== 核心：主界面 + 集成式设置页面 ====================
    /**
     * 初始化主界面（CardLayout实现页面切换，包含首页和设置页面）
     */
    private void initMainUI() {
        // 1. 创建主窗口
        mainFrame = new JFrame("多媒体播放器");
        mainFrame.setSize(800, 600);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null); // 居中显示

        // 2. 创建顶部导航栏（页面切换）
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        navPanel.setBackground(Color.LIGHT_GRAY);

        // 首页按钮
        JButton homeButton = new JButton("首页");
        homeButton.setPreferredSize(new Dimension(80, 30));
        homeButton.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));

        // 设置页面按钮（核心）
        JButton settingsButton = new JButton("设置");
        settingsButton.setPreferredSize(new Dimension(80, 30));
        settingsButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "SETTINGS");
            refreshConfigTable(); // 切换到设置页面时刷新配置数据
        });

        navPanel.add(homeButton);
        navPanel.add(settingsButton);
        mainFrame.add(navPanel, BorderLayout.NORTH);

        // 3. 创建主内容面板（CardLayout：切换首页/设置页面）
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 3.1 首页面板
        JPanel homePanel = createHomePanel();
        // 3.2 设置页面面板（核心需求）
        settingsPanel = createSettingsPanel();

        // 将面板添加到CardLayout
        mainPanel.add(homePanel, "HOME");
        mainPanel.add(settingsPanel, "SETTINGS");

        mainFrame.add(mainPanel, BorderLayout.CENTER);

        // 默认显示首页
        cardLayout.show(mainPanel, "HOME");

        // 显示主窗口
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            STATIC_LOGGER.info("主界面已打开，包含设置页面布局");
        });
    }

    /**
     * 创建首页面板（基础功能展示）
     */
    private JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(Color.WHITE);

        // 首页标题
        JLabel titleLabel = new JLabel("多媒体播放器", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 30));
        homePanel.add(titleLabel, BorderLayout.NORTH);

        // 功能按钮区域
        JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 100));

        JButton playButton = new JButton("播放视频");
        playButton.setPreferredSize(new Dimension(120, 40));
        functionPanel.add(playButton);

        JButton pluginButton = new JButton("加载插件");
        pluginButton.setPreferredSize(new Dimension(120, 40));
        pluginButton.addActionListener(e -> {
            String pluginPath = JOptionPane.showInputDialog(mainFrame, "请输入插件JAR路径：");
            if (pluginPath != null && !pluginPath.isEmpty()) {
                try {
                    Class<?> decoderInterface = Class.forName("com.multimediaplayer.extension.api.DecoderPlugin");
                    Object decoderPlugin = loadPlugin(pluginPath, decoderInterface);
                    if (decoderPlugin != null) {
                        JOptionPane.showMessageDialog(mainFrame, "插件加载成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "插件加载失败！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (ClassNotFoundException ex) {
                    JOptionPane.showMessageDialog(mainFrame, "插件接口不存在！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        functionPanel.add(pluginButton);

        homePanel.add(functionPanel, BorderLayout.CENTER);

        return homePanel;
    }

    /**
     * 创建设置页面面板（核心：集成式设置布局）
     */
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // 1. 设置页面标题
        JLabel titleLabel = new JLabel("播放器设置", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 2. 配置列表区域（表格展示所有配置项）
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("配置项列表"));

        // 配置表格（列：配置项、当前值、默认值）
        String[] columnNames = {"配置项", "当前值", "默认值"};
        configTable = new JTable(new DefaultTableModel(columnNames, 0));
        configTable.setRowHeight(30);
        configTable.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        // 表格选择事件：更新校验提示
        configTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && configTable.getSelectedRow() != -1) {
                String selectedKey = (String) configTable.getValueAt(configTable.getSelectedRow(), 0);
                validateTipLabel.setText("校验规则：" + configManager.getValidateTip(selectedKey));
            }
        });
        tablePanel.add(new JScrollPane(configTable), BorderLayout.CENTER);
        panel.add(tablePanel, BorderLayout.CENTER);

        // 3. 操作区域（修改配置）
        JPanel operatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        operatePanel.setBorder(BorderFactory.createTitledBorder("配置修改"));

        // 新值输入框
        operatePanel.add(new JLabel("新值："));
        newValueField = new JTextField(20);
        operatePanel.add(newValueField);

        // 校验提示标签
        validateTipLabel = new JLabel("校验规则：请选择配置项");
        validateTipLabel.setForeground(Color.GRAY);
        operatePanel.add(validateTipLabel);

        // 修改按钮
        JButton modifyButton = new JButton("修改");
        modifyButton.setPreferredSize(new Dimension(80, 30));
        modifyButton.addActionListener(e -> modifyConfig());
        operatePanel.add(modifyButton);

        // 重置按钮
        JButton resetButton = new JButton("重置默认");
        resetButton.setPreferredSize(new Dimension(80, 30));
        resetButton.addActionListener(e -> resetConfig());
        operatePanel.add(resetButton);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.setPreferredSize(new Dimension(80, 30));
        saveButton.addActionListener(e -> {
            try {
                configManager.saveConfig();
                JOptionPane.showMessageDialog(mainFrame, "配置已保存！", "成功", JOptionPane.INFORMATION_MESSAGE);
                refreshConfigTable(); // 刷新表格
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame, "保存失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        operatePanel.add(saveButton);

        panel.add(operatePanel, BorderLayout.SOUTH);

        // 初始化配置表格
        refreshConfigTable();

        return panel;
    }

    /**
     * 刷新配置表格数据
     */
    private void refreshConfigTable() {
        DefaultTableModel model = (DefaultTableModel) configTable.getModel();
        // 清空原有数据
        model.setRowCount(0);
        // 获取所有配置项
        Map<String, String> allConfigs = configManager.getAllConfigs();
        // 填充表格
        for (String key : configManager.getConfigKeys()) {
            String currentValue = allConfigs.getOrDefault(key, "未知");
            String defaultValue = configManager.getDefaultConfig(key);
            model.addRow(new Object[]{key, currentValue, defaultValue});
        }
    }

    /**
     * 修改选中的配置项
     */
    private void modifyConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainFrame, "请先选择要修改的配置项！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String key = (String) configTable.getValueAt(selectedRow, 0);
        String newValue = newValueField.getText().trim();
        if (newValue.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "新值不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            configManager.setConfig(key, newValue);
            JOptionPane.showMessageDialog(mainFrame, "配置修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            newValueField.setText("");
            refreshConfigTable(); // 刷新表格
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(mainFrame, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 重置选中配置项到默认值
     */
    private void resetConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainFrame, "请先选择要重置的配置项！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String key = (String) configTable.getValueAt(selectedRow, 0);
        boolean success = configManager.resetConfigToDefault(key);
        if (success) {
            JOptionPane.showMessageDialog(mainFrame, "配置已重置为默认值！", "成功", JOptionPane.INFORMATION_MESSAGE);
            refreshConfigTable(); // 刷新表格
        } else {
            JOptionPane.showMessageDialog(mainFrame, "重置失败：无默认值", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void close() {
        // 关闭主窗口
        if (mainFrame != null) {
            mainFrame.dispose();
        }
        // 卸载所有插件
        loadedPlugins.forEach(this::unloadPlugin);
        pluginClassLoaders.clear();
        logger.info("插件加载器已释放，共卸载{}个插件", loadedPlugins.size());
    }

    // ==================== 程序启动入口 ====================
    public static void main(String[] args) {
        // 确保Swing GUI在事件调度线程中运行
        SwingUtilities.invokeLater(() -> {
            // 1. 初始化AppContext（实现所有抽象方法）
            AppContext appContext = new AppContext() {
                @Override
                public Logger getGlobalLogger() {
                    return LoggerFactory.getLogger("MultiMediaPlayer");
                }

                @Override
                public <T> T getModule(Class<T> moduleClass) {
                    STATIC_LOGGER.debug("获取模块{}，测试场景返回null", moduleClass.getName());
                    return null;
                }
            };

            // 2. 初始化核心组件并显示主窗口
            try (ConfigManager configManager = new ConfigManager(appContext);
                 PluginLoader pluginLoader = new PluginLoader(appContext, configManager)) {

                STATIC_LOGGER.info("========== 播放器启动成功 ==========");

            } catch (Exception e) {
                STATIC_LOGGER.error("软件运行异常", e);
                JOptionPane.showMessageDialog(null, "程序启动失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}