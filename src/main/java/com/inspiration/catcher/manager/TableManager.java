package com.inspiration.catcher.manager;

import com.inspiration.catcher.controller.MainController;
import com.inspiration.catcher.model.Connection;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.util.DateUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TableManager {
    private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

    private final TableView<Idea> ideaTableView;
    private final IdeaManager ideaManager;
    private final MainController mainController;

    private ObservableList<Idea> originalDataList;
    private final FilteredList<Idea> filteredDataList;

    private TableColumn<Idea, String> titleColumn;
    private TableColumn<Idea, String> typeColumn;
    private TableColumn<Idea, Integer> importanceColumn;
    private TableColumn<Idea, String> moodColumn;
    private TableColumn<Idea, String> tagsColumn;
    private TableColumn<Idea, String> createdAtColumn;
    private TableColumn<Idea, String> updatedAtColumn;

    private TextArea ideaDetail;
    private Label statusLabel;

    private FilteredList<Idea> filteredIdeas;
    private SortedList<Idea> sortedIdeas;

    /** Static keyword for search highlighting used by TitleCell. */
    public static String highlightKeyword = "";

    public TableManager(TableView<Idea> tableView, IdeaManager ideaManager, MainController mainController) {
        this.ideaTableView = tableView;
        this.ideaManager = ideaManager;
        this.mainController = mainController;
        this.originalDataList = FXCollections.observableArrayList();
        this.filteredDataList = new FilteredList<>(originalDataList, _ -> true);
        logger.info("TableManager initialized");
    }

    public void setTableColumns(TableColumn<Idea, String> titleColumn,
                                TableColumn<Idea, String> typeColumn,
                                TableColumn<Idea, Integer> importanceColumn,
                                TableColumn<Idea, String> moodColumn,
                                TableColumn<Idea, String> tagsColumn,
                                TableColumn<Idea, String> createdAtColumn,
                                TableColumn<Idea, String> updatedAtColumn) {
        this.titleColumn = titleColumn;
        this.typeColumn = typeColumn;
        this.importanceColumn = importanceColumn;
        this.moodColumn = moodColumn;
        this.tagsColumn = tagsColumn;
        this.createdAtColumn = createdAtColumn;
        this.updatedAtColumn = updatedAtColumn;
    }

    public void setDetailPanels(TextArea ideaDetail) { this.ideaDetail = ideaDetail; }
    public void setStatusLabels(Label statusLabel) { this.statusLabel = statusLabel; }

    public void setFilteredIdeas(FilteredList<Idea> filteredIdeas) {
        this.filteredIdeas = filteredIdeas;
        setupSortedList();
    }

    private void setupSortedList() {
        sortedIdeas = new SortedList<>(filteredIdeas);
        sortedIdeas.comparatorProperty().bind(ideaTableView.comparatorProperty());
        ideaTableView.setItems(sortedIdeas);
    }

    public void setupTable() {
        logger.info("Setting up enhanced table");
        try {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            Label emptyTitle = new Label("还没有灵感");
            emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-text-tertiary;");
            Label emptyHint = new Label("点击「新建」或按 Ctrl+N 开始记录\n也可以使用 Ctrl+Q 快速捕捉");
            emptyHint.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-tertiary;");
            emptyHint.setTextAlignment(TextAlignment.CENTER);
            emptyState.getChildren().addAll(emptyTitle, emptyHint);
            ideaTableView.setPlaceholder(emptyState);

            originalDataList.clear();
            if (filteredIdeas != null && sortedIdeas == null) setupSortedList();
            setupColumns();

            if (filteredIdeas != null) {
                filteredIdeas.addListener((ListChangeListener<Idea>) _ -> Platform.runLater(() -> {
                    ideaTableView.refresh();
                    mainController.updateStatistics();
                }));
            }

            ideaTableView.setRowFactory(_ -> {
                TableRow<Idea> row = new TableRow<>();
                row.selectedProperty().addListener((_, _, sel) -> {
                    if (sel && row.getItem() != null) showIdeaDetail(row.getItem());
                });
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) mainController.handleEditIdea();
                });
                return row;
            });

            logger.info("Enhanced table setup complete");
        } catch (Exception e) {
            logger.error("Failed to setup table", e);
        }
    }

    private void setupColumns() {
        // Title: type icon + title text + optional highlight + content preview
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setCellFactory(_ -> new TableCell<>() {
            private final HBox box = new HBox(6);
            { box.setAlignment(Pos.CENTER_LEFT); }
            @Override
            protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                if (empty) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null) { setGraphic(null); return; }
                box.getChildren().clear();
                // Type icon
                FontIcon icon = new FontIcon(switch (idea.getType()) {
                    case IDEA -> FontAwesomeSolid.LIGHTBULB; case QUOTE -> FontAwesomeSolid.QUOTE_LEFT;
                    case QUESTION -> FontAwesomeSolid.QUESTION_CIRCLE; case TODO -> FontAwesomeSolid.CHECK_CIRCLE;
                    case DISCOVERY -> FontAwesomeSolid.SEARCH; case CONFUSION -> FontAwesomeSolid.QUESTION;
                    case HYPOTHESIS -> FontAwesomeSolid.FLASK;
                });
                icon.setIconSize(14);
                icon.setIconColor(Color.web(getTypeColor(idea.getType())));
                box.getChildren().add(icon);
                // Title with highlight
                String text = title != null && !title.isEmpty() ? title : "无标题";
                Label label = new Label(text);
                label.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
                if (highlightKeyword != null && !highlightKeyword.isEmpty()
                        && text.toLowerCase().contains(highlightKeyword.toLowerCase())) {
                    label.setStyle(label.getStyle() + " -fx-text-fill: -fx-primary;");
                }
                box.getChildren().add(label);
                setGraphic(box);
            }
        });

        // Type: colored badge
        typeColumn.setCellValueFactory(p -> {
            Idea.IdeaType t = p.getValue().getType();
            return new javafx.beans.property.SimpleStringProperty(t != null ? t.getDisplayName() : "");
        });
        typeColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null) { setGraphic(null); return; }
                String color = getTypeColor(idea.getType());
                Label badge = new Label(item);
                badge.setStyle(String.format(
                    "-fx-background-color: %s18; -fx-text-fill: %s; -fx-padding: 1 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 500;", color, color));
                setGraphic(badge);
            }
        });

        // Importance: stars
        importanceColumn.setCellValueFactory(new PropertyValueFactory<>("importance"));
        importanceColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Integer imp, boolean empty) {
                super.updateItem(imp, empty);
                if (empty || imp == null) { setGraphic(null); return; }
                HBox stars = new HBox(1);
                for (int i = 1; i <= 5; i++) {
                    Label s = new Label(i <= imp ? "★" : "☆");
                    s.setStyle(i <= imp ? "-fx-text-fill: #E8A838; -fx-font-size: 13px;" : "-fx-text-fill: #D0C8C0; -fx-font-size: 13px;");
                    stars.getChildren().add(s);
                }
                setGraphic(stars);
            }
        });

        // Mood: icon
        moodColumn.setCellValueFactory(p -> {
            Idea.Mood m = p.getValue().getMood();
            return new javafx.beans.property.SimpleStringProperty(m != null ? m.getDisplayName() : "");
        });
        moodColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null || idea.getMood() == null) { setGraphic(null); return; }
                FontIcon icon = new FontIcon(switch (idea.getMood()) {
                    case HAPPY -> FontAwesomeSolid.SMILE; case EXCITED -> FontAwesomeSolid.GRIN_STARS;
                    case CALM -> FontAwesomeSolid.SMILE_BEAM; case NEUTRAL -> FontAwesomeSolid.MEH;
                    case THOUGHTFUL -> FontAwesomeSolid.COMMENT; case CREATIVE -> FontAwesomeSolid.PALETTE;
                    case INSPIRED -> FontAwesomeSolid.STAR; case CURIOUS -> FontAwesomeSolid.SEARCH;
                    case CONFUSED -> FontAwesomeSolid.QUESTION; case FRUSTRATED -> FontAwesomeSolid.FROWN;
                });
                icon.setIconSize(14);
                icon.setIconColor(Color.web("#7A746E"));
                setGraphic(icon);
            }
        });

        // Tags: colored chips
        tagsColumn.setCellValueFactory(p -> {
            Idea idea = p.getValue();
            if (idea.getTags() == null || idea.getTags().isEmpty()) return new javafx.beans.property.SimpleStringProperty("");
            StringBuilder sb = new StringBuilder();
            for (Tag t : idea.getTags()) { if (!sb.isEmpty()) sb.append(", "); sb.append("#").append(t.getName()); }
            return new javafx.beans.property.SimpleStringProperty(sb.toString());
        });
        tagsColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null || idea.getTags() == null || idea.getTags().isEmpty()) { setGraphic(null); return; }
                HBox chips = new HBox(3);
                for (Tag t : idea.getTags()) {
                    String c = t.getColor() != null ? t.getColor() : "#C4843C";
                    Label chip = new Label("#" + t.getName());
                    chip.setStyle(String.format("-fx-background-color: %1$s18; -fx-text-fill: %1$s; -fx-padding: 0 6; -fx-background-radius: 8; -fx-font-size: 11px;", c));
                    chips.getChildren().add(chip);
                }
                setGraphic(chips);
            }
        });

        // CreatedAt: relative time
        createdAtColumn.setCellValueFactory(p -> {
            LocalDateTime d = p.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(d != null ? DateUtil.formatDateTime(d) : "");
        });
        createdAtColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null || idea.getCreatedAt() == null) { setGraphic(null); return; }
                String rel = formatRelative(idea.getCreatedAt());
                Label label = new Label(rel);
                label.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-tertiary;");
                setGraphic(label);
            }
        });

        // UpdatedAt: relative time
        updatedAtColumn.setCellValueFactory(p -> {
            LocalDateTime d = p.getValue().getUpdatedAt();
            return new javafx.beans.property.SimpleStringProperty(d != null ? DateUtil.formatDateTime(d) : "");
        });
        updatedAtColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Idea idea = getTableView().getItems().get(getIndex());
                if (idea == null || idea.getUpdatedAt() == null) { setGraphic(null); return; }
                String rel = formatRelative(idea.getUpdatedAt());
                Label label = new Label(rel);
                label.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-tertiary;");
                setGraphic(label);
            }
        });
    }

    // === Data operations ===

    public void loadDataToTable() {
        try {
            List<Idea> ideas = ideaManager.getIdeaList();
            if (originalDataList == null) originalDataList = FXCollections.observableArrayList();
            originalDataList.clear();
            originalDataList.addAll(ideas);
            if (sortedIdeas != null) ideaTableView.setItems(sortedIdeas);
            ideaTableView.refresh();
            if (statusLabel != null) statusLabel.setText("已加载 " + ideas.size() + " 条灵感");
            logger.debug("Loaded {} ideas", ideas.size());
        } catch (Exception e) { logger.error("Failed to load data", e); }
    }

    public int getFilteredCount() { return filteredDataList != null ? filteredDataList.size() : 0; }

    public void showIdeaDetail(Idea idea) {
        if (ideaDetail == null || idea == null) return;
        StringBuilder d = new StringBuilder();
        d.append(" 标题: ").append(idea.getTitle()).append("\n\n");
        d.append(" 基本信息:\n");
        d.append("   • 类型: ").append(idea.getType().getDisplayName()).append("\n");
        d.append("   • 重要性: ").append("★".repeat(idea.getImportance())).append("☆".repeat(5 - idea.getImportance())).append("\n");
        d.append("   • 心情: ").append(idea.getMood().getDisplayName()).append("\n");
        d.append("   • 创建: ").append(DateUtil.formatDateTime(idea.getCreatedAt())).append("\n");
        d.append("   • 更新: ").append(DateUtil.formatDateTime(idea.getUpdatedAt())).append("\n");
        d.append("   • 隐私: ").append(idea.getPrivacy().getDisplayName()).append("\n\n");
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            d.append(" 标签:\n");
            for (Tag t : idea.getTags()) d.append("   #").append(t.getName()).append(" ");
            d.append("\n\n");
        }
        if (idea.getConnections() != null && !idea.getConnections().isEmpty()) {
            d.append(" 关联:\n");
            for (Connection c : idea.getConnections()) {
                d.append("   • ").append(c.getRelationship().getDisplayName())
                  .append(" (").append(String.format("%.0f", c.getStrength() * 100)).append("%)\n");
            }
            d.append("\n");
        }
        d.append(" 内容:\n").append(idea.getContent()).append("\n\n");
        ideaDetail.setText(d.toString());
        ideaDetail.setScrollTop(0);
    }

    public void refreshTable() {
        loadDataToTable();
        if (statusLabel != null) statusLabel.setText("表格已刷新");
    }

    public void selectAndShowIdea(Idea idea) {
        if (idea == null) return;
        Platform.runLater(() -> {
            ideaTableView.getSelectionModel().clearSelection();
            for (int i = 0; i < ideaTableView.getItems().size(); i++) {
                Idea item = ideaTableView.getItems().get(i);
                if (item.getId().equals(idea.getId())) {
                    ideaTableView.getSelectionModel().select(i);
                    ideaTableView.scrollTo(i);
                    showIdeaDetail(item);
                    break;
                }
            }
        });
    }

    public Idea getSelectedIdea() { return ideaTableView.getSelectionModel().getSelectedItem(); }

    // === Utility ===

    private String getTypeColor(Idea.IdeaType type) {
        return switch (type) {
            case IDEA -> "#C4843C"; case QUOTE -> "#5B7FAF"; case QUESTION -> "#8B6FAF";
            case TODO -> "#5B8C5A"; case DISCOVERY -> "#C4A84C"; case CONFUSION -> "#C45656";
            case HYPOTHESIS -> "#C4843C";
        };
    }

    private String formatRelative(LocalDateTime dt) {
        if (dt == null) return "";
        long m = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (m < 1) return "刚刚";
        if (m < 60) return m + "分钟前";
        long h = ChronoUnit.HOURS.between(dt, LocalDateTime.now());
        if (h < 24) return h + "小时前";
        long d = ChronoUnit.DAYS.between(dt, LocalDateTime.now());
        if (d < 7) return d + "天前";
        return DateUtil.formatDate(dt);
    }
}
