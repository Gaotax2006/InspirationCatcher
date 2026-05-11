package com.inspiration.catcher.controller;

import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Project;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.service.IdeaService;
import com.inspiration.catcher.manager.TagManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

public class QuickCaptureController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(QuickCaptureController.class);
    private final IdeaService ideaService = new IdeaService();
    private final TagManager tagManager = new TagManager();
    private final Set<String> tags = new HashSet<>();
    // 回调函数
    private Consumer<Idea> onSaveSuccessCallback;
    private Runnable onWindowClosedCallback;

    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<Integer> importanceCombo;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private TextField tagInput;
    @FXML private FlowPane tagContainer;
    @FXML private ComboBox<String> moodCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化快速捕捉控制器");
        try {
            // 设置类型选择
            typeCombo.getItems().clear();
            for (Idea.IdeaType type : Idea.IdeaType.values())
                typeCombo.getItems().add(type.getDisplayName());
            typeCombo.getSelectionModel().selectFirst();

            // 设置重要性选择
            ObservableList<Integer> importanceLevels = FXCollections.observableArrayList(1, 2, 3, 4, 5);
            importanceCombo.setItems(importanceLevels);
            importanceCombo.getSelectionModel().select(2); // 默认3级
            // 初始化心情下拉框
            if (moodCombo != null) {
                moodCombo.getItems().clear();
                for (Idea.Mood mood : Idea.Mood.values())
                    moodCombo.getItems().add(mood.getDisplayName());
                moodCombo.setValue("中性");
            }
            // 加载所有标签
            tagManager.loadAllTags();
            // 设置初始焦点
            Platform.runLater(() -> contentArea.requestFocus());
            logger.info("快速捕捉控制器初始化完成");
        } catch (Exception e) {
            logger.error("初始化快速捕捉控制器失败", e);
            showAlert("初始化错误", "界面初始化失败: " + e.getMessage());
        }
    }
    @FXML private void handleAddTag() {
        try {
            String tagText = tagInput.getText().trim();
            if (!tagText.isEmpty()) {
                // 支持中文逗号、英文逗号、空格分隔
                String[] newTags = tagText.split("[，,\\s]+");
                for (String tag : newTags) {
                    if (!tag.isEmpty() && !tags.contains(tag)) {
                        tags.add(tag);addTagToUI(tag);
                    }
                }
                tagInput.clear();
            }
        } catch (Exception e) {
            logger.error("添加标签失败", e);
            showAlert("添加标签失败", "错误: " + e.getMessage());
        }
    }
    private void addTagToUI(String tag) {
        HBox tagBox = new HBox(5);
        tagBox.setStyle("-fx-background-color: #E3F2FD; -fx-padding: 3 8; -fx-background-radius: 10;");
        Label tagLabel = new Label("#" + tag);
        tagLabel.setStyle("-fx-text-fill: #1565C0;");
        Button removeBtn = new Button("×");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-font-weight: bold;");
        removeBtn.setOnAction(_ -> {
            tags.remove(tag);
            tagContainer.getChildren().remove(tagBox);
        });
        tagBox.getChildren().addAll(tagLabel, removeBtn);
        tagContainer.getChildren().add(tagBox);
    }
    @FXML private void handleSave() {
        try {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            // 验证内容
            if (content.isEmpty()) {showAlert("错误", "内容不能为空");return;}
            // 创建新的灵感对象
            Idea idea = new Idea();
            idea.setTitle(title.isEmpty() ? "无标题灵感" : title);
            idea.setContent(content);
            // 设置当前项目ID
            idea.setProjectId(MainController.getCurrentProjectId());
            // 设置类型
            if (typeCombo.getValue() != null) {
                String typeStr = typeCombo.getValue();
                Idea.IdeaType type = Idea.IdeaType.IDEA; // 默认值
                for (Idea.IdeaType t : Idea.IdeaType.values())
                    if (t.getDisplayName().equals(typeStr)) {
                        type = t;
                        break;
                    }
                idea.setType(type);
            } else idea.setType(Idea.IdeaType.IDEA);
            // 设置心情
            if (moodCombo != null && moodCombo.getValue() != null) {
                String moodStr = moodCombo.getValue();
                Idea.Mood mood = Idea.Mood.NEUTRAL; // 默认值
                for (Idea.Mood m : Idea.Mood.values())
                    if (m.getDisplayName().equals(moodStr)) {mood = m;break;}
                idea.setMood(mood);
            } else idea.setMood(Idea.Mood.NEUTRAL);
            // 设置重要性
            idea.setImportance(importanceCombo.getValue() != null ? importanceCombo.getValue() : Integer.valueOf(3));
            idea.setPrivacy(Idea.PrivacyLevel.PRIVATE);
            // 设置时间戳
            LocalDateTime now = LocalDateTime.now();
            idea.setCreatedAt(now);
            idea.setUpdatedAt(now);
            // 处理标签 - 将UI中的标签添加到Idea对象
            if (!tags.isEmpty()) for (String tagName : tags) {
                Tag tag = tagManager.findOrCreateTag(tagName);
                if (tag != null) idea.addTag(tag);
            }
            // 保存到数据库
            Idea savedIdea = ideaService.saveIdea(idea);
            if (savedIdea == null || savedIdea.getId() == null) {
                showAlert("保存失败", "无法保存灵感到数据库");return;}
            logger.info("灵感已保存: ID={}, 标题={}", savedIdea.getId(), savedIdea.getTitle());
            // 保存标签关联到数据库
            saveTagsToDatabase(savedIdea);
            // 调用回调通知保存成功
            if (onSaveSuccessCallback != null) onSaveSuccessCallback.accept(savedIdea);
            // 关闭窗口
            closeWindow();

        } catch (Exception e) {
            logger.error("保存灵感失败", e);
            showAlert("保存失败", "错误: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    // 保存标签到数据库
    private void saveTagsToDatabase(Idea idea) {
        if (idea == null || idea.getId() == null || idea.getTags().isEmpty()) return;
        logger.info("保存标签到数据库: ideaId={}, 标签数={}", idea.getId(), idea.getTags().size());
        // 确保所有标签都保存到数据库并获取ID
        for (Tag tag : idea.getTags()) if (tag.getId() == null) {// 标签尚未保存到数据库
                Tag savedTag = tagManager.findOrCreateTag(tag.getName());
                if (savedTag != null && savedTag.getId() != null) tag.setId(savedTag.getId());
            }
        // 保存标签关联
        for (Tag tag : idea.getTags()) if (tag.getId() != null) {
                boolean success = tagManager.addTagToIdea(idea.getId(), tag.getId());
                if (!success) logger.warn("添加标签到灵感失败: ideaId={}, tagId={}", idea.getId(), tag.getId());
                else logger.info("标签关联保存成功: ideaId={}, tagId={}, tagName={}",
                        idea.getId(), tag.getId(), tag.getName());
            }
    }
    @FXML private void handleCancel() {closeWindow();}
    private void closeWindow() {
        try {
            // 触发窗口关闭回调
            if (onWindowClosedCallback != null) onWindowClosedCallback.run();
            // 关闭窗口
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.close();
            logger.info("快速捕捉窗口已关闭");
        } catch (Exception e) {logger.error("关闭窗口失败", e);}
    }
    // 设置回调方法
    public void setOnSaveSuccessCallback(Consumer<Idea> callback) {
        this.onSaveSuccessCallback = callback;
    }
    public void setOnWindowClosedCallback(Runnable callback) {
        this.onWindowClosedCallback = callback;
    }
    // 添加静态方法：显示快速捕捉窗口
    public static void showQuickCaptureWindow(MainController mainController) {
        logger.info("显示快速捕捉窗口");
        try {
            // 创建新的Stage
            Stage quickStage = new Stage();
            quickStage.setTitle("快速捕捉灵感");
            quickStage.initModality(Modality.WINDOW_MODAL);
            // 设置所有者窗口
            if (mainController != null && mainController.getIdeaTableView() != null)
                quickStage.initOwner(mainController.getIdeaTableView().getScene().getWindow());
            // 加载FXML
            URL fxmlUrl = QuickCaptureController.class.getResource("/QuickCaptureView.fxml");
            if (fxmlUrl == null) {
                logger.error("无法找到QuickCaptureView.fxml文件");
                showAlert("错误", "无法加载快速捕捉窗口界面文件");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            // 获取控制器
            QuickCaptureController controller = loader.getController();
            // 设置回调
            if (mainController != null) {
                controller.setOnSaveSuccessCallback(savedIdea -> {
                    // 确保设置项目ID
                    Project currentProject = mainController.getProjectManager().getCurrentProject();
                    if (currentProject != null && savedIdea.getProjectId() == null)
                        savedIdea.setProjectId(currentProject.getId());
                    // 刷新表格数据
                    Platform.runLater(() -> {
                        mainController.getIdeaManager().loadAllIdeas();
                        mainController.getIdeaTableView().refresh();
                        mainController.refreshTableData();
                        // 更新状态栏
                        if (mainController.getStatusLabel() != null)
                            mainController.getStatusLabel().setText("快速捕捉: 灵感已保存 - " + savedIdea.getTitle());
                    });
                });
                controller.setOnWindowClosedCallback(() -> {
                    // 窗口关闭时的处理
                    Platform.runLater(() -> {
                        if (mainController.getStatusLabel() != null)
                            mainController.getStatusLabel().setText("快速捕捉窗口已关闭");
                    });
                });
            }
            // 创建场景并显示
            Scene scene = new Scene(root, 600, 400);
            quickStage.setScene(scene);
            quickStage.show();
            logger.info("快速捕捉窗口已打开");
        } catch (Exception e) {
            logger.error("打开快速捕捉窗口失败", e);
            // 显示错误提示
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("无法打开快速捕捉窗口");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }
    // 辅助方法：显示警告框
    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}