package com.inspiration.catcher.controller;

import com.inspiration.catcher.component.MindMapHandler;
import com.inspiration.catcher.component.MindMapView;
import com.inspiration.catcher.manager.FontManager;
import com.inspiration.catcher.manager.ShortcutManager;
import com.inspiration.catcher.manager.TableManager;
import com.inspiration.catcher.manager.*;
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
    // FXML injected UI components
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
        if (statusLabel != null) statusLabel.setText("Current: " + project.getName() + " - " + ideaManager.getIdeaCount() + " ideas");
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
            tableManager.setTableColumns(titleColumn, typeColumn, importanceColumn, moodColumn,
                    tagsColumn, createdAtColumn, updatedAtColumn);
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
    }

    public void updateStatistics() {
        try {
            int total = ideaManager.getTotalCount();
            int filtered = tableManager.getFilteredCount();
            int weekly = ideaManager.getWeeklyCount();
            String topTag = ideaManager.getTopTag();
            totalIdeasLabel.setText("Total: " + total);
            weeklyIdeasLabel.setText("+ " + weekly + " this week");
            topTagLabel.setText("Tag: " + topTag);
            statusLabel.setText(filtered == total
                    ? "Showing all " + total + " ideas"
                    : "Showing " + filtered + " of " + total + " ideas");
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
            statusLabel.setText("Refresh failed: " + e.getMessage());
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateHotTags() {
        if (tagCloud == null) return;
        tagCloud.getChildren().clear();
        TagManager tagManager = new TagManager();
        tagManager.loadAllTags();
        List<Tag> hotTags = tagManager.getHotTags(10);
        for (Tag tag : hotTags) {
            Label tagLabel = new Label("#" + tag.getName());
            tagLabel.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 12px;",
                    tag.getColor() != null ? tag.getColor() : "#4A90E2"));
            tagLabel.setOnMouseClicked(_ -> { tagFilter.setValue(tag.getName()); updateFilterPredicate(); });
            tagCloud.getChildren().add(tagLabel);
        }
        if (hotTags.isEmpty()) {
            Label emptyLabel = new Label("No tags");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            tagCloud.getChildren().add(emptyLabel);
        }
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
                statusLabel.setText("Created and switched to: " + projectName);
            }
        });
    }

    @FXML public void handleNewIdea() {
        ideaDetail.clear(); aiSuggestions.clear();
        mainTabPane.getSelectionModel().select(2);
        editorController.switchToNewMode();
    }

    @FXML public void handleQuickCapture() { QuickCaptureController.showQuickCaptureWindow(this); }

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
                    statusLabel.setText("Idea deleted");
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

    @FXML private void handleExit() {
        if (statusManager.showConfirmDialog("Confirm Exit", "Quit Inspiration Catcher", "Are you sure?")) {
            shortcutManager.cleanupShortcuts();
            javafx.application.Platform.exit();
        }
    }

    @FXML public void handleSave() { logger.info("Save clicked"); editorController.saveCurrentIdea(); updateStatistics(); }

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
}
