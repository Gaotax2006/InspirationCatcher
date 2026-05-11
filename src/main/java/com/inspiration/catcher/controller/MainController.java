package com.inspiration.catcher.controller;

import com.inspiration.catcher.component.MindMapView;
import com.inspiration.catcher.config.AIConfig;
import com.inspiration.catcher.manager.FontManager;
import com.inspiration.catcher.manager.ShortcutManager;
import com.inspiration.catcher.manager.TableManager;
import com.inspiration.catcher.manager.*;
import com.inspiration.catcher.model.*;
import com.inspiration.catcher.service.AIService;
import com.inspiration.catcher.util.MarkdownUtil;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static MainController instance; // 添加静态实例
    // 管理器实例
    private final ProjectManager projectManager = new ProjectManager();
    private final IdeaManager ideaManager = new IdeaManager(projectManager);
    private final StatusManager statusManager = new StatusManager();
    private final UIConfigManager configManager = new UIConfigManager();
    private ShortcutManager shortcutManager;
    private MindMapManager mindMapManager;
    private FontManager fontManager;
    private TableManager tableManager;
    // 控制器实例
    private FilterController filterController;
    private EditorController editorController;
    private MindMapView mindMapView;
    private final AIService aiService=new AIService();

    private boolean isAIGenerating = false;
    private Node aiButtonContainer = null;  // 存储按钮容器的引用
    private final Object aiLock = new Object();  // 用于同步状态检查
    // FXML 注入的 UI 组件
    @FXML private ComboBox<Project> projectSelector;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<Integer> importanceFilter;
    @FXML private ComboBox<String> moodFilter;
    @FXML private DatePicker startDateFilter;
    @FXML private DatePicker endDateFilter;
    @FXML private FlowPane tagCloud;
    @FXML private TabPane mainTabPane;
    @FXML public TableView<Idea> ideaTableView;
    @FXML private TextField editorTitleField;
    @FXML private ComboBox<String> editorTypeCombo;
    @FXML public TextArea markdownEditor;
    @FXML private WebView markdownPreview;
    @FXML public TextArea ideaDetail;
    @FXML public TextArea aiSuggestions;
    @FXML private ListView<Idea> relatedIdeasList;
    @FXML private Label totalIdeasLabel;
    @FXML private Label weeklyIdeasLabel;
    @FXML private Label topTagLabel;
    @FXML public Label statusLabel;
    @FXML private Pane mindMapPane;
    @FXML private ComboBox<String> tagFilter;
    @FXML private Spinner<Integer> editorImportanceSpinner;
    @FXML private ComboBox<String> editorMoodCombo;
    @FXML private TextField editorTagsField;
    @FXML private VBox mindMapIdeaListContainer;
    // 表格列（保留引用供其他管理器使用）
    @FXML private TableColumn<Idea, String> titleColumn;
    @FXML private TableColumn<Idea, String> typeColumn;
    @FXML private TableColumn<Idea, Integer> importanceColumn;
    @FXML private TableColumn<Idea, String> moodColumn;
    @FXML private TableColumn<Idea, String> tagsColumn;
    @FXML private TableColumn<Idea, String> createdAtColumn;
    @FXML private TableColumn<Idea, String> updatedAtColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        logger.info("初始化主控制器");
        try {
            // 初始化项目选择器
            initializeProjectSelector();
            // 初始化管理器
            initializeManagers();
            // 加载配置
            configManager.loadConfig();
            // 设置表格
            tableManager.setupTable();
            // 加载数据到表格
            tableManager.loadDataToTable();
            // 设置编辑器
            editorController.setupEditor();
            // 设置字体
            fontManager.updateAllFonts(this);
            // 加载数据
            loadProjectData(projectManager.getCurrentProject());
            // 初始化思维导图
            initializeMindMap();
            // 监听当前项目变化，重新加载数据
            projectManager.currentProjectProperty().addListener((_, _, newProject) -> {
                if (newProject != null) {loadProjectData(newProject);tableManager.loadDataToTable();}
            });
            // 加载当前项目数据
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject != null) loadProjectData(currentProject);
            else {
                logger.error("当前项目为 null，使用默认项目");
                Project defaultProject = projectManager.findDefaultProject();
                if (defaultProject != null) {
                    projectManager.setCurrentProject(defaultProject);
                    loadProjectData(defaultProject);
                }
            }
            // 设置事件处理器
            setupEventHandlers();
            // 更新热门标签
            updateHotTags();
            // 更新统计信息
            updateStatistics();
            // 设置只读属性
            ideaDetail.setEditable(false);
            aiSuggestions.setEditable(false);
            relatedIdeasList.setEditable(false);
            // 使用Platform.runLater延迟设置快捷键
            Platform.runLater(() -> {
                logger.info("延迟设置快捷键");
                try {// 给UI 一点时间完全加载
                    Thread.sleep(300);
                    shortcutManager.setupShortcuts();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("延迟设置快捷键失败", e);
                }
            });
            // 监听标签页切换，更新活动区域
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> {
                if (newTab != null) switch (newTab.getText()) {
                    case "灵感列表" -> shortcutManager.setActiveArea("table");
                    case "思维导图" -> shortcutManager.setActiveArea("mindmap");
                    case "编辑器" -> shortcutManager.setActiveArea("editor");
                }
            });
            filterController.updateFilterPredicate();
            logger.info("主控制器初始化完成");
        } catch (Exception e) {
            logger.error("初始化失败", e);
            statusLabel.setText("初始化失败: " + e.getMessage());
            showErrorDialog("初始化错误", e.getMessage());
        }
    }
    private void initializeProjectSelector() {
        if (projectSelector != null) {
            // 直接设置项目列表，不创建自定义选择器
            projectSelector.setItems(projectManager.getProjectList());
            // 设置当前项目
            Project current = projectManager.getCurrentProject();
            if (current != null) projectSelector.setValue(current);
            // 监听选择变化
            projectSelector.valueProperty().addListener((_, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) projectManager.switchToProject(newValue);
            });
        }
    }
    private void loadProjectData(Project project) {
        if (project == null) return;
        logger.info("加载项目数据: {}", project.getName());
        // 加载项目灵感
        ideaManager.loadAllIdeas();
        // 加载思维导图侧边栏
        if (mindMapView != null) loadIdeasToMindMapPanel(project);
        // 设置当前项目到思维导图管理器
        if (mindMapManager != null) {
            mindMapManager.setCurrentProject(project);
            // 更新思维导图视图
            if (mindMapView != null) mindMapView.setCurrentProject(project.getId());
        }
        // 刷新过滤器的数据源
        if (filterController != null) {
            filterController.refreshDataSource();
            filterController.refreshTagOptions();
        }
        // 刷新表格
        if (tableManager != null)tableManager.loadDataToTable();
        // 加载思维导图数据
        if (mindMapManager != null) {mindMapManager.setCurrentProject(project);refreshMindMap();}
        // 更新过滤器
        if (filterController != null) filterController.updateFilterPredicate();
        // 更新状态栏
        if (statusLabel != null) statusLabel.setText("当前项目: " + project.getName() + " - " + ideaManager.getIdeaCount() + " 条灵感");
        // 更新统计信息
        updateStatistics();
        // 立即刷新表格
        ideaTableView.refresh();
    }
    private void initializeManagers() {
        logger.info("初始化管理器");
        try {
            // 先确保项目管理器有当前项目
            if (projectManager.getCurrentProject() == null) {
                logger.warn("当前项目为null，设置为默认项目");
                projectManager.setCurrentProject(projectManager.findDefaultProject());
            }
            // 然后加载数据
            ideaManager.loadAllIdeas();
            // 初始化思维导图管理器
            mindMapManager = new MindMapManager(ideaManager);
            // 创建过滤器控制器（需要先有数据）
            filterController = new FilterController(
                    searchField, typeFilter, importanceFilter,
                    startDateFilter, endDateFilter, tagFilter, moodFilter, ideaManager, this
            );
            filterController.setupFilters();
            // 创建表格管理器
            tableManager = new TableManager(ideaTableView, ideaManager, this);
            // 关键：设置过滤列表到表格管理器
            tableManager.setFilteredIdeas(filterController.getFilteredIdeas());
            // 传递 UI 组件引用
            tableManager.setTableColumns(titleColumn, typeColumn, importanceColumn, moodColumn,
                    tagsColumn, createdAtColumn, updatedAtColumn);
            tableManager.setDetailPanels(ideaDetail);
            tableManager.setStatusLabels(statusLabel);
            // 初始化其他控制器
            editorController = new EditorController(
                    editorTitleField, editorTypeCombo, editorImportanceSpinner, editorMoodCombo, editorTagsField,
                    markdownEditor, markdownPreview, ideaManager, tableManager ,projectManager,this
            );
            fontManager = new FontManager(configManager, this);
            shortcutManager = new ShortcutManager(this);
            // 设置表格
            tableManager.setupTable();
            // 加载数据到表格
            tableManager.loadDataToTable();
            // 设置思维导图的管理器
            if (mindMapView != null) mindMapView.setMindMapManager(mindMapManager);
            logger.info("所有管理器初始化完成");
        } catch (Exception e) {
            logger.error("初始化管理器失败", e);
            e.printStackTrace();
            statusLabel.setText("初始化失败: " + e.getMessage());
        }
    }
    public void loadInitialData() {
        try {
            resetAllFilters();
            ideaManager.loadAllIdeas();
            statusLabel.setText("数据加载完成，共 " + ideaManager.getIdeaCount() + " 条灵感");
        } catch (Exception e) {
            logger.error("加载数据失败", e);
            statusLabel.setText("数据加载失败");
        }
    }
    private void setupEventHandlers() {
        // 表格选择监听
        ideaTableView.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newValue) -> {
                    if (newValue != null) tableManager.showIdeaDetail(newValue);});
        // 其他事件委托给相应的控制器
        filterController.setupEventHandlers();
        editorController.setupEventHandlers();
    }
    public void updateStatistics() {
        try {
            int total = ideaManager.getTotalCount();
            int filtered = tableManager.getFilteredCount();
            int weekly = ideaManager.getWeeklyCount();
            String topTag = ideaManager.getTopTag();
            totalIdeasLabel.setText("灵感总数: " + total);
            weeklyIdeasLabel.setText("本周新增: " + weekly);
            topTagLabel.setText("最活跃标签: " + topTag);
            if (mindMapManager != null) {// 思维导图统计信息
                Map<String, Integer> mindMapStats = mindMapManager.getStatistics();
                int nodeCount = mindMapStats.getOrDefault("nodeCount", 0);
                int connectionCount = mindMapStats.getOrDefault("connectionCount", 0);
                // 可以在状态栏或统计面板显示这些信息
            }
            // 更新状态栏显示过滤状态
            statusLabel.setText(filtered == total ? "显示所有 " + total + " 条灵感" : "显示 " + filtered + " 条（共 " + total + " 条）");
        } catch (Exception e) {logger.error("更新统计信息失败", e);}
    }
    // 公共方法供其他控制器调用
    public TableView<Idea> getIdeaTableView() { return ideaTableView; }
    public Label getStatusLabel() { return statusLabel; }
    public IdeaManager getIdeaManager() { return ideaManager; }
    public ProjectManager getProjectManager() { return projectManager; }
    public TextField getSearchField() { return searchField; }
    public Project getCurrentProject() {return projectManager.getCurrentProject();}
    // 添加静态方法获取当前项目
    public static Integer getCurrentProjectId() {
        if (instance != null) {
            Project currentProject = instance.projectManager.getCurrentProject();
            return currentProject != null ? currentProject.getId() : 1;
        }
        return 1;
    }
    // 添加刷新表格数据的方法
    public void refreshTableData() {
        try {
            logger.info("刷新表格数据");
            // 重新加载数据
            ideaManager.loadAllIdeas();
            // 重新设置表格数据
            FilteredList<Idea> filteredList = filterController.getFilteredIdeas();
            // 清空并重新填充过滤列表
            if (filteredList != null) filterController.updateFilterPredicate();
            // 更新表格
            if (ideaTableView != null) {
                ideaTableView.refresh();
                ideaTableView.getSelectionModel().clearSelection();
            }
            // 更新统计信息
            updateStatistics();
            // 更新热门标签
            updateHotTags();
            // 刷新思维导图左侧面板
            if (mindMapView != null) {
                Project currentProject = projectManager.getCurrentProject();
                if (currentProject != null) loadIdeasToMindMapPanel(currentProject);
            }
            logger.info("表格数据刷新完成");
        } catch (Exception e) {
            logger.error("刷新表格数据失败", e);
            statusLabel.setText("刷新失败: " + e.getMessage());
        }
    }
    // 私有辅助方法
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // 显示热门标签
    private void updateHotTags() {
        if (tagCloud == null) return;
        tagCloud.getChildren().clear();
        // 获取热门标签
        TagManager tagManager = new TagManager();
        tagManager.loadAllTags();
        List<Tag> hotTags = tagManager.getHotTags(10);
        for (Tag tag : hotTags) {
            Label tagLabel = new Label("#" + tag.getName());
            tagLabel.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 3 8; " +
                            "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 12px;",
                    tag.getColor() != null ? tag.getColor() : "#4A90E2"
            ));
            // 点击标签时自动筛选
            tagLabel.setOnMouseClicked(_ -> {
                tagFilter.setValue(tag.getName());
                updateFilterPredicate();
            });
            tagCloud.getChildren().add(tagLabel);
        }
        // 如果没有标签，显示提示
        if (hotTags.isEmpty()) {
            Label emptyLabel = new Label("暂无标签");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            tagCloud.getChildren().add(emptyLabel);
        }
    }
    // 事件处理方法（供FXML调用）
    @FXML private void handleNewProject() {
        // 创建一个简单的对话框让用户输入项目名称
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新建项目");
        dialog.setHeaderText("创建新项目");
        dialog.setContentText("请输入项目名称:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(projectName -> {
            Project newProject = projectManager.createProject(projectName, "", "#36B37E");
            if (newProject != null) {
                // 切换到新项目
                projectManager.switchToProject(newProject);
                statusLabel.setText("已创建并切换到新项目: " + projectName);
            }
        });
    }
    @FXML public void handleNewIdea() {
        ideaDetail.clear();
        aiSuggestions.clear();
        // 切换到编辑器标签页
        mainTabPane.getSelectionModel().select(2);
        // 切换到新建模式
        editorController.switchToNewMode();
    }
    @FXML public void handleQuickCapture() {
        logger.info("打开快速捕捉窗口");
        // 显示快速捕捉窗口
        QuickCaptureController.showQuickCaptureWindow(this);
    }
    @FXML public void handleEditIdea() {
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            logger.info("编辑选中灵感: {}", selected.getTitle());
            // 切换到编辑器标签页
            mainTabPane.getSelectionModel().select(2);
            // 切换到编辑模式
            editorController.switchToEditMode(selected);
        } else {
            statusManager.showAlert("请先选择一个灵感");
        }
    }
    @FXML public void handleDeleteIdea() {
        Idea selected = tableManager.getSelectedIdea(); // 改为从TableManager 获取
        if (selected != null) {
            if (statusManager.showConfirmDialog("确认删除", "删除灵感",
                    "确定要删除这个灵感吗？此操作不可撤销。")) {
                try {
                    ideaManager.deleteIdea(selected);
                    tableManager.refreshTable(); // 刷新表格
                    statusLabel.setText("灵感已删除");
                    updateStatistics();
                    updateHotTags();
                } catch (Exception e) {
                    statusManager.showError("删除失败", e.getMessage());
                }
            }
        }
    }
    @FXML private void generateAISuggestions() {
        // 使用同步锁防止并发访问
        synchronized (aiLock) {
            if (isAIGenerating) {statusManager.showAlert("AI正在思考中，请稍后再试");return;}
            isAIGenerating = true;
        }
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 显示加载状态
            statusLabel.setText("🤖 AI思考中...");
            aiSuggestions.setText("正在生成AI建议，请稍候...");
            // 禁用AI按钮
            disableAIButtons();
            // 异步调用
            ideaManager.generateAISuggestions(selected)
                    .thenAccept(suggestions -> Platform.runLater(() -> {
                        aiSuggestions.setText(suggestions);
                        statusLabel.setText("AI建议生成完成");
                        // 启用AI按钮并添加插入按钮
                        enableAIButtons();
                        addInsertButtons();
                        // 重置状态
                        synchronized (aiLock) {
                            isAIGenerating = false;
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            aiSuggestions.setText("AI建议生成失败: " + e.getMessage());
                            statusLabel.setText("AI生成失败");
                            // 启用AI按钮
                            enableAIButtons();
                            // 重置状态
                            synchronized (aiLock) {
                                isAIGenerating = false;
                            }
                        });
                        return null;
                    });
        } else {
            statusManager.showAlert("请先选择一个灵感");
            // 重置状态
            synchronized (aiLock) {
                isAIGenerating = false;
            }
        }
    }
    // 添加插入按钮到AI建议区域
    private void addInsertButtons() {
        VBox aiContainer = (VBox) aiSuggestions.getParent();
        // 检查是否已经有按钮容器
        if (aiButtonContainer != null && aiContainer.getChildren().contains(aiButtonContainer)) {
            // 按钮已存在，只需要确保它们可见和启用
            for (Node node : ((HBox) aiButtonContainer).getChildren()) {
                if (node instanceof Button) {
                    node.setVisible(true);
                    node.setDisable(false);
                }
            }
            return;  // 不再添加新的按钮
        }
        // 创建新的按钮容器
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        Button insertToEditor = new Button("📝 插入到编辑器");
        insertToEditor.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertToEditor.setOnAction(_ -> insertAIToEditor());
        Button copyToClipboard = new Button("📋 复制建议");
        copyToClipboard.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        copyToClipboard.setOnAction(_ -> copyAIToClipboard());
        buttonBox.getChildren().addAll(insertToEditor, copyToClipboard);
        // 保存引用
        aiButtonContainer = buttonBox;
        // 添加到AI建议区域
        aiContainer.getChildren().add(buttonBox);
    }
    // 插入AI建议到编辑器
    private void insertAIToEditor() {
        String aiContent = aiSuggestions.getText();
        if (aiContent != null && !aiContent.trim().isEmpty()) {
            String currentContent = markdownEditor.getText();
            String separator = "\n\n---\n\n## AI扩展建议\n\n";
            markdownEditor.setText(currentContent + separator + aiContent + "\n\n---\n\n");
            // 切换到编辑器标签页
            mainTabPane.getSelectionModel().select(2);
            statusLabel.setText("AI建议已插入编辑器");
            handleSave();
        }
    }
    // 复制到剪贴板
    private void copyAIToClipboard() {
        String aiContent = aiSuggestions.getText();
        if (aiContent != null && !aiContent.trim().isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(aiContent);
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("AI建议已复制到剪贴板");
        }
    }
    @FXML private void handleAISettings() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI设置");
        dialog.setHeaderText("配置AI API密钥");
        // 创建输入字段
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("输入DeepSeek API密钥");
        apiKeyField.setText(AIConfig.getApiKey());
        Label infoLabel = new Label("获取API密钥：https://platform.deepseek.com/api_keys");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(new Label("API密钥:"), 0, 0);
        grid.add(apiKeyField, 1, 0);
        grid.add(infoLabel, 1, 1);
        dialog.getDialogPane().setContent(grid);
        // 添加按钮
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // 处理结果
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) return apiKeyField.getText();
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(apiKey -> {
            // 保存到配置文件
            saveApiKey(apiKey);
            statusLabel.setText("API密钥已保存");
        });
    }
    private void saveApiKey(String apiKey) {
        try {
            Properties props = new Properties();
            props.setProperty("ai.api.key", apiKey);
            // 保存到用户目录
            File configFile = new File(System.getProperty("user.home") +
                    "/.inspiration-catcher/ai.properties");
            configFile.getParentFile().mkdirs();
            try (OutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "AI Configuration");
            }
        } catch (IOException e) {
            logger.error("保存API密钥失败", e);
            statusManager.showAlert("保存失败\n\n无法保存API密钥: " + e.getMessage());
        }
    }
    @FXML private void testAIConnection() {
        statusLabel.setText("测试AI连接...");
        new Thread(() -> Platform.runLater(() -> statusLabel.setText(aiService.testConnection() ? "✅ AI连接正常" : "❌ AI连接失败，请检查API密钥"))).start();
    }
    @FXML private void generateAIExpansion() {
        // 使用同步锁防止并发访问
        synchronized (aiLock) {
            if (isAIGenerating) {
                statusManager.showAlert("AI正在思考中，请稍后再试");
                return;
            }
            isAIGenerating = true;
        }
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        // 第一阶段：生成并编辑提示词
        if (selected != null) generateAndEditPrompt(selected);
        else {
            statusManager.showAlert("请先选择一个灵感");
            // 重置状态
            synchronized (aiLock) {isAIGenerating = false;}
        }
    }

    // 第一阶段：生成并编辑提示词
    private void generateAndEditPrompt(Idea idea) {
        statusLabel.setText("🤖 正在分析灵感特点，生成个性化提示词...");
        // 禁用AI按钮
        disableAIButtons();
        aiService.generateCustomPrompt(idea)
                .thenAccept(customPrompt -> Platform.runLater(() -> {
                    // 显示提示词编辑对话框
                    showPromptEditor(idea, customPrompt);
                    // 启用AI按钮
                    enableAIButtons();
                }))
                .exceptionally(_ -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("提示词生成失败");
                        statusManager.showAlert("提示词生成失败\n" + "使用默认模板继续？");
                        // 使用默认提示词继续
                        showPromptEditor(idea, getDefaultPrompt(idea));
                        // 启用AI按钮
                        enableAIButtons();
                    });
                    return null;
                });
    }

    // 显示提示词编辑器
    private void showPromptEditor(Idea idea, String initialPrompt) {
        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("定制AI提示词");
        dialog.setHeaderText("已根据您的灵感生成个性化提示词，可修改后发送给AI");
        logger.info("个性化提示词: {}",initialPrompt);
        // 创建文本区域
        TextArea promptEditor = new TextArea(initialPrompt);
        promptEditor.setWrapText(true);
        promptEditor.setPrefSize(600, 400);
        promptEditor.setStyle("-fx-font-family: 'Monaco', 'Consolas', monospace; -fx-font-size: 12px;");

        // 添加标签
        Label hintLabel = new Label("提示：您可以根据需要修改提示词，调整AI的思考方向");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-padding: 0 0 5 0;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(hintLabel, promptEditor);

        dialog.getDialogPane().setContent(content);

        // 添加按钮
        ButtonType sendButton = new ButtonType("发送给AI", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, cancelButton);

        // 处理结果
        dialog.setResultConverter(buttonType -> {
            // 第二阶段：使用编辑后的提示词调用AI
            if (buttonType == sendButton) generateWithCustomPrompt(idea, promptEditor.getText());
            return buttonType;
        });
        dialog.showAndWait();
    }

    // 第二阶段：使用自定义提示词生成扩展
    private void generateWithCustomPrompt(Idea idea, String customPrompt) {
        statusLabel.setText("🤖 AI正在根据您的提示词思考...");
        aiSuggestions.setText("正在生成AI扩展，请稍候...");
        // 禁用AI按钮
        disableAIButtons();
        aiService.generateWithCustomPrompt(customPrompt)
                .thenAccept(suggestions -> Platform.runLater(() -> {
                    // 显示结果
                    displayAIResults(idea, suggestions, customPrompt);
                    // 启用AI按钮
                    enableAIButtons();
                    // 重置状态
                    synchronized (aiLock) {
                        isAIGenerating = false;
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        aiSuggestions.setText("AI扩展生成失败: " + e);
                        statusLabel.setText("AI扩展失败");
                        // 启用AI按钮
                        enableAIButtons();
                        // 重置状态
                        synchronized (aiLock) {
                            isAIGenerating = false;
                        }
                    });
                    return null;
                });
    }

    // 显示AI结果
    private void displayAIResults(Idea idea, String suggestions, String usedPrompt) {
        aiSuggestions.setText(suggestions);
        statusLabel.setText("AI扩展生成完成");

        // 添加元信息
        String metaInfo = String.format("\n\n---\n\n*提示词定制信息*\n- 灵感ID: %d\n- 生成时间: %s\n- 提示词长度: %d字符",
                idea.getId() != null ? idea.getId() : 0,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                usedPrompt.length()
        );
        aiSuggestions.appendText(metaInfo);
        // 添加操作按钮
        addAIResultActions(idea, usedPrompt);
    }
    // 添加结果操作按钮
    private void addAIResultActions(Idea idea, String usedPrompt) {
        VBox aiContainer = (VBox) aiSuggestions.getParent();
        // 检查是否已经有按钮容器
        if (aiButtonContainer != null && aiContainer.getChildren().contains(aiButtonContainer)) {
            // 按钮已存在，清除旧按钮，重新添加（因为这次可能更新了按钮）
            aiContainer.getChildren().remove(aiButtonContainer);
            aiButtonContainer = null;
        }
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        // 插入到编辑器按钮
        Button insertBtn = new Button("📝 插入到编辑器");
        insertBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertBtn.setOnAction(_ -> insertAIToEditor());
        // 保存提示词按钮
        Button savePromptBtn = new Button("💾 保存提示词模板");
        savePromptBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        savePromptBtn.setOnAction(_ -> savePromptTemplate(idea, usedPrompt));
        // 重新生成按钮
        Button regenerateBtn = new Button("🔄 重新生成");
        regenerateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        regenerateBtn.setOnAction(_ -> {
            synchronized (aiLock) {
                if (isAIGenerating) {
                    statusManager.showAlert("AI正在思考中，请稍后再试");
                    return;
                }
                isAIGenerating = true;
            }
            showPromptEditor(idea, usedPrompt);
        });
        buttonBox.getChildren().addAll(insertBtn, savePromptBtn, regenerateBtn);
        // 保存引用
        aiButtonContainer = buttonBox;
        // 添加到AI建议区域
        aiContainer.getChildren().add(buttonBox);
    }
    // 保存提示词模板
    private void savePromptTemplate(Idea idea, String prompt) {
        TextInputDialog dialog = new TextInputDialog("提示词模板-" + idea.getTitle());
        dialog.setTitle("保存提示词模板");
        dialog.setHeaderText("为这个提示词模板命名");
        dialog.setContentText("模板名称:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                // 保存到文件
                File templatesDir = new File(System.getProperty("user.home") + "/.inspiration-catcher/prompt-templates/");
                templatesDir.mkdirs();
                File templateFile = new File(templatesDir, name + ".txt");
                try (PrintWriter writer = new PrintWriter(templateFile)) {
                    writer.println("## " + name);
                    writer.println("灵感类型: " + idea.getType().getDisplayName());
                    writer.println("生成时间: " + java.time.LocalDateTime.now());
                    writer.println();
                    writer.println(prompt);
                }
                statusLabel.setText("提示词模板已保存: " + name);
            } catch (IOException e) {
                logger.error("保存提示词模板失败", e);
                statusManager.showAlert("保存失败\n"+ "无法保存模板: " + e.getMessage());
            }
        });
    }
    // 从默认提示词
    private String getDefaultPrompt(Idea idea) {
        return String.format("""
        作为创意扩展助手，请对以下灵感进行深入思考：
        
        《%s》
        %s
        
        请从多个角度分析这个灵感，包括：
        1. 核心价值：这个想法的独特之处是什么？
        2. 连接扩展：它可以与哪些现有概念或领域连接？
        3. 实现路径：如果要实现它，需要哪些步骤和资源？
        
        请用中文回答，语气亲切而专业。
        """,
                idea.getTitle() != null ? idea.getTitle() : "未命名灵感",
                idea.getContent() != null ? idea.getContent() : "[内容为空]"
        );
    }
    // 禁用所有AI相关按钮
    private void disableAIButtons() {
        Platform.runLater(() -> {
            // 找到AI建议按钮（如果有多个地方，需要一一处理）
            // 这里假设按钮在AI建议面板的底部
            VBox aiContainer = (VBox) aiSuggestions.getParent();
            // 遍历容器中的按钮
            for (Node node : aiContainer.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(true);
                    node.setStyle("-fx-opacity: 0.5;");
                } else if (node instanceof HBox) {
                    // 如果是按钮容器，禁用它里面的所有按钮
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Button) {
                            child.setDisable(true);
                            child.setStyle("-fx-opacity: 0.5;");
                        }
                    }
                }
            }
        });
    }
    // 启用所有AI相关按钮
    private void enableAIButtons() {
        Platform.runLater(() -> {
            VBox aiContainer = (VBox) aiSuggestions.getParent();
            // 遍历容器中的按钮
            for (Node node : aiContainer.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(false);
                    node.setStyle("");  // 重置样式
                } else if (node instanceof HBox) {
                    // 如果是按钮容器，启用它里面的所有按钮
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Button) {
                            child.setDisable(false);
                            child.setStyle("");  // 重置样式
                        }
                    }
                }
            }
        });
    }
    // ============================================================
    // Navigation Rail — view switching
    // ============================================================
    @FXML private void switchToIdeaList() {
        mainTabPane.getSelectionModel().select(0);
        shortcutManager.setActiveArea("table");
    }

    @FXML private void switchToMindMap() {
        mainTabPane.getSelectionModel().select(1);
        shortcutManager.setActiveArea("mindmap");
    }

    @FXML private void switchToEditor() {
        mainTabPane.getSelectionModel().select(2);
        shortcutManager.setActiveArea("editor");
    }

    @FXML private void focusSearch() {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
        }
    }

    @FXML private void handleExit() {
        if (statusManager.showConfirmDialog("确认退出", "退出灵感捕手", "确定要退出应用程序吗？")) {
            // 清理快捷键
            shortcutManager.cleanupShortcuts();
            // 关闭应用程序
            javafx.application.Platform.exit();
        }
    }
    @FXML public void handleSave() {logger.info("用户点击保存");editorController.saveCurrentIdea();updateStatistics();}
    // 字体调节方法
    @FXML private void handleIncreaseFont() {fontManager.increaseFontSize();fontManager.updateAllFonts(this);statusLabel.setText("字体已增大");}
    @FXML private void handleDecreaseFont() {fontManager.decreaseFontSize();fontManager.updateAllFonts(this);statusLabel.setText("字体已减小");}
    @FXML private void handleResetFont() {fontManager.resetFontSize();fontManager.updateAllFonts(this);statusLabel.setText("字体已重置");}
    @FXML private void showFontDialog() {fontManager.showFontDialog();fontManager.updateAllFonts(this);}
    @FXML private void resetAllFilters() {filterController.resetAllFilters();}
    @FXML private void updateFilterPredicate() {filterController.updateFilterPredicate();}
    @FXML private void refreshPreview() {editorController.updateMarkdownPreview();statusLabel.setText("预览已刷新");}
    // Markdown 处理方法
    @FXML public void handleBold() {insertMarkdownFormat("**", "**", "粗体文本");}
    @FXML public void handleItalic() {insertMarkdownFormat("*", "*", "斜体文本");}
    @FXML public void handleHeader1() {insertMarkdownFormat("# ", "", "一级标题");}
    @FXML public void handleHeader2() {insertMarkdownFormat("## ", "", "二级标题");}
    @FXML public void handleCode() {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            markdownEditor.replaceSelection("```\n" + selectedText + "\n```");
        } else {
            int caretPosition = markdownEditor.getCaretPosition();
            String text = markdownEditor.getText();
            String newText = text.substring(0, caretPosition) + "\n```\n代码块\n```\n" + text.substring(caretPosition);
            markdownEditor.setText(newText);
            markdownEditor.positionCaret(caretPosition + 8);
        }
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    @FXML private void handleList() {insertMarkdownFormat("- ", "", "列表项");}
    @FXML private void handleQuote() {insertMarkdownFormat("> ", "", "引用文本");}
    @FXML private void handleLink() {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        String linkText ="["+(selectedText != null && !selectedText.isEmpty() ? selectedText : "链接文本")+ "](https://example.com)";
        markdownEditor.replaceSelection(linkText);
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    @FXML private void handleImage() {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        String imageText ="!["+(selectedText != null && !selectedText.isEmpty() ?  selectedText : "图片描述")+ "](图片URL)";
        markdownEditor.replaceSelection(imageText);
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    // 辅助方法：插入 Markdown格式
    private void insertMarkdownFormat(String prefix, String suffix, String placeholder) {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        String formattedText = prefix + (selectedText != null && !selectedText.isEmpty() ? selectedText :placeholder) + suffix;
        markdownEditor.replaceSelection(formattedText);
        markdownEditor.requestFocus();

        // 如果插入的是占位符，选中占位符部分以便用户直接编辑
        if (selectedText == null || selectedText.isEmpty()) {
            int caretPosition = markdownEditor.getCaretPosition();
            markdownEditor.selectPositionCaret(caretPosition - placeholder.length());
        }
        updateMarkdownPreview();
    }
    // 更新 Markdown预览
    private void updateMarkdownPreview() {
        if (markdownPreview == null || markdownEditor == null) return;
        try {
            String markdown = markdownEditor.getText();
            String html = MarkdownUtil.markdownToHtml(markdown);
            markdownPreview.getEngine().loadContent(html);
        } catch (Exception e) {
            e.printStackTrace();
            markdownPreview.getEngine().loadContent("<pre>" + markdownEditor.getText() + "</pre>");
        }
    }
    @FXML private void generateOutline() {
        // 生成当前编辑器内容的大纲
        String content = markdownEditor.getText();
        if (content.trim().isEmpty()) {
            statusManager.showAlert("编辑器内容为空");
            return;
        }
        // 提取标题创建大纲
        String outline = extractMarkdownOutline(content);
        if (outline.isEmpty()) {
            statusManager.showAlert("未找到标题，无法生成大纲");
            return;
        }
        // 在大纲前添加提示
        String finalOutline = "## 内容大纲\n\n" + outline + "\n\n---\n\n";
        // 将大纲插入到内容开头
        markdownEditor.setText(finalOutline + content);
        statusLabel.setText("大纲已生成并插入到开头");
        handleSave();
    }
    private String extractMarkdownOutline(String markdown) {
        StringBuilder outline = new StringBuilder();
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) outline.append("- ").append(line.substring(2)).append("\n");
            else if (line.startsWith("## ")) outline.append("  - ").append(line.substring(3)).append("\n");
            else if (line.startsWith("### ")) outline.append("     - ").append(line.substring(4)).append("\n");
        }
        return outline.toString();
    }
    // 清理未使用的标签
    @FXML private void handleCleanUnusedTags() {
        if (statusManager.showConfirmDialog("确认清理", "清理未使用的标签",
                """
                        确定要清理未使用的标签吗？
                        
                        这将删除所有没有被任何灵感引用的标签。
                        此操作不可撤销。""")) {
            try {
                // 先进行预检，查看有多少未使用的标签
                int unusedCount = ideaManager.cleanUnusedTags(true);
                if (unusedCount == 0) {
                    statusManager.showAlert("没有需要清理的标签");
                    return;
                }
                // 再次确认
                if (statusManager.showConfirmDialog("再次确认",
                        "发现 " + unusedCount + " 个未使用的标签",
                        "确定要删除这 " + unusedCount + " 个未使用的标签吗？")) {
                    // 执行清理
                    int deletedCount = ideaManager.cleanUnusedTags();
                    // 更新热门标签显示
                    updateHotTags();
                    // 更新统计信息
                    updateStatistics();
                    statusManager.showAlert("已成功清理 " + deletedCount + " 个未使用的标签");
                    statusLabel.setText("已清理 " + deletedCount + " 个未使用的标签");
                }

            } catch (Exception e) {
                logger.error("清理标签失败", e);
                statusManager.showError("清理失败", e.getMessage());
            }
        }
    }
    // 显示标签统计信息
    @FXML private void showTagStats() {
        try {
            TagManager tagManager = new TagManager();
            Map<String, Integer> tagStats = tagManager.getTagUsageStats();

            if (tagStats.isEmpty()) {statusManager.showAlert("暂无标签统计信息");return;}
            // 构建统计信息
            StringBuilder statsText = new StringBuilder();
            statsText.append("📊 标签使用统计\n\n");
            int totalUsage = 0;
            for (Map.Entry<String, Integer> entry : tagStats.entrySet()) {
                statsText.append("• ").append(entry.getKey())
                        .append(": 使用 ").append(entry.getValue()).append(" 次\n");
                totalUsage += entry.getValue();
            }

            statsText.append("\n总计: ").append(tagStats.size()).append(" 个标签，")
                    .append(totalUsage).append(" 次使用");

            // 显示统计信息
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("标签统计");
            alert.setHeaderText("标签使用情况");

            TextArea textArea = new TextArea(statsText.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(400, 300);

            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();

        } catch (Exception e) {
            logger.error("显示标签统计失败", e);
            statusManager.showError("获取统计失败", e.getMessage());
        }
    }
    // 添加初始化思维导图的方法
    private void initializeMindMap() {
        if (mindMapPane == null) {logger.error("mindMapPane为null");return;}
        try {
            // 创建思维导图视图
            mindMapView = new MindMapView();
            // 设置灵感跳转回调
            mindMapView.setIdeaJumpCallback(ideaId -> Platform.runLater(() -> jumpToIdea(ideaId)));
            mindMapView.setPrefSize(800, 600);
            // 设置思维导图管理器（如果已初始化）
            if (mindMapManager != null) mindMapView.setMindMapManager(mindMapManager);
            // 设置当前项目
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject != null) {
                mindMapView.setCurrentProject(currentProject.getId());
                // 加载当前项目的灵感到左侧面板
                loadIdeasToMindMapPanel(currentProject);
            }
            // 添加到面板
            mindMapPane.getChildren().add(mindMapView);
            logger.info("思维导图初始化完成");
        } catch (Exception e) {logger.error("初始化思维导图失败", e);}
    }
    // 加载灵感到思维导图左侧面板

    // 刷新思维导图
    public void refreshMindMap() {
        if (mindMapManager != null) {
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject != null) {
                mindMapManager.loadProjectMindMap(currentProject.getId());
                // 刷新侧边栏
                loadIdeasToMindMapPanel(currentProject);
                statusLabel.setText("思维导图已刷新");
            }
        }
    }
    // 添加跳转到灵感的方法
    public void jumpToIdea(Integer ideaId) {
        if (ideaId == null) {logger.warn("跳转失败: 灵感ID为空");return;}
        try {
            // 1. 获取灵感对象
            Idea idea = ideaManager.getIdeaById(ideaId);
            if (idea == null) {statusLabel.setText("无法找到ID为 " + ideaId + " 的灵感");return;}
            logger.info("跳转到灵感: ID={}, Title={}", idea.getId(), idea.getTitle());
            // 2. 切换到编辑器标签页
            mainTabPane.getSelectionModel().select(2);
            // 3. 切换到编辑模式
            editorController.switchToEditMode(idea);
            // 4. 在表格中选中该灵感
            tableManager.selectAndShowIdea(idea);
            // 5. 更新状态栏
            statusLabel.setText("已跳转到灵感: " + (idea.getTitle() != null ? idea.getTitle() : "无标题灵感"));
        } catch (Exception e) {
            logger.error("跳转到灵感失败", e);
            statusLabel.setText("跳转失败: " + e.getMessage());
        }
    }
    private void loadIdeasToMindMapPanel(Project project) {
        if (project == null || mindMapIdeaListContainer == null) return;
        try {
            // 清空现有内容
            mindMapIdeaListContainer.getChildren().clear();
            // 获取当前项目的所有灵感
            List<Idea> ideas = ideaManager.getIdeasByProject(project.getId());
            if (ideas == null || ideas.isEmpty()) {
                Label emptyLabel = new Label("暂无灵感");
                emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 20;");
                mindMapIdeaListContainer.getChildren().add(emptyLabel);
                return;
            }
            for (Idea idea : ideas) mindMapIdeaListContainer.getChildren().add(createMindMapIdeaCard(idea));

            logger.info("已加载 {} 个灵感到思维导图列表", ideas.size());
        } catch (Exception e) {
            logger.error("加载灵感到列表失败", e);
        }
    }

    // 创建思维导图专用的灵感卡片
    private Node createMindMapIdeaCard(Idea idea) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        // 鼠标悬停效果
        card.setOnMouseEntered(_ -> card.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #bbdefb; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;"));
        card.setOnMouseExited(_ -> card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: default;"));
        // 标题行
        HBox titleRow = new HBox(5);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        // 类型图标
        Node typeIcon = createMindMapTypeIcon(idea.getType());
        // 标题
        Label titleLabel = new Label(idea.getTitle());
        titleLabel.setStyle("-fx-text-fill: #009fff; -fx-font-weight: bold; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleRow.getChildren().addAll(typeIcon, titleLabel);
        // 内容预览
        String contentPreview = idea.getContent();
        if (contentPreview.length() > 80) contentPreview = contentPreview.substring(0, 80) + "...";
        Label contentLabel = new Label(contentPreview);
        contentLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        contentLabel.setWrapText(true);
        // 元数据行（标签和重要性）
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        // 标签
        HBox tagsBox = new HBox(3);
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            for (int i = 0; i < Math.min(2, idea.getTags().size()); i++) {
                Tag tag = idea.getTags().get(i);
                Label tagLabel = new Label("#" + tag.getName());
                tagLabel.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 1 4; -fx-background-radius: 8; -fx-font-size: 12px;",
                        tag.getColor() != null ? tag.getColor() : "#4A90E2"
                ));
                tagsBox.getChildren().add(tagLabel);
            }
        }
        // 重要性星星
        HBox starsBox = new HBox(1);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < idea.getImportance() ? "★" : "☆");
            star.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px;");
            starsBox.getChildren().add(star);
        }
        // 心情图标
        Node moodIcon = createMindMapMoodIcon(idea.getMood());
        metaRow.getChildren().addAll(tagsBox, starsBox, moodIcon);
        card.getChildren().addAll(titleRow, contentLabel, metaRow);
        // 设置拖拽功能
        setupMindMapCardDrag(card, idea);
        return card;
    }
    // 创建类型图标（FontAwesome）
    private Node createMindMapTypeIcon(Idea.IdeaType type) {
        FontAwesomeSolid icon = switch (type) {
            case IDEA -> FontAwesomeSolid.LIGHTBULB;
            case QUOTE -> FontAwesomeSolid.QUOTE_LEFT;
            case QUESTION -> FontAwesomeSolid.QUESTION_CIRCLE;
            case TODO -> FontAwesomeSolid.CHECK_CIRCLE;
            case DISCOVERY -> FontAwesomeSolid.SEARCH;
            case CONFUSION -> FontAwesomeSolid.QUESTION;
            case HYPOTHESIS -> FontAwesomeSolid.FLASK;
        };
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(javafx.scene.paint.Color.web("#FFB300"));
        return fi;
    }

    // 创建心情图标（FontAwesome）
    private Node createMindMapMoodIcon(Idea.Mood mood) {
        FontAwesomeSolid icon = switch (mood) {
            case HAPPY -> FontAwesomeSolid.SMILE;
            case EXCITED -> FontAwesomeSolid.GRIN_STARS;
            case CALM -> FontAwesomeSolid.SMILE_BEAM;
            case NEUTRAL -> FontAwesomeSolid.MEH;
            case THOUGHTFUL -> FontAwesomeSolid.COMMENT;
            case CREATIVE -> FontAwesomeSolid.PALETTE;
            case INSPIRED -> FontAwesomeSolid.STAR;
            case CURIOUS -> FontAwesomeSolid.SEARCH;
            case CONFUSED -> FontAwesomeSolid.QUESTION;
            case FRUSTRATED -> FontAwesomeSolid.FROWN;
        };
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(javafx.scene.paint.Color.web("#FFB300"));
        return fi;
    }

    // 设置卡片拖拽到思维导图
    private void setupMindMapCardDrag(Node card, Idea idea) {
        // 开始拖拽
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("idea:" + idea.getId());
            // 设置拖拽图像
            Rectangle dragImage = new Rectangle(120, 60);
            dragImage.setFill(javafx.scene.paint.Color.web("#4A90E2", 0.8));
            dragImage.setArcWidth(10);
            dragImage.setArcHeight(10);

            Text dragText = new Text(idea.getTitle());
            dragText.setFill(javafx.scene.paint.Color.WHITE);
            dragText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            // 居中文本
            Bounds textBounds = dragText.getBoundsInLocal();
            dragText.setTranslateX((dragImage.getWidth() - textBounds.getWidth()) / 2);
            dragText.setTranslateY((dragImage.getHeight() + textBounds.getHeight()) / 2 - 4);

            Group dragGroup = new Group(dragImage, dragText);
            db.setDragView(dragGroup.snapshot(null, null));
            db.setContent(content);
            event.consume();

            logger.debug("开始拖拽灵感到思维导图: {}", idea.getTitle());
        });
        // 设置拖拽完成后的样式
        card.setOnDragDone(_ -> card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;"));
    }
    // 添加灵感节点按钮处理方法
    @FXML private void handleAddIdeaNodeToMindMap() {
        if (mindMapManager == null || mindMapView == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加灵感节点");
        dialog.setHeaderText("创建新的灵感节点");
        dialog.setContentText("请输入节点标题:");
        dialog.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                // 在画布中心创建节点
                double centerX = mindMapPane.getWidth() / 2;
                double centerY = mindMapPane.getHeight() / 2;
                // 这里应该弹出一个对话框让用户选择灵感
                // 暂时创建一个概念节点
                mindMapManager.createConceptNode(text.trim(), centerX, centerY);
            }
        });
        if (mindMapView != null) mindMapView.redraw();
    }
    // 添加概念节点按钮处理方法
    @FXML private void handleAddConceptNodeToMindMap() {
        if (mindMapManager == null || mindMapView == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加概念节点");
        dialog.setHeaderText("创建新的概念节点");
        dialog.setContentText("请输入概念文本:");

        dialog.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                double centerX = mindMapPane.getWidth() / 2;
                double centerY = mindMapPane.getHeight() / 2;
                mindMapManager.createConceptNode(text.trim(), centerX, centerY);
            }
        });
        if (mindMapView != null) mindMapView.redraw();
    }
    // 添加外部节点按钮处理方法
    @FXML private void handleAddExternalNodeToMindMap() {
        if (mindMapManager == null || mindMapView == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加外部链接节点");
        dialog.setHeaderText("创建外部链接节点");
        dialog.setContentText("请输入链接标题:");

        dialog.showAndWait().ifPresent(title -> {
            if (!title.trim().isEmpty()) {
                TextInputDialog urlDialog = new TextInputDialog("https://");
                urlDialog.setTitle("输入链接地址");
                urlDialog.setHeaderText("请输入链接地址");
                urlDialog.setContentText("URL:");

                urlDialog.showAndWait().ifPresent(url -> {
                    if (!url.trim().isEmpty()) {
                        double centerX = mindMapPane.getWidth() / 2;
                        double centerY = mindMapPane.getHeight() / 2;
                        mindMapManager.createExternalNode(title.trim(), url.trim(), centerX, centerY);
                    }
                });
            }
        });
        if (mindMapView != null) mindMapView.redraw();
    }
    // 刷新思维导图灵感列表
    @FXML private void refreshMindMapIdeaList() {
        Project currentProject = projectManager.getCurrentProject();
        if (currentProject != null) {
            loadIdeasToMindMapPanel(currentProject);
            logger.info("刷新思维导图灵感列表");
        }
    }
}