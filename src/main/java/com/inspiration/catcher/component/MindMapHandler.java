package com.inspiration.catcher.component;

import com.inspiration.catcher.controller.EditorController;
import com.inspiration.catcher.manager.IdeaManager;
import com.inspiration.catcher.manager.MindMapManager;
import com.inspiration.catcher.manager.ProjectManager;
import com.inspiration.catcher.manager.TableManager;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.MindMapNode;
import com.inspiration.catcher.model.Project;
import com.inspiration.catcher.model.Tag;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles all mind map UI coordination: initialization, sidebar cards,
 * drag-and-drop, node creation, and idea jumping.
 */
public class MindMapHandler {
    private static final Logger logger = LoggerFactory.getLogger(MindMapHandler.class);

    private final Pane mindMapPane;
    private final VBox mindMapIdeaListContainer;
    private final Label statusLabel;
    private final TabPane mainTabPane;
    private final IdeaManager ideaManager;
    private final ProjectManager projectManager;
    private final EditorController editorController;
    private final TableManager tableManager;

    private MindMapView mindMapView;
    private MindMapManager mindMapManager;

    public MindMapHandler(Pane mindMapPane, VBox mindMapIdeaListContainer,
                           Label statusLabel, TabPane mainTabPane,
                           IdeaManager ideaManager, ProjectManager projectManager,
                           EditorController editorController, TableManager tableManager) {
        this.mindMapPane = mindMapPane;
        this.mindMapIdeaListContainer = mindMapIdeaListContainer;
        this.statusLabel = statusLabel;
        this.mainTabPane = mainTabPane;
        this.ideaManager = ideaManager;
        this.projectManager = projectManager;
        this.editorController = editorController;
        this.tableManager = tableManager;
    }

    public void setMindMapManager(MindMapManager manager) {
        this.mindMapManager = manager;
    }

    public MindMapView getMindMapView() { return mindMapView; }

    /** Initialize the mind map canvas and sidebar */
    public void initialize() {
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
            logger.info("Mind map initialized");
        } catch (Exception e) { logger.error("Mind map init failed", e); }
    }

    /** Reload mind map data for current project */
    public void refresh() {
        if (mindMapManager != null) {
            Project p = projectManager.getCurrentProject();
            if (p != null) {
                mindMapManager.loadProjectMindMap(p.getId());
                loadIdeasToMindMapPanel(p);
                statusLabel.setText("Mind map refreshed");
            }
        }
    }

    /** Navigate to an idea in the editor tab */
    public void jumpToIdea(Integer ideaId) {
        if (ideaId == null) return;
        Idea idea = ideaManager.getIdeaById(ideaId);
        if (idea == null) { statusLabel.setText("Idea not found: ID " + ideaId); return; }
        mainTabPane.getSelectionModel().select(2);
        editorController.switchToEditMode(idea);
        tableManager.selectAndShowIdea(idea);
        statusLabel.setText("Jumped to: " + (idea.getTitle() != null ? idea.getTitle() : "untitled"));
    }

    /** Load ideas into the sidebar card list */
    public void loadIdeasToMindMapPanel(Project project) {
        if (project == null || mindMapIdeaListContainer == null) return;
        mindMapIdeaListContainer.getChildren().clear();
        List<Idea> ideas = ideaManager.getIdeasByProject(project.getId());
        if (ideas == null || ideas.isEmpty()) {
            Label empty = new Label("No ideas");
            empty.setStyle("-fx-text-fill: #ADA7A0; -fx-font-style: italic; -fx-padding: 20;");
            mindMapIdeaListContainer.getChildren().add(empty);
            return;
        }
        for (Idea idea : ideas) mindMapIdeaListContainer.getChildren().add(createIdeaCard(idea));
    }

    // === Card rendering ===

    private Node createIdeaCard(Idea idea) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle(cardBaseStyle());
        card.setOnMouseEntered(_ -> card.setStyle(cardHoverStyle()));
        card.setOnMouseExited(_ -> card.setStyle(cardBaseStyle()));

        HBox titleRow = new HBox(5);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(createTypeIcon(idea.getType()));
        Label titleLabel = new Label(idea.getTitle());
        titleLabel.setStyle("-fx-text-fill: #C4843C; -fx-font-weight: bold; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleRow.getChildren().add(titleLabel);

        String preview = idea.getContent();
        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
        Label contentLabel = new Label(preview);
        contentLabel.setStyle("-fx-text-fill: #7A746E; -fx-font-size: 12px;");
        contentLabel.setWrapText(true);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        HBox tagsBox = new HBox(3);
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            for (int i = 0; i < Math.min(2, idea.getTags().size()); i++) {
                Tag tag = idea.getTags().get(i);
                Label tagLabel = new Label("#" + tag.getName());
                tagLabel.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 1 4; -fx-background-radius: 8; -fx-font-size: 12px;",
                        tag.getColor() != null ? tag.getColor() : "#C4843C"));
                tagsBox.getChildren().add(tagLabel);
            }
        }
        HBox starsBox = new HBox(1);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < idea.getImportance() ? "★" : "☆");
            star.setStyle("-fx-text-fill: #C4843C; -fx-font-size: 16px;");
            starsBox.getChildren().add(star);
        }
        metaRow.getChildren().addAll(tagsBox, starsBox, createMoodIcon(idea.getMood()));
        card.getChildren().addAll(titleRow, contentLabel, metaRow);
        setupCardDrag(card, idea);
        return card;
    }

    private Node createTypeIcon(Idea.IdeaType type) {
        FontAwesomeSolid icon = switch (type) {
            case IDEA -> FontAwesomeSolid.LIGHTBULB; case QUOTE -> FontAwesomeSolid.QUOTE_LEFT;
            case QUESTION -> FontAwesomeSolid.QUESTION_CIRCLE; case TODO -> FontAwesomeSolid.CHECK_CIRCLE;
            case DISCOVERY -> FontAwesomeSolid.SEARCH; case CONFUSION -> FontAwesomeSolid.QUESTION;
            case HYPOTHESIS -> FontAwesomeSolid.FLASK;
        };
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(Color.web("#C4843C"));
        return fi;
    }

    private Node createMoodIcon(Idea.Mood mood) {
        FontAwesomeSolid icon = switch (mood) {
            case HAPPY -> FontAwesomeSolid.SMILE; case EXCITED -> FontAwesomeSolid.GRIN_STARS;
            case CALM -> FontAwesomeSolid.SMILE_BEAM; case NEUTRAL -> FontAwesomeSolid.MEH;
            case THOUGHTFUL -> FontAwesomeSolid.COMMENT; case CREATIVE -> FontAwesomeSolid.PALETTE;
            case INSPIRED -> FontAwesomeSolid.STAR; case CURIOUS -> FontAwesomeSolid.SEARCH;
            case CONFUSED -> FontAwesomeSolid.QUESTION; case FRUSTRATED -> FontAwesomeSolid.FROWN;
        };
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(Color.web("#C4843C"));
        return fi;
    }

    private void setupCardDrag(Node card, Idea idea) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("idea:" + idea.getId());
            Rectangle dragImage = new Rectangle(120, 60);
            dragImage.setFill(Color.web("#C4843C", 0.9));
            dragImage.setArcWidth(10); dragImage.setArcHeight(10);
            Text dragText = new Text(idea.getTitle());
            dragText.setFill(Color.BLACK);
            dragText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            Bounds tb = dragText.getBoundsInLocal();
            dragText.setTranslateX((120 - tb.getWidth()) / 2);
            dragText.setTranslateY((60 + tb.getHeight()) / 2 - 4);
            db.setDragView(new Group(dragImage, dragText).snapshot(null, null));
            db.setContent(content); event.consume();
        });
        card.setOnDragDone(_ -> card.setStyle(cardBaseStyle()));
    }

    // === Node creation commands ===

    @SuppressWarnings("unused")
    public void addIdeaNode() { addMindMapNode("idea"); }
    public void addConceptNode() { addMindMapNode("concept"); }
    public void addExternalNode() { addMindMapNode("external"); }

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
                urlDialog.showAndWait().ifPresent(url -> {
                    if (!url.trim().isEmpty()) mindMapManager.createExternalNode(text.trim(), url.trim(), cx, cy);
                });
            } else {
                mindMapManager.createConceptNode(text.trim(), cx, cy);
            }
            if (mindMapView != null) mindMapView.redraw();
        });
    }

    /** Apply tree auto-layout to organize the mind map */
    public void applyAutoLayout() {
        if (mindMapManager != null) {
            mindMapManager.applyTreeLayout();
            if (mindMapView != null) mindMapView.redraw();
            statusLabel.setText("Auto-layout applied");
        }
    }

    /** Export mind map as PNG image */
    public void exportImage() {
        if (mindMapView != null) mindMapView.exportToImage();
    }

    public void refreshIdeaList() {
        Project p = projectManager.getCurrentProject();
        if (p != null) { loadIdeasToMindMapPanel(p); logger.info("Mind map idea list refreshed"); }
    }

    // === Card styles ===

    private static String cardBaseStyle() {
        return "-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD4; -fx-border-width: 1; " +
               "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
               "-fx-effect: dropshadow(gaussian, rgba(44,41,36,0.06), 4, 0, 0, 1);";
    }

    private static String cardHoverStyle() {
        return "-fx-background-color: #EBE6DE; -fx-border-color: #CDC7BE; -fx-border-width: 1; " +
               "-fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;";
    }
}
