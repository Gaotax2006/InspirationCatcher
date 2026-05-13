package com.inspiration.catcher;

import atlantafx.base.theme.PrimerLight;
import com.inspiration.catcher.dao.DatabaseManager;
import com.inspiration.catcher.dao.ProjectDao;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static Stage primaryStage;
    // 主方法也保留
    public static void main(String[] args) {
        logger.info("应用程序开始启动...");
        try {
            // 设置系统属性
            System.setProperty("prism.order", "sw");
            System.setProperty("javafx.macosx.embedded", "true");
            // 启动JavaFX应用
            launch(args);
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            showErrorDialog("启动失败", e.getMessage());
        }
    }
    @Override
    public void start(Stage stage) throws Exception {
        logger.info("JavaFX应用程序启动");
        try {
            // 初始化数据库
            DatabaseManager.initializeDatabase();
            // 确保有默认项目
            ensureDefaultProject();
            // 设置全局异常处理
            Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> {
                // JGraphX SwingNode DnD 冲突引起的异常都是无害的，静默忽略
                String msg = throwable.getMessage() != null ? throwable.getMessage() : "";
                boolean isJGraphXDnD = throwable instanceof java.awt.dnd.InvalidDnDOperationException
                        && msg.contains("Drag and drop in progress");
                boolean isJGraphXCorruptedState = throwable instanceof IndexOutOfBoundsException
                        && containsCall(throwable, "com.mxgraph.swing");
                if (isJGraphXDnD || isJGraphXCorruptedState) {
                    logger.debug("JGraphX AWT DnD exception suppressed (harmless in SwingNode)");
                    return;
                }
                logger.error("未捕获的异常", throwable);
                throwable.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("应用程序遇到错误");
                    alert.setContentText(throwable.getMessage());
                    TextArea textArea = new TextArea(getStackTrace(throwable));
                    textArea.setEditable(false);
                    alert.getDialogPane().setExpandableContent(textArea);
                    alert.showAndWait();
                });
            });
            // 应用 AtlantaFX 主题（浅色）
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            // 加载主界面
            primaryStage = stage;
            loadMainView();
            // 设置应用程序关闭时的清理操作
            stage.setOnCloseRequest(_ -> {
                logger.info("应用程序正在关闭");
                DatabaseManager.closeConnection();
                Platform.exit();
                System.exit(0);
            });
            // 显示主窗口
            stage.show();
            logger.info("应用程序启动完成");
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("启动错误");
            alert.setHeaderText("无法启动应用程序");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            Platform.exit();
        }
    }
    // 添加辅助方法
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    // 确保默认项目的方法
    private void ensureDefaultProject() {
        ProjectDao projectDao = new ProjectDao();
        projectDao.createDefaultProject();
    }
    // 加载主界面
    private void loadMainView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 800);
        // 加载自定义样式（覆盖 AtlantaFX PrimerDark 基础主题）
        URL cssUrl = getClass().getResource("/css/app.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        primaryStage.setTitle("灵感捕手 - AI Powered Writing Assistant");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
    }
    // 检查堆栈中是否包含指定类名
    private static boolean containsCall(Throwable t, String className) {
        for (var e : t.getStackTrace()) {
            if (e.getClassName() != null && e.getClassName().contains(className)) return true;
        }
        return false;
    }

    // 显示错误对话框的方法
    private static void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}