package com.inspiration.catcher.controller;

import com.inspiration.catcher.manager.*;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Project;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.util.MarkdownUtil;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

import static com.inspiration.catcher.MainApp.getStackTrace;

public class EditorController {
    private static final Logger logger = LoggerFactory.getLogger(EditorController.class);

    private final TextField editorTitleField;
    private final ComboBox<String> editorTypeCombo;
    private final Spinner<Integer> importanceSpinner;
    private final ComboBox<String> moodCombo;
    private final TextField tagsInputField;
    private final TextArea markdownEditor;
    private final WebView markdownPreview;
    private final IdeaManager ideaManager;
    private final TableManager tableManager;
    private final ProjectManager projectManager;
    private final MainController mainController;
    private final TagManager tagManager = new TagManager();
    // 模式管理器
    private final EditorModeManager modeManager = new EditorModeManager();
    public EditorController(TextField editorTitleField,
                            ComboBox<String> editorTypeCombo,
                            Spinner<Integer> importanceSpinner,
                            ComboBox<String> moodCombo,
                            TextField tagsInputField,
                            TextArea markdownEditor,
                            WebView markdownPreview,
                            IdeaManager ideaManager,
                            TableManager tableManager,
                            ProjectManager projectManager,
                            MainController mainController) {
        this.editorTitleField = editorTitleField;
        this.editorTypeCombo = editorTypeCombo;
        this.importanceSpinner = importanceSpinner;
        this.moodCombo = moodCombo;
        this.tagsInputField = tagsInputField;
        this.markdownEditor = markdownEditor;
        this.markdownPreview = markdownPreview;
        this.ideaManager = ideaManager;
        this.tableManager = tableManager;
        this.projectManager = projectManager;
        this.mainController = mainController;
    }

    //初始化编辑器
    public void setupEditor() {
        logger.info("设置编辑器");
        // 初始化类型下拉框
        editorTypeCombo.getItems().clear();
        for (Idea.IdeaType type : Idea.IdeaType.values())
            editorTypeCombo.getItems().add(type.getDisplayName());
        editorTypeCombo.getSelectionModel().selectFirst();
        // 设置重要性选择器
        SpinnerValueFactory<Integer> importanceFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3);
        importanceSpinner.setValueFactory(importanceFactory);
        // 设置心情下拉框
        moodCombo.getItems().clear();
        for (Idea.Mood mood : Idea.Mood.values())
            moodCombo.getItems().add(mood.getDisplayName());
        moodCombo.setValue("中性");
        // 加载所有标签
        tagManager.loadAllTags();
        // 设置自动完成标签
        setupTagAutoComplete();
        // 默认设置为新建模式
        switchToNewMode();
    }
    // 添加自动完成标签的改进版本
    private void setupTagAutoComplete() {
        // 创建自动完成弹出框
        ContextMenu suggestionMenu = new ContextMenu();
        suggestionMenu.setAutoHide(true);
        // 监听文本变化
        tagsInputField.textProperty().addListener((_, _, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                suggestionMenu.hide();return;}
            // 获取最后一个单词
            String currentInput = newValue.trim();
            String lastWord = getLastTagWord(currentInput);
            if (lastWord.isEmpty()) {suggestionMenu.hide();return;}
            // 搜索匹配的标签
            List<Tag> matchingTags = tagManager.searchTags(lastWord);
            if (!matchingTags.isEmpty()) {
                suggestionMenu.getItems().clear();
                // 添加建议项
                for (Tag tag : matchingTags) {
                    MenuItem item = new MenuItem(tag.getName());
                    item.setOnAction(_ -> {
                        String currentText = tagsInputField.getText();
                        String[] parts = currentText.split("[，,\\s]+");
                        if (parts.length > 0) {
                            // 替换最后一个单词
                            String newText;
                            if (currentText.endsWith(" ")) {
                                newText = currentText + tag.getName() + " ";
                            } else if (currentText.endsWith(",") || currentText.endsWith("，")) {
                                newText = currentText + tag.getName() + " ";
                            } else {
                                int lastSeparatorIndex = Math.max(
                                        currentText.lastIndexOf(','),
                                        Math.max(
                                                currentText.lastIndexOf('，'),
                                                currentText.lastIndexOf(' ')
                                        )
                                );

                                if (lastSeparatorIndex == -1) {
                                    newText = tag.getName() + " ";
                                } else {
                                    newText = currentText.substring(0, lastSeparatorIndex + 1) +
                                            tag.getName() + " ";
                                }
                            }
                            tagsInputField.setText(newText);
                        }
                        suggestionMenu.hide();
                    });
                    suggestionMenu.getItems().add(item);
                }

                // 显示建议菜单
                if (!suggestionMenu.isShowing()) {
                    Bounds bounds = tagsInputField.localToScreen(tagsInputField.getBoundsInLocal());
                    suggestionMenu.show(tagsInputField, bounds.getMinX(), bounds.getMaxY());
                }
            } else {
                suggestionMenu.hide();
            }
        });

        // 当输入框失去焦点时隐藏菜单
        tagsInputField.focusedProperty().addListener((_, _, newVal) -> {
            if (!newVal) {
                suggestionMenu.hide();
            }
        });
    }

    // 获取最后一个标签单词
    private String getLastTagWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String trimmed = text.trim();
        // 查找最后一个分隔符（中英文逗号或空格）
        int lastSeparatorIndex = -1;
        for (int i = trimmed.length() - 1; i >= 0; i--) {
            char c = trimmed.charAt(i);
            if (c == ',' || c == '，' || c == ' ') {
                lastSeparatorIndex = i;
                break;
            }
        }

        if (lastSeparatorIndex == -1) {
            return trimmed;
        } else {
            return trimmed.substring(lastSeparatorIndex + 1);
        }
    }
    //设置事件处理器
    public void setupEventHandlers() {
        // Markdown 内容变化时更新预览
        markdownEditor.textProperty().addListener((_, _, _) -> updateMarkdownPreview());
        // 标题变化时自动保存草稿（可选）
        editorTitleField.textProperty().addListener((_, _, _) -> {
            // 这里可以添加自动保存草稿的逻辑
        });
    }

    //切换到新建模式
    public void switchToNewMode() {
        logger.info("切换到新建模式");
        // 清空编辑器
        clearEditor();
        // 更新模式状态
        modeManager.switchToNewMode();
        // 更新状态栏
        updateStatusBar("新建灵感 - 请输入内容");
    }

    //切换到编辑模式
    public void switchToEditMode(Idea idea) {
        if (idea == null) {
            logger.error("无法切换到编辑模式：idea为null");
            return;
        }

        logger.info("切换到编辑模式：ID={}, Title={}", idea.getId(), idea.getTitle());
        // 填充编辑器
        loadIdeaToEditor(idea);
        // 设置重要性
        importanceSpinner.getValueFactory().setValue(idea.getImportance());
        // 设置心情
        moodCombo.setValue(idea.getMood().getDisplayName());
        // 设置标签
        tagsInputField.setText(idea.getTagsString());
        // 更新模式状态
        modeManager.switchToEditMode(idea);
        // 更新详情面板
        tableManager.showIdeaDetail(idea);
        // 更新状态栏
        updateStatusBar(String.format("编辑模式 - %s", idea.getTitle()));
    }

    //加载灵感到编辑器
    private void loadIdeaToEditor(Idea idea) {
        // 设置标题
        editorTitleField.setText(idea.getTitle());
        // 设置内容
        markdownEditor.setText(idea.getContent());
        // 设置类型
        editorTypeCombo.getSelectionModel().select(idea.getType().getDisplayName());
        // 设置重要性
        if (importanceSpinner != null && idea.getImportance() != null)
            importanceSpinner.getValueFactory().setValue(idea.getImportance());
        // 设置心情
        if (moodCombo != null && idea.getMood() != null) moodCombo.setValue(idea.getMood().getDisplayName());
        // 设置标签 - 修复显示格式
        if (tagsInputField != null) {
            String tagsText = "";
            if (idea.getTags() != null && !idea.getTags().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Tag tag : idea.getTags()) {
                    if (tag != null && tag.getName() != null) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(tag.getName());
                    }
                }
                tagsText = sb.toString();
            }
            tagsInputField.setText(tagsText);
        }
        // 更新 Markdown预览
        updateMarkdownPreview();
    }

    public void saveCurrentIdea() {
        try {
            // 验证输入
            if (!validateInput()) return;
            // 获取编辑器内容
            String title = editorTitleField.getText().trim();
            String content = markdownEditor.getText().trim();
            String typeStr = editorTypeCombo.getValue();
            Idea.IdeaType type = Idea.IdeaType.IDEA; // 默认值
            for (Idea.IdeaType t : Idea.IdeaType.values())
                if (t.getDisplayName().equals(typeStr)) {type = t;break;}
            // 处理标题
            if (title.isEmpty()) {
                title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                editorTitleField.setText(title); // 更新 UI
            }
            Idea savedIdea = modeManager.isNewMode() ? saveNewIdea(title, content, type) : updateExistingIdea(title, content, type);
            if (savedIdea == null) {
                // 显示详细的错误对话框
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("保存失败");
                    alert.setHeaderText("无法保存灵感到数据库");

                    TextArea textArea = new TextArea();
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setText("""
                            保存失败详情：
                            请查看控制台输出获取更多信息。
                            
                            常见问题：
                            1. 数据库连接失败
                            2. 数据库表不存在
                            3. 数据格式错误
                            4. 字段值不符合约束
                            
                            建议操作：
                            1. 查看控制台日志
                            2. 检查数据库文件是否存在
                            3. 重启应用程序""");

                    alert.getDialogPane().setExpandableContent(textArea);
                    alert.getDialogPane().setExpanded(true);
                    alert.showAndWait();
                });
                return;
            }
            // 保存成功后处理
            handleSaveSuccess(savedIdea);

        } catch (Exception e) {
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("保存异常");
                alert.setHeaderText("保存过程中发生异常");
                // 显示详细信息
                TextArea textArea = new TextArea();
                textArea.setText("异常类型: " + e.getClass().getName() + "\n" +
                        "异常信息: " + e.getMessage() + "\n\n" +
                        "堆栈跟踪:\n" + getStackTrace(e));
                textArea.setEditable(false);
                textArea.setWrapText(true);

                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
            });
        }
    }

    private Idea saveNewIdea(String title, String content, Idea.IdeaType type) {
        // 查重（弃用）
//        boolean exists = ideaManager.ideaExists(title, content);
//        if (exists) {
//            Platform.runLater(() -> showAlert("相似的灵感已存在，请检查灵感列表"));
//            return null;
//        }
        // 创建新灵感对象
        Idea newIdea = new Idea(title, content, type);
        // 设置当前项目ID
        Project currentProject = projectManager.getCurrentProject();
        if (currentProject != null && currentProject.getId() != null) {
            newIdea.setProjectId(currentProject.getId());
        } else {
            newIdea.setProjectId(ProjectManager.DEFAULT_PROJECT_ID);
        }
        // 设置重要性
        newIdea.setImportance(importanceSpinner.getValue());
        // 设置心情
        String moodStr = moodCombo.getValue();
        Idea.Mood mood = Idea.Mood.NEUTRAL; // 默认值
        for (Idea.Mood m : Idea.Mood.values())
            if (m.getDisplayName().equals(moodStr)) {
                mood = m;
                break;
            }
        newIdea.setMood(mood);
        // 处理标签
        String tagsText = tagsInputField.getText().trim();

        if (!tagsText.isEmpty()) {
            // 支持中文逗号、英文逗号、空格分隔
            String[] tagNames = tagsText.split("[，,\\s]+");

            for (String name : tagNames) {
                String tagName = name.trim();
                if (!tagName.isEmpty()) {
                    Tag tag = tagManager.findOrCreateTag(tagName);
                    if (tag != null && tag.getId() != null) newIdea.addTag(tag);
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        newIdea.setCreatedAt(now);
        newIdea.setUpdatedAt(now);

        // 保存到数据库
        Idea savedIdea = ideaManager.saveIdea(newIdea);

        if (savedIdea == null || savedIdea.getId() == null) {
            logger.error("新建灵感保存失败");
            return null;
        }

        // 保存标签关联到数据库
        saveTagsToDatabase(savedIdea);
        logger.info("新灵感创建成功: ID={}", savedIdea.getId());
        return savedIdea;
    }

    // 保存标签到数据库
    private void saveTagsToDatabase(Idea idea) {
        if (idea == null || idea.getId() == null || idea.getTags().isEmpty()) return;
        logger.info("保存标签到数据库: ideaId={}, 标签数={}", idea.getId(), idea.getTags().size());
        // 确保所有标签都保存到数据库
        for (Tag tag : idea.getTags()) {
            if (tag.getId() == null) {
                // 标签尚未保存到数据库
                Tag savedTag = tagManager.findOrCreateTag(tag.getName());
                if (savedTag != null && savedTag.getId() != null) tag.setId(savedTag.getId());
            }
        }
        // 保存标签关联
        for (Tag tag : idea.getTags()) {
            if (tag.getId() != null) {
                boolean success = tagManager.addTagToIdea(idea.getId(), tag.getId());
                if (!success) {
                    logger.warn("添加标签到灵感失败: ideaId={}, tagId={}", idea.getId(), tag.getId());
                }
            }
        }
        logger.info("标签保存完成");
    }

    // 更新已有灵感
    private Idea updateExistingIdea(String title, String content, Idea.IdeaType type) {
        Idea currentIdea = modeManager.getCurrentIdea();
        if (currentIdea == null || currentIdea.getId() == null) {
            logger.error("更新失败：当前编辑的灵感无效");
            return null;
        }
        logger.info("更新已有灵感: ID={}", currentIdea.getId());

        // 检查标签是否变化
        String tagsText = tagsInputField.getText().trim();
        String currentTags = currentIdea.getTagsString();
        boolean tagsChanged = !tagsText.equals(currentTags);

        // 更新灵感对象
        currentIdea.setTitle(title);
        currentIdea.setContent(content);
        currentIdea.setType(type);
        currentIdea.setImportance(importanceSpinner.getValue());
        String moodStr = moodCombo.getValue();
        Idea.Mood mood = Idea.Mood.NEUTRAL; // 默认值
        for (Idea.Mood m : Idea.Mood.values())
            if (m.getDisplayName().equals(moodStr)) {
                mood = m;break;}
        currentIdea.setMood(mood);
        currentIdea.setUpdatedAt(LocalDateTime.now());

        // 更新标签
        if (tagsChanged) {
            currentIdea.getTags().clear(); // 清空旧标签
            if (!tagsText.isEmpty()) {
                String[] tagNames = tagsText.split("[，,\\s]+");
                for (String tagName : tagNames) if (!tagName.trim().isEmpty()) {
                        Tag tag = tagManager.findOrCreateTag(tagName.trim());
                        if (tag != null) currentIdea.addTag(tag);
                }
            }
        }
        // 保存到数据库
        Idea savedIdea = ideaManager.saveIdea(currentIdea);
        if (savedIdea == null) {logger.error("更新灵感失败");return null;}
        // 保存标签关联到数据库
        if (tagsChanged) saveTagsToDatabase(savedIdea);
        logger.info("灵感更新成功: ID={}", savedIdea.getId());
        return savedIdea;
    }
    // 保存成功后的处理
    private void handleSaveSuccess(Idea savedIdea) {
        logger.info("保存成功处理: ID={}", savedIdea.getId());
        Platform.runLater(() -> {
            // 更新模式管理器
            if (modeManager.isNewMode()) modeManager.afterSave(savedIdea);
            // 刷新表格
            mainController.refreshTableData();
            // 选中并显示保存的灵感
            tableManager.selectAndShowIdea(savedIdea);
            // 更新状态栏
            updateStatusBar(String.format("保存成功: %s", savedIdea.getTitle()));
            // 显示成功提示
//            showSuccessAlert("保存成功", "灵感已保存到数据库");
        });
    }

    // 验证输入
    private boolean validateInput() {
        String content = markdownEditor.getText().trim();
        if (content.isEmpty()) {showAlert("内容不能为空");return false;}
        return true;
    }

    //清空编辑器
    private void clearEditor() {
        // 清空标题
        editorTitleField.clear();
        // 清空内容
        markdownEditor.clear();
        // 重置类型为默认值
        editorTypeCombo.getSelectionModel().selectFirst();
        // 重置重要性为默认值 (3)
        importanceSpinner.getValueFactory().setValue(3);
        // 重置心情为默认值 (NEUTRAL)
        moodCombo.setValue("中性");
        // 清空标签输入框
        tagsInputField.clear();
        // 清空预览
        markdownPreview.getEngine().loadContent("");
    }
    // 更新状态栏
    private void updateStatusBar(String message) {
        if (mainController.getStatusLabel() != null) mainController.getStatusLabel().setText(message);
    }
    // 更新 Markdown预览
    public void updateMarkdownPreview() {
        String markdown = markdownEditor.getText();
        String html = MarkdownUtil.markdownToHtml(markdown);
        try {
            // 保存当前的滚动位置
            double currentScrollTop = 0;
            try {
                Object result = markdownPreview.getEngine().executeScript("window.scrollY || document.documentElement.scrollTop");
                if (result instanceof Number) currentScrollTop = ((Number) result).doubleValue();
            } catch (Exception _) {}
            // 标记当前是否需要恢复滚动位置
            final double scrollPositionToRestore = currentScrollTop;
            // 设置一个监听器，在页面加载完成后恢复滚动位置
            markdownPreview.getEngine().getLoadWorker().stateProperty().addListener(
                    (_, _, newState) -> {
                        // 延迟一小段时间确保页面完全渲染
                        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) Platform.runLater(() -> {
                            try {
                                if (scrollPositionToRestore > 0) markdownPreview.getEngine().executeScript(
                                        String.format("window.scrollTo(0, %f);", scrollPositionToRestore)
                                );
                            } catch (Exception _) {
                            } // 忽略恢复滚动位置的错误
                        });
                    }
            );
            // 加载新内容
            markdownPreview.getEngine().loadContent(html);
        } catch (Exception e) {
            e.printStackTrace();
            markdownPreview.getEngine().loadContent("<pre>" + markdown + "</pre>");
        }
    }

    private void showAlert(String message) {
        Platform.runLater(() -> new StatusManager().showAlert(message));
    }
}