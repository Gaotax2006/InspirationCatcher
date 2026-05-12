package com.inspiration.catcher.controller;

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

    // Mind map initialization
    private void initializeMindMap() {
        if (mindMapPane == null) { logger.error("mindMapPane is null"); return; }
        try {
            mindMapView = new MindMapView();
            mindMapView.setIdeaJumpCallback(ideaId -> Platform.runLater(() -> jumpToIdea(ideaId)));
            mindMapView.setPrefSize(800, 600);
            if (mindMapManager != null) mindMapView.setMindMapManager(mindMapManager);
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject != null) {
                mindMapView.setCurrentProject(currentProject.getId());
                loadIdeasToMindMapPanel(currentProject);
            }
            mindMapPane.getChildren().add(mindMapView);
        } catch (Exception e) { logger.error("Mind map init failed", e); }
    }

    public void refreshMindMap() {
        if (mindMapManager != null) {
            Project p = projectManager.getCurrentProject();
            if (p != null) { mindMapManager.loadProjectMindMap(p.getId()); loadIdeasToMindMapPanel(p); statusLabel.setText("Mind map refreshed"); }
        }
    }

    public void jumpToIdea(Integer ideaId) {
        if (ideaId == null) return;
        Idea idea = ideaManager.getIdeaById(ideaId);
        if (idea == null) { statusLabel.setText("Idea not found: ID " + ideaId); return; }
        mainTabPane.getSelectionModel().select(2);
        editorController.switchToEditMode(idea);
        tableManager.selectAndShowIdea(idea);
        statusLabel.setText("Jumped to: " + (idea.getTitle() != null ? idea.getTitle() : "untitled"));
    }

    private void loadIdeasToMindMapPanel(Project project) {
        if (project == null || mindMapIdeaListContainer == null) return;
        mindMapIdeaListContainer.getChildren().clear();
        List<Idea> ideas = ideaManager.getIdeasByProject(project.getId());
        if (ideas == null || ideas.isEmpty()) {
            Label empty = new Label("No ideas");
            empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 20;");
            mindMapIdeaListContainer.getChildren().add(empty);
            return;
        }
        for (Idea idea : ideas) mindMapIdeaListContainer.getChildren().add(createMindMapIdeaCard(idea));
    }

    private Node createMindMapIdeaCard(Idea idea) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD4; -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: dropshadow(gaussian, rgba(44,41,36,0.06), 4, 0, 0, 1);");
        card.setOnMouseEntered(_ -> card.setStyle("-fx-background-color: #EBE6DE; -fx-border-color: #CDC7BE; -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;"));
        card.setOnMouseExited(_ -> card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD4; -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: dropshadow(gaussian, rgba(44,41,36,0.06), 4, 0, 0, 1);"));
        HBox titleRow = new HBox(5);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(createMindMapTypeIcon(idea.getType()));
        Label titleLabel = new Label(idea.getTitle());
        titleLabel.setStyle("-fx-text-fill: #C4843C; -fx-font-weight: bold; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleRow.getChildren().add(titleLabel);
        String preview = idea.getContent();
        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
        Label contentLabel = new Label(preview);
        contentLabel.setStyle("-fx-text-fill: #7A746E; -fx-font-size: 12px;"); contentLabel.setWrapText(true);
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        HBox tagsBox = new HBox(3);
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            for (int i = 0; i < Math.min(2, idea.getTags().size()); i++) {
                Tag tag = idea.getTags().get(i);
                Label tagLabel = new Label("#" + tag.getName());
                tagLabel.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 1 4; -fx-background-radius: 8; -fx-font-size: 12px;", tag.getColor() != null ? tag.getColor() : "#4A90E2"));
                tagsBox.getChildren().add(tagLabel);
            }
        }
        HBox starsBox = new HBox(1);
        for (int i = 0; i < 5; i++) { Label star = new Label(i < idea.getImportance() ? "★" : "☆"); star.setStyle("-fx-text-fill: #C4843C; -fx-font-size: 16px;"); starsBox.getChildren().add(star); }
        metaRow.getChildren().addAll(tagsBox, starsBox, createMindMapMoodIcon(idea.getMood()));
        card.getChildren().addAll(titleRow, contentLabel, metaRow);
        setupMindMapCardDrag(card, idea);
        return card;
    }

    private Node createMindMapTypeIcon(Idea.IdeaType type) {
        FontAwesomeSolid icon = switch (type) {
            case IDEA -> FontAwesomeSolid.LIGHTBULB; case QUOTE -> FontAwesomeSolid.QUOTE_LEFT;
            case QUESTION -> FontAwesomeSolid.QUESTION_CIRCLE; case TODO -> FontAwesomeSolid.CHECK_CIRCLE;
            case DISCOVERY -> FontAwesomeSolid.SEARCH; case CONFUSION -> FontAwesomeSolid.QUESTION;
            case HYPOTHESIS -> FontAwesomeSolid.FLASK;
        };
        FontIcon fi = new FontIcon(icon); fi.setIconSize(14); fi.setIconColor(javafx.scene.paint.Color.web("#C4843C"));
        return fi;
    }

    private Node createMindMapMoodIcon(Idea.Mood mood) {
        FontAwesomeSolid icon = switch (mood) {
            case HAPPY -> FontAwesomeSolid.SMILE; case EXCITED -> FontAwesomeSolid.GRIN_STARS;
            case CALM -> FontAwesomeSolid.SMILE_BEAM; case NEUTRAL -> FontAwesomeSolid.MEH;
            case THOUGHTFUL -> FontAwesomeSolid.COMMENT; case CREATIVE -> FontAwesomeSolid.PALETTE;
            case INSPIRED -> FontAwesomeSolid.STAR; case CURIOUS -> FontAwesomeSolid.SEARCH;
            case CONFUSED -> FontAwesomeSolid.QUESTION; case FRUSTRATED -> FontAwesomeSolid.FROWN;
        };
        FontIcon fi = new FontIcon(icon); fi.setIconSize(14); fi.setIconColor(javafx.scene.paint.Color.web("#C4843C"));
        return fi;
    }

    private void setupMindMapCardDrag(Node card, Idea idea) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("idea:" + idea.getId());
            Rectangle dragImage = new Rectangle(120, 60);
            dragImage.setFill(javafx.scene.paint.Color.web("#C4843C", 0.9));
            dragImage.setArcWidth(10); dragImage.setArcHeight(10);
            Text dragText = new Text(idea.getTitle());
            dragText.setFill(javafx.scene.paint.Color.BLACK);
            dragText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            Bounds tb = dragText.getBoundsInLocal();
            dragText.setTranslateX((120 - tb.getWidth()) / 2);
            dragText.setTranslateY((60 + tb.getHeight()) / 2 - 4);
            db.setDragView(new Group(dragImage, dragText).snapshot(null, null));
            db.setContent(content); event.consume();
        });
        card.setOnDragDone(_ -> card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD4; -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px;"));
    }

    @FXML private void handleAddIdeaNodeToMindMap() { addMindMapNode("idea"); }
    @FXML private void handleAddConceptNodeToMindMap() { addMindMapNode("concept"); }
    @FXML private void handleAddExternalNodeToMindMap() { addMindMapNode("external"); }

    private void addMindMapNode(String type) {
        if (mindMapManager == null || mindMapView == null) return;
        TextInputDialog dialog = new TextInputDialog();
        String title = switch (type) {
            case "idea" -> "Add Idea Node"; case "concept" -> "Add Concept Node"; default -> "Add External Link";
        };
        dialog.setTitle(title); dialog.setHeaderText("Create a new " + type + " node"); dialog.setContentText("Node text:");
        dialog.showAndWait().ifPresent(text -> {
            if (text.trim().isEmpty()) return;
            double cx = mindMapPane.getWidth() / 2, cy = mindMapPane.getHeight() / 2;
            if ("external".equals(type)) {
                TextInputDialog urlDialog = new TextInputDialog("https://");
                urlDialog.setTitle("URL"); urlDialog.setContentText("URL:");
                urlDialog.showAndWait().ifPresent(url -> { if (!url.trim().isEmpty()) mindMapManager.createExternalNode(text.trim(), url.trim(), cx, cy); });
            } else {
                mindMapManager.createConceptNode(text.trim(), cx, cy);
            }
            if (mindMapView != null) mindMapView.redraw();
        });
    }

    @FXML private void refreshMindMapIdeaList() {
        Project p = projectManager.getCurrentProject();
        if (p != null) { loadIdeasToMindMapPanel(p); logger.info("Mind map idea list refreshed"); }
    }
}
