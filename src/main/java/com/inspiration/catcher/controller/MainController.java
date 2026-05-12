package com.inspiration.catcher.controller;

import com.inspiration.catcher.component.MindMapHandler;
import com.inspiration.catcher.component.MindMapView;
import com.inspiration.catcher.manager.FontManager;
import com.inspiration.catcher.manager.ShortcutManager;
import com.inspiration.catcher.manager.TableManager;
import com.inspiration.catcher.manager.*;
import com.inspiration.catcher.dao.FileDataManager;
import com.inspiration.catcher.model.*;
import com.inspiration.catcher.util.MarkdownUtil;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
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
    private static MainController instance;
    // Manager instances
    private final ProjectManager projectManager = new ProjectManager();
    private final IdeaManager ideaManager = new IdeaManager(projectManager);
    private final StatusManager statusManager = new StatusManager();
    private final UIConfigManager configManager = new UIConfigManager();
    private ShortcutManager shortcutManager;
    private MindMapManager mindMapManager;
    private FontManager fontManager;
    private TableManager tableManager;
    // Controller instances
    private FilterController filterController;
    private EditorController editorController;
    private AIPanelController aiPanelController;
    private MindMapHandler mindMapHandler;
    private MindMapView mindMapView;
    private StatisticsManager statisticsManager;
    private SnippetsManager snippetsManager;
    private CollectionManager collectionManager;
    // FXML injected UI components
    @FXML private ComboBox<Project> projectSelector;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<Integer> importanceFilter;
    @FXML private ComboBox<String> moodFilter;
    @FXML private DatePicker startDateFilter;
    @FXML private DatePicker endDateFilter;
    @FXML private TreeView<String> tagTreeView;
    @FXML private TabPane mainTabPane;
    @FXML public TableView<Idea> ideaTableView;
    @FXML private TextField editorTitleField;
    @FXML private ComboBox<String> editorTypeCombo;
    @FXML public TextArea markdownEditor;
    @FXML private WebView markdownPreview;
    @FXML public TextArea ideaDetail;
    @FXML private VBox detailPanel;
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
    @FXML private VBox chartContainer;
    @FXML private Button favoriteBtn;
    // 卡片式单列（替代多列表格）
    @FXML private TableColumn<Idea, Idea> cardColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        logger.info("Initializing main controller");
        try {
            initializeProjectSelector();
            initializeManagers();
            configManager.loadConfig();
            tableManager.setupTable();
            tableManager.loadDataToTable();
            editorController.setupEditor();
            // Connect FontManager to scene for root-level CSS font scaling
            javafx.application.Platform.runLater(() -> {
                if (projectSelector != null && projectSelector.getScene() != null) {
                    fontManager.setTargetScene(projectSelector.getScene());
                }
                // 初始化统计图表
                initStatisticsCharts();
            });
            loadProjectData(projectManager.getCurrentProject());
            initializeMindMap();
            projectManager.currentProjectProperty().addListener((_, _, newProject) -> {
                if (newProject != null) { loadProjectData(newProject); tableManager.loadDataToTable(); }
            });
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject != null) loadProjectData(currentProject);
            else {
                logger.error("Current project is null, using default");
                Project defaultProject = projectManager.findDefaultProject();
                if (defaultProject != null) {
                    projectManager.setCurrentProject(defaultProject);
                    loadProjectData(defaultProject);
                }
            }
            setupEventHandlers();
            updateHotTags();
            updateStatistics();
            ideaDetail.setEditable(false);
            aiSuggestions.setEditable(false);
            relatedIdeasList.setEditable(false);
            Platform.runLater(() -> {
                logger.info("Setting up keyboard shortcuts");
                try {
                    Thread.sleep(300);
                    shortcutManager.setupShortcuts();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("Failed to set up shortcuts", e);
                }
            });
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> {
                if (newTab != null) switch (newTab.getText()) {
                    case "灵感列表" -> shortcutManager.setActiveArea("table");
                    case "思维导图" -> shortcutManager.setActiveArea("mindmap");
                    case "编辑器" -> shortcutManager.setActiveArea("editor");
                }
            });
            filterController.updateFilterPredicate();
            logger.info("Main controller initialization complete");
        } catch (Exception e) {
            logger.error("Initialization failed", e);
            statusLabel.setText("Initialization failed: " + e.getMessage());
            showErrorDialog("Initialization Error", e.getMessage());
        }
    }

    private void initializeProjectSelector() {
        if (projectSelector != null) {
            projectSelector.setItems(projectManager.getProjectList());
            Project current = projectManager.getCurrentProject();
            if (current != null) projectSelector.setValue(current);
            projectSelector.valueProperty().addListener((_, _, newValue) -> {
                if (newValue != null && !newValue.equals(projectManager.getCurrentProject()))
                    projectManager.switchToProject(newValue);
            });
        }
    }

    private void loadProjectData(Project project) {
        if (project == null) return;
        logger.info("Loading project data: {}", project.getName());
        ideaManager.loadAllIdeas();
        if (mindMapView != null) loadIdeasToMindMapPanel(project);
        if (mindMapManager != null) {
            mindMapManager.setCurrentProject(project);
            if (mindMapView != null) mindMapView.setCurrentProject(project.getId());
        }
        if (filterController != null) {
            filterController.refreshDataSource();
            filterController.refreshTagOptions();
        }
        if (tableManager != null) tableManager.loadDataToTable();
        if (mindMapManager != null) { mindMapManager.setCurrentProject(project); refreshMindMap(); }
        if (filterController != null) filterController.updateFilterPredicate();
        if (statusLabel != null) statusLabel.setText(project.getName() + " - " + ideaManager.getIdeaCount() + " 条灵感");
        updateStatistics();
        ideaTableView.refresh();
    }

    private void initializeManagers() {
        logger.info("Initializing managers");
        try {
            if (projectManager.getCurrentProject() == null) {
                logger.warn("Current project is null, setting default");
                projectManager.setCurrentProject(projectManager.findDefaultProject());
            }
            ideaManager.loadAllIdeas();
            mindMapManager = new MindMapManager(ideaManager);
            filterController = new FilterController(
                    searchField, typeFilter, importanceFilter,
                    startDateFilter, endDateFilter, tagFilter, moodFilter, ideaManager, this
            );
            filterController.setupFilters();
            tableManager = new TableManager(ideaTableView, ideaManager, this);
            tableManager.setFilteredIdeas(filterController.getFilteredIdeas());
            tableManager.setCardColumn(cardColumn);
            tableManager.setDetailPanels(ideaDetail);
            tableManager.setStatusLabels(statusLabel);
            editorController = new EditorController(
                    editorTitleField, editorTypeCombo, editorImportanceSpinner, editorMoodCombo, editorTagsField,
                    markdownEditor, markdownPreview, ideaManager, tableManager, projectManager, this
            );
            aiPanelController = new AIPanelController(
                    aiSuggestions, markdownEditor, statusLabel,
                    ideaTableView, mainTabPane, ideaManager,
                    statusManager, () -> handleSave()
            );
            fontManager = new FontManager(configManager);
            statisticsManager = new StatisticsManager(ideaManager);
            snippetsManager = new SnippetsManager();
            collectionManager = new CollectionManager();
            mindMapHandler = new MindMapHandler(mindMapPane, mindMapIdeaListContainer,
                    statusLabel, mainTabPane, ideaManager, projectManager,
                    editorController, tableManager);
            mindMapHandler.setMindMapManager(mindMapManager);
            shortcutManager = new ShortcutManager(this);
            tableManager.setupTable();
            tableManager.loadDataToTable();
            if (mindMapView != null) mindMapView.setMindMapManager(mindMapManager);
            logger.info("All managers initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize managers", e);
            e.printStackTrace();
            statusLabel.setText("Manager initialization failed: " + e.getMessage());
        }
    }

    public void loadInitialData() {
        try {
            resetAllFilters();
            ideaManager.loadAllIdeas();
            statusLabel.setText("Data loaded: " + ideaManager.getIdeaCount() + " ideas");
        } catch (Exception e) {
            logger.error("Failed to load data", e);
            statusLabel.setText("Data load failed");
        }
    }

    private void setupEventHandlers() {
        ideaTableView.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newValue) -> {
                    if (newValue != null) {
                        tableManager.showIdeaDetail(newValue);
                        if (detailPanel != null) { detailPanel.setManaged(true); detailPanel.setVisible(true); }
                    } else {
                        if (detailPanel != null) { detailPanel.setManaged(false); detailPanel.setVisible(false); }
                    }
                });
        filterController.setupEventHandlers();
        editorController.setupEventHandlers();
        // 搜索高亮
        searchField.textProperty().addListener((_, _, newVal) -> {
            com.inspiration.catcher.component.IdeaCardCell.highlightKeyword = newVal;
            ideaTableView.refresh();
        });
    }

    public void updateStatistics() {
        try {
            int total = ideaManager.getTotalCount();
            int filtered = tableManager.getFilteredCount();
            int weekly = ideaManager.getWeeklyCount();
            String topTag = ideaManager.getTopTag();
            totalIdeasLabel.setText(String.valueOf(total));
            weeklyIdeasLabel.setText(String.valueOf(weekly));
            topTagLabel.setText(topTag.isEmpty() ? "无" : "#" + topTag);
            if (statisticsManager != null) statisticsManager.refresh();
            statusLabel.setText(filtered == total
                    ? "显示全部 " + total + " 条灵感"
                    : "显示 " + filtered + " 条（共 " + total + " 条）");
        } catch (Exception e) { logger.error("Failed to update statistics", e); }
    }

    // Public API called by sub-controllers
    public TableView<Idea> getIdeaTableView() { return ideaTableView; }
    public Label getStatusLabel() { return statusLabel; }
    public IdeaManager getIdeaManager() { return ideaManager; }
    public ProjectManager getProjectManager() { return projectManager; }
    public TextField getSearchField() { return searchField; }
    public Project getCurrentProject() { return projectManager.getCurrentProject(); }
    public static Integer getCurrentProjectId() {
        if (instance != null) {
            Project currentProject = instance.projectManager.getCurrentProject();
            return currentProject != null ? currentProject.getId() : 1;
        }
        return 1;
    }

    private void initStatisticsCharts() {
        if (statisticsManager == null || chartContainer == null) return;
        try {
            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(statisticsManager.createTypePieChart());
            chartContainer.getChildren().add(statisticsManager.createWeeklyBarChart());
            logger.info("Statistics charts initialized");
        } catch (Exception e) {
            logger.error("Failed to init stats charts", e);
        }
    }

    @FXML public void refreshTableData() {
        try {
            logger.info("Refreshing table data");
            ideaManager.loadAllIdeas();
            FilteredList<Idea> filteredList = filterController.getFilteredIdeas();
            if (filteredList != null) filterController.updateFilterPredicate();
            if (ideaTableView != null) {
                ideaTableView.refresh();
                ideaTableView.getSelectionModel().clearSelection();
            }
            updateStatistics();
            updateHotTags();
            if (mindMapView != null) {
                Project currentProject = projectManager.getCurrentProject();
                if (currentProject != null) loadIdeasToMindMapPanel(currentProject);
            }
        } catch (Exception e) {
            logger.error("Failed to refresh table", e);
            statusLabel.setText("刷新失败: " + e.getMessage());
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateHotTags() {
        if (tagTreeView == null) return;
        TagManager tagManager = new TagManager();
        tagManager.loadAllTags();
        List<Tag> tags = tagManager.getHotTags(20);

        TreeItem<String> root = new TreeItem<>("所有标签 (" + tags.size() + ")");
        root.setExpanded(true);

        // 按首字母分组
        java.util.Map<String, List<Tag>> grouped = tags.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    t -> t.getName().substring(0, 1).toUpperCase()));
        grouped.keySet().stream().sorted().forEach(letter -> {
            TreeItem<String> letterItem = new TreeItem<>(letter);
            for (Tag tag : grouped.get(letter)) {
                TreeItem<String> tagItem = new TreeItem<>("#" + tag.getName() + " (" + tag.getUsageCount() + ")");
                tagItem.setValue("#" + tag.getName());
                letterItem.getChildren().add(tagItem);
            }
            root.getChildren().add(letterItem);
        });

        if (tags.isEmpty()) {
            root.setValue("暂无标签");
        }
        tagTreeView.setRoot(root);

        // 点击标签过滤
        tagTreeView.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            if (selected != null && !selected.equals(root)) {
                String val = selected.getValue();
                if (val.startsWith("#")) {
                    String tagName = val.contains(" (") ? val.substring(1, val.indexOf(" (")) : val.substring(1);
                    tagFilter.setValue(tagName);
                    updateFilterPredicate();
                }
            }
        });
    }

    // ============================================================
    // FXML Event Handlers
    // ============================================================
    @FXML private void handleNewProject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project"); dialog.setHeaderText("Create a new project"); dialog.setContentText("Project name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(projectName -> {
            Project newProject = projectManager.createProject(projectName, "", "#36B37E");
            if (newProject != null) {
                projectManager.switchToProject(newProject);
                statusLabel.setText("已创建并切换到项目: " + projectName);
            }
        });
    }

    @FXML public void handleNewIdea() {
        ideaDetail.clear(); aiSuggestions.clear();
        mainTabPane.getSelectionModel().select(2);
        editorController.switchToNewMode();
    }

    @FXML public void handleQuickCapture() { QuickCaptureController.showQuickCaptureWindow(this); }

    @FXML public void handleToggleFavorite() {
        Idea selected = tableManager.getSelectedIdea();
        if (selected == null || selected.getId() == null) {
            statusManager.showAlert("请先选择一个灵感");
            return;
        }
        if (collectionManager == null) return;
        // 默认收藏到第一个收藏夹
        var collections = collectionManager.getAll();
        if (collections.isEmpty()) return;
        var fav = collections.get(0);
        if (fav.contains(selected.getId())) {
            fav.removeIdeaId(selected.getId());
            collectionManager.save();
            if (favoriteBtn != null) favoriteBtn.setText("收藏");
            statusLabel.setText("已取消收藏");
        } else {
            fav.addIdeaId(selected.getId());
            collectionManager.save();
            if (favoriteBtn != null) favoriteBtn.setText("★ 已收藏");
            statusLabel.setText("已添加到收藏");
        }
    }

    @FXML public void handleExportHtml() {
        try {
            java.util.List<Idea> ideas = new java.util.ArrayList<>(ideaTableView.getItems());
            if (ideas.isEmpty()) { statusManager.showAlert("No ideas to export"); return; }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Export as HTML");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("HTML", "*.html"));
            fc.setInitialFileName("ideas-export.html");
            java.io.File file = fc.showSaveDialog(ideaTableView.getScene().getWindow());
            if (file == null) return;
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>灵感捕手导出</title>");
            sb.append("<style>body{font-family:'Microsoft YaHei',sans-serif;max-width:800px;margin:0 auto;padding:20px;background:#f7f4f0;color:#2c2924}");
            sb.append(".idea-card{background:white;border-radius:8px;padding:16px;margin:12px 0;box-shadow:0 1px 3px rgba(0,0,0,0.1)}");
            sb.append(".title{font-size:18px;font-weight:600;color:#C4843C}.meta{font-size:12px;color:#7a746e;margin:4px 0}");
            sb.append(".content{margin-top:8px;line-height:1.6}.tag{display:inline-block;background:#f5ecd8;color:#C4843C;border-radius:10px;padding:1px 8px;font-size:12px;margin:2px}");
            sb.append("</style></head><body>");
            sb.append("<h1>灵感捕手 - 导出文档</h1>");
            sb.append("<p>共 ").append(ideas.size()).append(" 条灵感</p><hr>");
            for (Idea idea : ideas) {
                String typeColor = switch (idea.getType()) {
                    case IDEA -> "#C4843C"; case QUOTE -> "#5B7FAF"; case QUESTION -> "#8B6FAF";
                    case TODO -> "#5B8C5A"; case DISCOVERY -> "#C4A84C"; case CONFUSION -> "#C45656";
                    case HYPOTHESIS -> "#C4843C";
                };
                sb.append("<div class='idea-card' style='border-left:4px solid ").append(typeColor).append("'>");
                sb.append("<div class='title'>").append(escapeHtml(idea.getTitle())).append("</div>");
                sb.append("<div class='meta'>").append(idea.getType().getDisplayName());
                if (idea.getTags() != null) for (var tag : idea.getTags()) {
                    sb.append(" <span class='tag'>#").append(escapeHtml(tag.getName())).append("</span>");
                }
                sb.append(" &middot; ").append(com.inspiration.catcher.util.DateUtil.formatDateTime(idea.getCreatedAt()));
                sb.append("</div>");
                sb.append("<div class='content'>").append(escapeHtml(idea.getContent())).append("</div>");
                sb.append("</div>");
            }
            sb.append("</body></html>");
            java.nio.file.Files.writeString(file.toPath(), sb.toString());
            statusLabel.setText("Exported " + ideas.size() + " ideas to HTML");
        } catch (Exception e) { logger.error("HTML export failed", e); statusManager.showError("Export failed", e.getMessage()); }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", "<br>");
    }

    @FXML public void handleEditIdea() {
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            mainTabPane.getSelectionModel().select(2);
            editorController.switchToEditMode(selected);
        } else statusManager.showAlert("Please select an idea first");
    }

    @FXML public void handleDeleteIdea() {
        Idea selected = tableManager.getSelectedIdea();
        if (selected != null) {
            if (statusManager.showConfirmDialog("Confirm Delete", "Delete Idea",
                    "This action cannot be undone.")) {
                try {
                    ideaManager.deleteIdea(selected);
                    tableManager.refreshTable();
                    statusLabel.setText("灵感已删除");
                    updateStatistics(); updateHotTags();
                } catch (Exception e) { statusManager.showError("Delete failed", e.getMessage()); }
            }
        }
    }

    // AI Delegation — handled by AIPanelController
    @FXML private void generateAISuggestions() { aiPanelController.generateSuggestions(); }
    @FXML private void generateAIExpansion() { aiPanelController.generateExpansion(); }
    @FXML private void testAIConnection() { aiPanelController.testConnection(); }
    @FXML private void handleAISettings() { aiPanelController.showSettings(); }

    // Navigation Rail
    @FXML private void switchToIdeaList() { mainTabPane.getSelectionModel().select(0); shortcutManager.setActiveArea("table"); }
    @FXML private void switchToMindMap() { mainTabPane.getSelectionModel().select(1); shortcutManager.setActiveArea("mindmap"); }
    @FXML private void switchToEditor() { mainTabPane.getSelectionModel().select(2); shortcutManager.setActiveArea("editor"); }

    @FXML private void focusSearch() { if (searchField != null) { searchField.requestFocus(); searchField.selectAll(); } }

    @FXML private void handleSaveProjectToFile() {
        if (ideaManager == null) return;
        try {
            String projectName = projectManager.getCurrentProject().getName();
            FileDataManager fileMgr = new FileDataManager();
            // Create project dir
            fileMgr.createProject(projectName);
            fileMgr.openProject(projectName);
            // Save all ideas
            List<Idea> ideas = ideaManager.getIdeaList();
            for (Idea idea : ideas) {
                fileMgr.saveIdea(idea);
            }
            // Save tags
            TagManager tagMgr = new TagManager();
            tagMgr.loadAllTags();
            for (Tag tag : tagMgr.getTagList()) {
                fileMgr.saveTag(tag);
            }
            statusLabel.setText("项目已保存到文件: projects/" + projectName);
        } catch (Exception e) {
            logger.error("Failed to save project to file", e);
            statusLabel.setText("保存失败: " + e.getMessage());
        }
    }

    @FXML private void handleOpenProjectFromFile() {
        try {
            FileDataManager fileMgr = new FileDataManager();
            List<String> projects = fileMgr.listProjects();
            if (projects.isEmpty()) {
                statusManager.showAlert("没有找到保存的项目文件");
                return;
            }
            // Show project selection dialog
            ChoiceDialog<String> dialog = new ChoiceDialog<>(projects.get(0), projects);
            dialog.setTitle("打开项目");
            dialog.setHeaderText("从文件选择要打开的项目");
            dialog.setContentText("项目:");
            dialog.showAndWait().ifPresent(selected -> {
                if (fileMgr.openProject(selected)) {
                    // Load ideas from file
                    List<Idea> fileIdeas = fileMgr.loadAllIdeas();
                    if (!fileIdeas.isEmpty()) {
                        // Import into current project
                        statusLabel.setText("已加载 " + fileIdeas.size() + " 条灵感");
                        ideaTableView.refresh();
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to open project from file", e);
            statusLabel.setText("打开失败: " + e.getMessage());
        }
    }

    @FXML private void handleExit() {
        if (statusManager.showConfirmDialog("Confirm Exit", "Quit Inspiration Catcher", "Are you sure?")) {
            shortcutManager.cleanupShortcuts();
            javafx.application.Platform.exit();
        }
    }

    @FXML public void handleSave() { logger.info("保存"); editorController.saveCurrentIdea(); updateStatistics(); }

    // Font controls
    @FXML private void handleIncreaseFont() { fontManager.increaseFontSize(); fontManager.updateAllFonts(this); statusLabel.setText("Font increased"); }
    @FXML private void handleDecreaseFont() { fontManager.decreaseFontSize(); fontManager.updateAllFonts(this); statusLabel.setText("Font decreased"); }
    @FXML private void handleResetFont() { fontManager.resetFontSize(); fontManager.updateAllFonts(this); statusLabel.setText("Font reset"); }
    @FXML private void showFontDialog() { fontManager.showFontDialog(); fontManager.updateAllFonts(this); }

    // Filters
    @FXML private void resetAllFilters() { filterController.resetAllFilters(); }
    @FXML private void updateFilterPredicate() { filterController.updateFilterPredicate(); }
    @FXML private void refreshPreview() { editorController.updateMarkdownPreview(); statusLabel.setText("Preview refreshed"); }

    // Markdown toolbar
    @FXML public void handleBold() { insertMarkdownFormat("**", "**", "bold text"); }
    @FXML public void handleItalic() { insertMarkdownFormat("*", "*", "italic text"); }
    @FXML public void handleHeader1() { insertMarkdownFormat("# ", "", "Heading 1"); }
    @FXML public void handleHeader2() { insertMarkdownFormat("## ", "", "Heading 2"); }
    @FXML public void handleCode() {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            markdownEditor.replaceSelection("```\n" + selectedText + "\n```");
        } else {
            int pos = markdownEditor.getCaretPosition();
            String text = markdownEditor.getText();
            markdownEditor.setText(text.substring(0, pos) + "\n```\ncode block\n```\n" + text.substring(pos));
            markdownEditor.positionCaret(pos + 8);
        }
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    @FXML private void handleList() { insertMarkdownFormat("- ", "", "list item"); }
    @FXML private void handleQuote() { insertMarkdownFormat("> ", "", "quote"); }
    @FXML private void handleLink() {
        if (markdownEditor == null) return;
        String sel = markdownEditor.getSelectedText();
        markdownEditor.replaceSelection("[" + (sel != null && !sel.isEmpty() ? sel : "link text") + "](https://example.com)");
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    @FXML private void handleImage() {
        if (markdownEditor == null) return;
        String sel = markdownEditor.getSelectedText();
        markdownEditor.replaceSelection("![" + (sel != null && !sel.isEmpty() ? sel : "image alt") + "](image url)");
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }
    private void insertMarkdownFormat(String prefix, String suffix, String placeholder) {
        if (markdownEditor == null) return;
        String selectedText = markdownEditor.getSelectedText();
        String formatted = prefix + (selectedText != null && !selectedText.isEmpty() ? selectedText : placeholder) + suffix;
        markdownEditor.replaceSelection(formatted);
        markdownEditor.requestFocus();
        if (selectedText == null || selectedText.isEmpty()) {
            int pos = markdownEditor.getCaretPosition();
            markdownEditor.selectPositionCaret(pos - placeholder.length());
        }
        updateMarkdownPreview();
    }
    private void updateMarkdownPreview() {
        if (markdownPreview == null || markdownEditor == null) return;
        try {
            String html = MarkdownUtil.markdownToHtml(markdownEditor.getText());
            markdownPreview.getEngine().loadContent(html);
        } catch (Exception e) {
            e.printStackTrace();
            markdownPreview.getEngine().loadContent("<pre>" + markdownEditor.getText() + "</pre>");
        }
    }
    @FXML private void showSnippetsMenu() {
        if (snippetsManager == null) return;
        ContextMenu menu = new ContextMenu();
        // 按类别分组
        java.util.Map<String, java.util.List<SnippetsManager.Snippet>> grouped = snippetsManager.getSnippets()
                .stream().collect(java.util.stream.Collectors.groupingBy(SnippetsManager.Snippet::getCategory));
        grouped.forEach((cat, snippetList) -> {
            Menu categoryMenu = new Menu(cat);
            for (SnippetsManager.Snippet s : snippetList) {
                MenuItem item = new MenuItem(s.getName());
                item.setOnAction(_ -> insertSnippet(s.getContent()));
                categoryMenu.getItems().add(item);
            }
            menu.getItems().add(categoryMenu);
        });
        menu.show(markdownEditor.getScene().getWindow(),
                javafx.geometry.Point2D.ZERO.getX(),
                markdownEditor.getScene().getWindow().getY() + 200);
    }

    private void insertSnippet(String content) {
        if (markdownEditor == null || content == null) return;
        int pos = markdownEditor.getCaretPosition();
        String text = markdownEditor.getText();
        markdownEditor.setText(text.substring(0, pos) + content + text.substring(pos));
        markdownEditor.positionCaret(pos + content.length());
        markdownEditor.requestFocus();
        updateMarkdownPreview();
    }

    @FXML private void generateOutline() {
        String content = markdownEditor.getText();
        if (content.trim().isEmpty()) { statusManager.showAlert("Editor is empty"); return; }
        StringBuilder outline = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.startsWith("# ")) outline.append("- ").append(line.substring(2)).append("\n");
            else if (line.startsWith("## ")) outline.append("  - ").append(line.substring(3)).append("\n");
            else if (line.startsWith("### ")) outline.append("     - ").append(line.substring(4)).append("\n");
        }
        if (outline.isEmpty()) { statusManager.showAlert("No headings found"); return; }
        markdownEditor.setText("## Outline\n\n" + outline + "\n---\n\n" + content);
        statusLabel.setText("Outline generated"); handleSave();
    }

    // Export
    @FXML private void handleExportJson() {
        try {
            List<Idea> ideas = new ArrayList<>(ideaTableView.getItems());
            if (ideas.isEmpty()) { statusManager.showAlert("No ideas to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Export as JSON");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            fc.setInitialFileName("ideas-export.json");
            File file = fc.showSaveDialog(ideaTableView.getScene().getWindow());
            if (file == null) return;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, ideas);
            statusLabel.setText("Exported " + ideas.size() + " ideas to JSON");
        } catch (Exception e) { logger.error("Export failed", e); statusManager.showError("Export failed", e.getMessage()); }
    }

    @FXML private void handleExportMarkdown() {
        try {
            List<Idea> ideas = new ArrayList<>(ideaTableView.getItems());
            if (ideas.isEmpty()) { statusManager.showAlert("No ideas to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Export as Markdown");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown", "*.md"));
            fc.setInitialFileName("ideas-export.md");
            File file = fc.showSaveDialog(ideaTableView.getScene().getWindow());
            if (file == null) return;
            StringBuilder sb = new StringBuilder("# Exported Ideas\n\n");
            for (Idea idea : ideas) {
                sb.append("## ").append(idea.getTitle() != null ? idea.getTitle() : "Untitled").append("\n\n");
                sb.append("**Type:** ").append(idea.getType().getDisplayName()).append("  \n");
                sb.append("**Importance:** ").append("★".repeat(idea.getImportance())).append("☆".repeat(5 - idea.getImportance())).append("  \n");
                sb.append("**Mood:** ").append(idea.getMood().getDisplayName()).append("  \n");
                sb.append("**Created:** ").append(idea.getCreatedAt()).append("  \n");
                if (!idea.getTags().isEmpty()) {
                    sb.append("**Tags:** ");
                    for (Tag t : idea.getTags()) sb.append("#").append(t.getName()).append(" ");
                    sb.append("  \n");
                }
                sb.append("\n").append(idea.getContent()).append("\n\n---\n\n");
            }
            java.nio.file.Files.writeString(file.toPath(), sb.toString());
            statusLabel.setText("Exported " + ideas.size() + " ideas to Markdown");
        } catch (Exception e) { logger.error("Export failed", e); statusManager.showError("Export failed", e.getMessage()); }
    }

    // Tag management
    @FXML private void handleCleanUnusedTags() {
        if (!statusManager.showConfirmDialog("Confirm", "Clean unused tags",
                "Remove all tags not referenced by any idea?")) return;
        try {
            int unusedCount = ideaManager.cleanUnusedTags(true);
            if (unusedCount == 0) { statusManager.showAlert("No unused tags"); return; }
            if (statusManager.showConfirmDialog("Confirm", "Found " + unusedCount + " unused tags",
                    "Delete these " + unusedCount + " tags?")) {
                int deleted = ideaManager.cleanUnusedTags();
                updateHotTags(); updateStatistics();
                statusManager.showAlert("Cleaned " + deleted + " tags");
                statusLabel.setText("Cleaned " + deleted + " unused tags");
            }
        } catch (Exception e) { logger.error("Tag cleanup failed", e); statusManager.showError("Cleanup failed", e.getMessage()); }
    }
    @FXML private void showTagStats() {
        try {
            TagManager tagManager = new TagManager();
            Map<String, Integer> stats = tagManager.getTagUsageStats();
            if (stats.isEmpty()) { statusManager.showAlert("No tag statistics"); return; }
            StringBuilder sb = new StringBuilder("Tag Statistics\n\n");
            int total = 0;
            for (Map.Entry<String, Integer> e : stats.entrySet()) { sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append(" times\n"); total += e.getValue(); }
            sb.append("\nTotal: ").append(stats.size()).append(" tags, ").append(total).append(" uses");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Tag Stats"); alert.setHeaderText("Tag Usage");
            TextArea ta = new TextArea(sb.toString()); ta.setEditable(false); ta.setWrapText(true); ta.setPrefSize(400, 300);
            alert.getDialogPane().setContent(ta); alert.showAndWait();
        } catch (Exception e) { logger.error("Tag stats failed", e); statusManager.showError("Stats failed", e.getMessage()); }
    }

    // Mind map — delegated to MindMapHandler
    private void initializeMindMap() {
        if (mindMapHandler != null) {
            mindMapHandler.initialize();
            mindMapView = mindMapHandler.getMindMapView();
        }
    }
    public void refreshMindMap() { if (mindMapHandler != null) mindMapHandler.refresh(); }
    public void jumpToIdea(Integer ideaId) { if (mindMapHandler != null) mindMapHandler.jumpToIdea(ideaId); }
    public void loadIdeasToMindMapPanel(Project project) { if (mindMapHandler != null) mindMapHandler.loadIdeasToMindMapPanel(project); }
    @FXML private void handleAddIdeaNodeToMindMap() { if (mindMapHandler != null) mindMapHandler.addConceptNode(); }
    @FXML private void handleAddConceptNodeToMindMap() { if (mindMapHandler != null) mindMapHandler.addConceptNode(); }
    @FXML private void handleAddExternalNodeToMindMap() { if (mindMapHandler != null) mindMapHandler.addExternalNode(); }
    @FXML private void refreshMindMapIdeaList() { if (mindMapHandler != null) mindMapHandler.refreshIdeaList(); }
    @FXML private void handleAutoLayout() { if (mindMapHandler != null) mindMapHandler.applyAutoLayout(); }
    @FXML private void handleExportMindMapImage() { if (mindMapHandler != null) mindMapHandler.exportImage(); }
}
