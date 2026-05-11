package com.inspiration.catcher.manager;

import com.inspiration.catcher.controller.MainController;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.*;

public class ShortcutManager {
    private static final Logger logger = LoggerFactory.getLogger(ShortcutManager.class);
    private final MainController mainController;
    private final AtomicBoolean shortcutsConfigured = new AtomicBoolean(false);
    // 当前活动区域的标记
    private String activeArea = "table"; // 默认全局，可选值: "mindmap", "editor", "table"
    public ShortcutManager(MainController mainController) {
        this.mainController = mainController;
    }
    public void setActiveArea(String area) {this.activeArea = area;logger.debug("切换到活动区域: {}", area);}
    public void setupShortcuts() {
        logger.info("开始设置快捷键");
        // 使用Platform.runLater延迟执行，等待UI完全加载
        runLater(() -> {
            try {
                // 先尝试获取场景
                Scene scene = mainController.getIdeaTableView().getScene();
                if (scene == null) {
                    logger.info("场景尚未加载，等待并重试...");
                    // 如果场景为空，使用监听器等待场景加载
                    setupShortcutsWithSceneListener();
                } else setupSceneShortcuts(scene);// 场景已加载，直接设置快捷键

            } catch (Exception e) {
                logger.error("设置快捷键失败", e);
                // 即使失败也尝试使用备选方案
                setupFallbackShortcuts();
            }
        });
    }

    private void setupShortcutsWithSceneListener() {
        // 监听场景属性的变化
        mainController.getIdeaTableView().sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null && !shortcutsConfigured.get()) {
                logger.info("场景已附加，设置快捷键...");
                setupSceneShortcuts(newScene);
            }
        });
        // 同时设置一个超时机制
        new Thread(() -> {
            try {
                // 等待最多3秒
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(100);
                    Scene scene = mainController.getIdeaTableView().getScene();
                    if (scene != null && !shortcutsConfigured.get()) {
                        runLater(() -> setupSceneShortcuts(scene));
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void setupSceneShortcuts(Scene scene) {
        if (shortcutsConfigured.get()) {
            logger.info("快捷键已配置，跳过重复配置");
            return;
        }
        try {
            logger.info("正在为场景添加快捷键事件过滤器");
            // 添加全局快捷键
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                // Ctrl+N: 新建灵感
                if (event.isControlDown() && event.getCode() == KeyCode.N && (!Objects.equals(activeArea, "mindmap"))) {
                    event.consume();
                    runLater(mainController::handleNewIdea);
                    logger.debug("快捷键: Ctrl+N - 新建灵感");
                }
                // Ctrl+F: 搜索
                else if (event.isControlDown() && event.getCode() == KeyCode.F && Objects.equals(activeArea, "table")) {
                    event.consume();
                    runLater(() -> {
                        if (mainController.getSearchField() != null) {
                            mainController.getSearchField().requestFocus();
                            mainController.getSearchField().selectAll();
                        }
                    });
                    logger.debug("快捷键: Ctrl+F - 搜索");
                }
                // Ctrl+S: 保存
                else if (event.isControlDown() && event.getCode() == KeyCode.S && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleSave);
                    logger.debug("快捷键: Ctrl+S - 保存");
                }
                // Ctrl+Q: 快速捕捉
                else if (event.isControlDown() && event.getCode() == KeyCode.Q) {
                    event.consume();
                    runLater(mainController::handleQuickCapture);
                    logger.debug("快捷键: Ctrl+Q - 快速捕捉");
                }
                // Esc: 取消搜索
                else if (event.getCode() == KeyCode.ESCAPE && (!Objects.equals(activeArea, "mindmap"))) {
                    if (mainController.getSearchField() != null &&
                            mainController.getSearchField().isFocused() &&
                            !mainController.getSearchField().getText().isEmpty()) {
                        event.consume();
                        runLater(() -> mainController.getSearchField().clear());
                        logger.debug("快捷键: Esc - 清除搜索");
                    }
                }
                // Ctrl+E: 编辑选中灵感
                else if (event.isControlDown() && event.getCode() == KeyCode.E && Objects.equals(activeArea, "table")) {
                    event.consume();
                    runLater(mainController::handleEditIdea);
                    logger.debug("快捷键: Ctrl+E - 编辑灵感");
                }
                // Delete: 删除选中灵感
                else if (event.getCode() == KeyCode.DELETE && Objects.equals(activeArea, "table")) {
                    event.consume();
                    runLater(mainController::handleDeleteIdea);
                    logger.debug("快捷键: Delete - 删除灵感");
                }
                // Ctrl+R: 刷新列表
                else if (event.isControlDown() && event.getCode() == KeyCode.R && Objects.equals(activeArea, "table")) {
                    event.consume();
                    runLater(() -> {
                        mainController.loadInitialData();
                        mainController.getStatusLabel().setText("列表已刷新");
                    });
                    logger.debug("快捷键: Ctrl+R - 刷新列表");
                }
                // Ctrl+B: 加粗
                else if (event.isControlDown() && event.getCode() == KeyCode.B  && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleBold);
                    logger.debug("快捷键: Ctrl+B - 加粗");
                }
                // Ctrl+I: 斜体
                else if (event.isControlDown() && event.getCode() == KeyCode.I  && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleItalic);
                    logger.debug("快捷键: Ctrl+I - 斜体");
                }
                // Ctrl+1: 一级标题
                else if (event.isControlDown() && event.getCode() == KeyCode.DIGIT1  && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleHeader1);
                    logger.debug("快捷键: Ctrl+1 - 一级标题");
                }
                // Ctrl+2: 二级标题
                else if (event.isControlDown() && event.getCode() == KeyCode.DIGIT2  && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleHeader2);
                    logger.debug("快捷键: Ctrl+2 - 二级标题");
                }
                // Ctrl+Shift+`: 插入代码块
                else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.BACK_QUOTE  && Objects.equals(activeArea, "editor")) {
                    event.consume();
                    runLater(mainController::handleCode);
                    logger.debug("快捷键: Ctrl+Shift+` - 插入代码块 ");
                }
            });
            shortcutsConfigured.set(true);
            logger.info("快捷键设置完成");
            // 更新状态栏
            runLater(() -> {if (mainController.getStatusLabel() != null) mainController.getStatusLabel().setText("快捷键已启用");});
        } catch (Exception e) {
            logger.error("设置场景快捷键失败", e);
            shortcutsConfigured.set(false);
        }
    }

    private void setupFallbackShortcuts() {
        logger.warn("使用备用方案设置快捷键");
        // 备用方案：使用轮询等待场景加载
        new Thread(() -> {
            try {
                for (int i = 0; i < 50; i++) { // 最多等待5秒
                    Thread.sleep(100);
                    runLater(() -> {
                        try {
                            Scene scene = mainController.getIdeaTableView().getScene();
                            if (scene != null && !shortcutsConfigured.get()) setupSceneShortcuts(scene);
                        } catch (Exception _) {}// 忽略此轮异常
                    });
                    if (shortcutsConfigured.get()) break;
                }

                if (!shortcutsConfigured.get()) {
                    logger.error("5秒后仍无法设置快捷键");
                    runLater(() -> {
                        if (mainController.getStatusLabel() != null)
                            mainController.getStatusLabel().setText("快捷键设置失败");
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // 清理快捷键
    public void cleanupShortcuts() {
        try {
            Scene scene = mainController.getIdeaTableView().getScene();
            if (scene != null) logger.info("已清理快捷键");
        } catch (Exception e) {logger.error("清理快捷键失败", e);}
        shortcutsConfigured.set(false);
    }
}