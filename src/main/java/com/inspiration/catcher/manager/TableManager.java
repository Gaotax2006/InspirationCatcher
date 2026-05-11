package com.inspiration.catcher.manager;

import com.inspiration.catcher.controller.MainController;
import com.inspiration.catcher.model.Connection;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.util.DateUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class TableManager {
    private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

    private final TableView<Idea> ideaTableView;
    private final IdeaManager ideaManager;
    private final MainController mainController;
    // 添加排序相关的成员变量
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

    // 支持排序的关键
    private FilteredList<Idea> filteredIdeas;
    private SortedList<Idea> sortedIdeas;

    public TableManager(TableView<Idea> tableView, IdeaManager ideaManager, MainController mainController) {
        this.ideaTableView = tableView;
        this.ideaManager = ideaManager;
        this.mainController = mainController;

        // 初始化列表
        this.originalDataList = FXCollections.observableArrayList();
        this.filteredDataList = new FilteredList<>(originalDataList, _ -> true);
        logger.info("TableManager 构造函数: ideaTableView={}, ideaManager={}",
                tableView != null ? "非空" : "空",
                ideaManager != null ? "非空" : "空");
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
        this.moodColumn = moodColumn;
        this.importanceColumn = importanceColumn;
        this.tagsColumn = tagsColumn;
        this.createdAtColumn = createdAtColumn;
        this.updatedAtColumn = updatedAtColumn;
    }

    public void setDetailPanels(TextArea ideaDetail) {this.ideaDetail = ideaDetail;}
    public void setStatusLabels(Label statusLabel) {
        this.statusLabel = statusLabel;
    }
    // 设置过滤列表
    public void setFilteredIdeas(FilteredList<Idea> filteredIdeas) {
        this.filteredIdeas = filteredIdeas;
        setupSortedList();
    }
    private void setupSortedList() {
        // 创建SortedList并绑定到表格
        sortedIdeas = new SortedList<>(filteredIdeas);
        // 关键：绑定表格的比较器到SortedList的比较器
        sortedIdeas.comparatorProperty().bind(ideaTableView.comparatorProperty());
        // 设置表格数据
        ideaTableView.setItems(sortedIdeas);
    }
    public void setupTable() {
        logger.info("设置表格");

        try {
            // 清空现有数据
            originalDataList.clear();
            // 设置表格列
            if (filteredIdeas != null && sortedIdeas == null) setupSortedList();
            setupTableColumns();
            // 监听 filteredIdeas 变化
            if (filteredIdeas != null) {
                filteredIdeas.addListener((ListChangeListener<Idea>) _ -> Platform.runLater(() -> {
                    ideaTableView.refresh();
                    mainController.updateStatistics();
                }));
            }
            // 设置双击编辑
            setupDoubleClickEdit();
            // 设置列宽自动调整
            setupColumnWidths();
            logger.info("表格设置完成");
        } catch (Exception e) {
            logger.error("设置表格时出错", e);
            showError("表格初始化错误", "表格初始化失败: " + e.getMessage());
        }
    }
    private void setupTableColumns() {
        // 标题列 - 使用PropertyValueFactory
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        // 类型列 - 需要自定义，因为type是枚举
        typeColumn.setCellValueFactory(cellData -> {
            Idea.IdeaType type = cellData.getValue().getType();
            return new SimpleStringProperty(type != null ? type.getDisplayName() : "");
        });
        // 重要性列 - 使用PropertyValueFactory
        importanceColumn.setCellValueFactory(new PropertyValueFactory<>("importance"));
        // 设置重要性列的单元格工厂，显示为星级
        importanceColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Integer importance, boolean empty) {
                super.updateItem(importance, empty);
                if (empty || importance == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // 创建星级显示
                    HBox stars = new HBox(2);
                    for (int i = 1; i <= 5; i++) {
                        Label star = new Label(i <= importance ? "★" : "☆");
                        star.setStyle(i <= importance ?
                                "-fx-font-size: 14px;" :
                                "-fx-text-fill: #CCCCCC; ");
                        stars.getChildren().add(star);
                    }
                    setGraphic(stars);
                    setText(null);
                }
            }
        });
        // 心情列
        moodColumn.setCellValueFactory(cellData -> {
            Idea.Mood mood = cellData.getValue().getMood();
            return new SimpleStringProperty(mood != null ? mood.getDisplayName() : "");
        });
        // 标签列 - 自定义，显示标签名称列表
        tagsColumn.setCellValueFactory(cellData -> {
            try {
                Idea idea = cellData.getValue();
                if (idea == null || idea.getTags() == null || idea.getTags().isEmpty())
                    return new SimpleStringProperty("");
                StringBuilder tags = new StringBuilder();
                for (Tag tag : idea.getTags()) {
                    if (tag != null && tag.getName() != null && !tag.getName().trim().isEmpty()) {
                        if (!tags.isEmpty()) tags.append(", ");
                        tags.append("#").append(tag.getName());
                    }
                }
                return new SimpleStringProperty(tags.toString());
            } catch (Exception e) {return new SimpleStringProperty("");}
        });
        // 创建时间列
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(date != null ? DateUtil.formatDateTime(date) : "");
        });
        // 更新时间列
        updatedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getUpdatedAt();
            return new SimpleStringProperty(date != null ? DateUtil.formatDateTime(date) : "");
        });
    }

    private void setupDoubleClickEdit() {
        ideaTableView.setRowFactory(_ -> {
            TableRow<Idea> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) mainController.handleEditIdea();
            });
            return row;
        });
    }

    private void setupColumnWidths() {
        titleColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.15));
        typeColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.06));
        importanceColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.08));
        moodColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.06));
        tagsColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.25));
        createdAtColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.20));
        updatedAtColumn.prefWidthProperty().bind(ideaTableView.widthProperty().multiply(0.20));
    }

    // 加载数据到表格（支持过滤）
    public void loadDataToTable() {
        try {
            List<Idea> ideas = ideaManager.getIdeaList();
            // 清空原始列表并添加新数据
            if (originalDataList == null) originalDataList = FXCollections.observableArrayList();
            originalDataList.clear();
            originalDataList.addAll(ideas);
            // 强制刷新表格数据绑定
            if (sortedIdeas != null) ideaTableView.setItems(sortedIdeas);
            // 强制刷新表格
            ideaTableView.refresh();
            if (statusLabel != null) statusLabel.setText("已加载 " + ideas.size() + " 条灵感");
            logger.info("表格数据加载完成，共 {} 条记录", ideas.size());
        } catch (Exception e) {
            logger.error("加载数据失败", e);
            showError("加载失败", "无法加载数据: " + e.getMessage());
        }
    }
    // 获取当前过滤状态
    public int getFilteredCount() {return (filteredDataList != null) ? filteredDataList.size() : 0;}
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
        detail.append("   • 项目名称: ").append(mainController.getCurrentProject().getName()).append("\n");
        detail.append("   • 隐私级别: ").append(idea.getPrivacy().getDisplayName()).append("\n\n");
        // 显示标签
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            detail.append(" 标签:\n");
            for (Tag tag : idea.getTags()) detail.append("   #").append(tag.getName()).append(" ");
            detail.append("\n\n");
        }
        // 显示关联
        if (idea.getConnections() != null && !idea.getConnections().isEmpty()) {
            detail.append("🔗 关联灵感:\n");
            for (Connection connection : idea.getConnections()) {
                detail.append("   • ").append(connection.getRelationship().getDisplayName())
                        .append(" (强度: ").append(String.format("%.1f", connection.getStrength() * 100))
                        .append("%)\n");
            }
            detail.append("\n");
        }
        detail.append(" 内容:\n").append(idea.getContent()).append("\n\n");
        ideaDetail.setText(detail.toString());
        ideaDetail.setScrollTop(0); // 滚动到顶部
    }
    public void refreshTable() {
        // 重新加载数据
        loadDataToTable();
        if (statusLabel != null) statusLabel.setText("表格已刷新");
    }
    public void selectAndShowIdea(Idea idea) {
        if (idea == null) return;
        Platform.runLater(() -> {
            try {
                // 在表格中找到这个灵感
                ideaTableView.getSelectionModel().clearSelection();
                // 遍历表格中的所有项
                for (int i = 0; i < ideaTableView.getItems().size(); i++) {
                    Idea item = ideaTableView.getItems().get(i);
                    if (item.getId().equals(idea.getId())) {
                        // 选中这一行
                        ideaTableView.getSelectionModel().select(i);
                        // 滚动到这一行
                        ideaTableView.scrollTo(i);
                        // 显示详情
                        showIdeaDetail(item);
                        break;
                    }
                }
            } catch (Exception e) {logger.error("选中灵感失败", e);}
        });
    }
    // 获取当前选中的灵感
    public Idea getSelectedIdea() {return ideaTableView.getSelectionModel().getSelectedItem();}
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}