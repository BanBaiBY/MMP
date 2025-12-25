package com.multimediaplayer.container;

import com.multimediaplayer.ui.api.PlayerUI;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 应用入口：负责启动JavaFX、装配模块、展示UI
 */
public class Main extends Application {
    private ModuleAssembler moduleAssembler;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 初始化模块装配器
            moduleAssembler = new ModuleAssembler();
            moduleAssembler.initModules();

            // 获取UI模块，启动主界面
            PlayerUI playerUI = moduleAssembler.getModule(PlayerUI.class);
            playerUI.show(primaryStage);

            moduleAssembler.getGlobalLogger().info("应用启动成功");
        } catch (Exception e) {
            moduleAssembler.getGlobalLogger().error("应用启动失败", e);
            System.exit(1);
        }

    }

    @Override
    public void stop() {
        // 应用关闭时销毁所有模块
        if (moduleAssembler != null) {
            moduleAssembler.destroyModules();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
