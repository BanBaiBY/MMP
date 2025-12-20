package com.multimediaplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    public Main() {
        super();
    }

    @Override
    public void start(Stage stage) { // 移除throws IOException，手动捕获所有异常
        try {
            URL fxmlUrl = Main.class.getClassLoader().getResource("fxml/player.fxml");
            // 打印FXML路径，验证是否找到文件
            System.out.println("FXML文件路径：" + (fxmlUrl == null ? "未找到" : fxmlUrl.toExternalForm()));

            if (fxmlUrl == null) {
                throw new IOException("FXML文件 fxml/player.fxml 未找到，请检查资源目录结构");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setTitle("多媒体播放器");
            stage.setScene(scene);
            stage.show();

            // 打印启动成功日志，验证是否正常加载窗口
            System.out.println("JavaFX窗口已正常显示，主线程开始阻塞");

        } catch (Exception e) {
            // 打印所有异常堆栈，排查隐性错误
            System.err.println("应用启动异常：" + e.getMessage());
            e.printStackTrace(); // 打印完整堆栈，定位问题位置
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
