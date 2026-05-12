package com.inspiration.catcher.manager;

import com.inspiration.catcher.component.IdeaCardCell;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TableManager {
    private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

    private final TableView<Idea> ideaTableView;
    private final IdeaManager ideaManager;
    private final MainController mainController;

    private ObservableList<Idea> originalDataList;
    private final FilteredList<Idea> filteredDataList;

    // 卡片列（单列模式）
    private TableColumn<Idea, Idea> cardColumn;
    private TextArea ideaDetail;
    private Label statusLabel;

    private FilteredList<Idea> filteredIdeas;
    private SortedList<Idea> sortedIdeas;

    // 搜索高亮关键词
    private String highlightKeyword = "";

    public TableManager(TableView<Idea> tableView, IdeaManager ideaManager, MainController mainController) {
        this.ideaTableView = tableView;
        this.ideaManager = ideaManager;
        this.mainController = mainController;
        this.originalDataList = FXCollections.observableArrayList();
        this.filteredDataList = new FilteredList<>(originalDataList, _ -> true);
        logger.info("TableManager initialized");
    }

    public void setCardColumn(TableColumn<Idea, Idea> column) {
        this.cardColumn = column;
    }

    public void setDetailPanels(TextArea ideaDetail) {
        this.ideaDetail = ideaDetail;
    }

    public void setStatusLabels(Label statusLabel) {
        this.statusLabel = statusLabel;
    }

    public void setFilteredIdeas(FilteredList<Idea> filteredIdeas) {
        this.filteredIdeas = filteredIdeas;
        setupSortedList();
    }

    public void setHighlightKeyword(String keyword) {
        this.highlightKeyword = keyword != null ? keyword : "";
    }

    private void setupSortedList() {
        sortedIdeas = new SortedList<>(filteredIdeas);
        sortedIdeas.comparatorProperty().bind(ideaTableView.comparatorProperty());
        ideaTableView.setItems(sortedIdeas);
    }

    @SuppressWarnings("unchecked")
    public void setupTable() {
        logger.info("Setting up card-style table");
        try {
            // Empty state
            Label emptyTitle = new Label("还没有灵感");
            emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-text-tertiary;");
            Label emptyHint = new Label("点击「新建」或按 Ctrl+N 开始记录\n也可以使用 Ctrl+Q 快速捕捉");
            emptyHint.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-tertiary;");
            emptyHint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            javafx.scene.layout.VBox emptyState = new javafx.scene.layout.VBox(8);
            emptyState.setAlignment(javafx.geometry.Pos.CENTER);
            emptyState.getChildren().addAll(emptyTitle, emptyHint);
            ideaTableView.setPlaceholder(emptyState);

            originalDataList.clear();
            if (filteredIdeas != null && sortedIdeas == null) setupSortedList();
            setupCardColumn();
            setupRowStyling();
            if (filteredIdeas != null) {
                filteredIdeas.addListener((ListChangeListener<Idea>) _ -> Platform.runLater(() -> {
                    ideaTableView.refresh();
                    mainController.updateStatistics();
                }));
            }
            logger.info("Card-style table setup complete");
        } catch (Exception e) {
            logger.error("Failed to setup table", e);
        }
    }

    private void setupCardColumn() {
        if (cardColumn == null) return;

        // 卡片列填充整个表格宽度
        cardColumn.prefWidthProperty().bind(ideaTableView.widthProperty().subtract(4));
        cardColumn.setResizable(false);
        cardColumn.setSortable(false);

        // 设置 CellValueFactory（列类型为 Idea，直接返回自身）
        cardColumn.setCellValueFactory(p -> {
            Idea idea = p.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(idea);
        });

        // 使用 IdeaCardCell 渲染
        cardColumn.setCellFactory(_ -> new IdeaCardCell());
    }

    private void setupRowStyling() {
        ideaTableView.setRowFactory(_ -> {
            TableRow<Idea> row = new TableRow<Idea>() {
                @Override
                protected void updateItem(Idea idea, boolean empty) {
                    super.updateItem(idea, empty);
                }
            };
            // 选中时更新详情面板
            row.selectedProperty().addListener((_, _, selected) -> {
                if (selected && row.getItem() != null) {
                    showIdeaDetail(row.getItem());
                }
            });
            // 双击编辑
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mainController.handleEditIdea();
                }
            });
            return row;
        });
    }

    // === Data Loading ===

    public void loadDataToTable() {
        try {
            List<Idea> ideas = ideaManager.getIdeaList();
            if (originalDataList == null) originalDataList = FXCollections.observableArrayList();
            originalDataList.clear();
            originalDataList.addAll(ideas);
            if (sortedIdeas != null) ideaTableView.setItems(sortedIdeas);
            ideaTableView.refresh();
            if (statusLabel != null) statusLabel.setText("已加载 " + ideas.size() + " 条灵感");
            logger.info("Loaded {} ideas to table", ideas.size());
        } catch (Exception e) {
            logger.error("Failed to load data", e);
        }
    }

    public int getFilteredCount() {
        return (filteredDataList != null) ? filteredDataList.size() : 0;
    }

    public void showIdeaDetail(Idea idea) {
        if (ideaDetail == null || idea == null) return;
        StringBuilder detail = new StringBuilder();
        detail.append(" 标题: ").append(idea.getTitle()).append("\n\n");
        detail.append(" 基本信息:\n");
        detail.append("   • 类型: ").append(idea.getType().getDisplayName()).append("\n");
        detail.append("   • 重要性: ").append("★".repeat(idea.getImportance()))
                .append("☆".repeat(5 - idea.getImportance())).append("\n");
        detail.append("   • 心情: ").append(idea.getMood().getDisplayName()).append("\n");
        detail.append("   • 创建时间: ").append(DateUtil.formatDateTime(idea.getCreatedAt())).append("\n");
        detail.append("   • 更新时间: ").append(DateUtil.formatDateTime(idea.getUpdatedAt())).append("\n");
        detail.append("   • 隐私级别: ").append(idea.getPrivacy().getDisplayName()).append("\n\n");
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            detail.append(" 标签:\n");
            for (Tag tag : idea.getTags()) detail.append("   #").append(tag.getName()).append(" ");
            detail.append("\n\n");
        }
        if (idea.getConnections() != null && !idea.getConnections().isEmpty()) {
            detail.append(" 关联灵感:\n");
            for (Connection conn : idea.getConnections()) {
                detail.append("   • ").append(conn.getRelationship().getDisplayName())
                        .append(" (强度: ").append(String.format("%.1f", conn.getStrength() * 100))
                        .append("%)\n");
            }
            detail.append("\n");
        }
        detail.append(" 内容:\n").append(idea.getContent()).append("\n\n");
        ideaDetail.setText(detail.toString());
        ideaDetail.setScrollTop(0);
    }

    public void refreshTable() {
        loadDataToTable();
        if (statusLabel != null) statusLabel.setText("表格已刷新");
    }

    public void selectAndShowIdea(Idea idea) {
        if (idea == null) return;
        Platform.runLater(() -> {
            try {
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
            } catch (Exception e) { logger.error("Failed to select idea", e); }
        });
    }

    public Idea getSelectedIdea() {
        return ideaTableView.getSelectionModel().getSelectedItem();
    }
}
