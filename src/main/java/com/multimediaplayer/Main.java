package com.multimediaplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

/**
 * 多媒体播放器应用主入口类
 * 负责启动JavaFX应用程序，加载FXML界面布局，初始化并显示应用窗口
 */
public class Main extends Application {

    /**
     * 默认构造方法
     * 继承自JavaFX的Application基类，用于初始化应用程序实例
     */
    public Main() {
        super();
    }

    /**
     * JavaFX应用程序的核心启动方法
     * 负责加载界面布局、初始化窗口样式并显示主窗口
     * @param stage JavaFX的主舞台（窗口容器），由FX框架自动传入
     */
    @Override
    public void start(Stage stage) {
        // 手动捕获所有异常，便于排查启动阶段的各类问题（如FXML缺失、资源加载失败等）
        try {
            // 获取FXML布局文件的资源路径（从类加载器中读取，兼容打包后的资源结构）
            URL fxmlUrl = Main.class.getClassLoader().getResource("fxml/player.fxml");

            // 打印FXML文件路径，用于验证资源是否成功定位（排错关键）
            System.out.println("FXML文件查找路径：" + (fxmlUrl == null ? "未找到fxml/player.fxml" : fxmlUrl.toExternalForm()));

            // 校验FXML资源是否存在，不存在则抛出明确异常
            if (fxmlUrl == null) {
                throw new IOException("FXML布局文件不存在！请检查资源目录结构，确保fxml/player.fxml已放置在资源根目录下");
            }

            // 加载FXML布局，创建JavaFX场景（包含界面所有控件）
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(fxmlLoader.load(), 800, 600); // 初始化窗口默认宽高：800x600

            // 配置主窗口属性并显示
            stage.setTitle("多媒体播放器"); // 设置窗口标题
            stage.setScene(scene); // 将场景绑定到主舞台
            stage.show(); // 显示主窗口

            // 调试日志：确认窗口正常启动，便于判断应用是否成功初始化
            System.out.println("多媒体播放器窗口已成功显示，应用启动完成！");

        } catch (Exception e) {
            // 异常处理：打印详细错误信息，便于定位启动失败原因
            System.err.println("应用启动失败！错误原因：" + e.getMessage());
            System.err.println("错误堆栈详情：");
            e.printStackTrace(); // 打印完整异常堆栈，精准定位问题代码行
        }
    }

    /*应用程序入口方法（Java程序执行的起点)*/
    public static void main(String[] args) {
        launch(args); // 启动JavaFX应用，底层会自动调用start()方法
    }
}
