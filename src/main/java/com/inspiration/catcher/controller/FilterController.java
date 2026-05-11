package com.inspiration.catcher.controller;

import com.inspiration.catcher.manager.IdeaManager;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Tag;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FilterController {
    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);

    private final TextField searchField;
    private final ComboBox<String> typeFilter;
    private final ComboBox<Integer> importanceFilter;
    private final DatePicker startDateFilter;
    private final DatePicker endDateFilter;
    private final ComboBox<String> tagFilter;
    private final ComboBox<String> moodFilter;

    private final IdeaManager ideaManager;
    private final MainController mainController;

    private FilteredList<Idea> filteredIdeas;
    private String currentSearchText = "";
    private String currentTypeFilter = "所有类型";
    private String currentImportanceFilter = "所有级别";
    private String currentMoodFilter = "所有心情";
    private LocalDate currentStartDate = null;
    private LocalDate currentEndDate = null;

    public FilterController(TextField searchField,
                            ComboBox<String> typeFilter,
                            ComboBox<Integer> importanceFilter,
                            DatePicker startDateFilter,
                            DatePicker endDateFilter,
                            ComboBox<String> tagFilter,
                            ComboBox<String> moodFilter,
                            IdeaManager ideaManager,
                            MainController mainController) {
        this.searchField = searchField;
        this.typeFilter = typeFilter;
        this.importanceFilter = importanceFilter;
        this.startDateFilter = startDateFilter;
        this.endDateFilter = endDateFilter;
        this.tagFilter = tagFilter;
        this.moodFilter = moodFilter;
        this.ideaManager = ideaManager;
        this.mainController = mainController;
    }

    public void setupFilters() {
        logger.info("设置过滤器");
        // 设置标签筛选器选项
        updateTagFilterOptions();
        tagFilter.setValue("全部标签");
        // 初始化过滤列表
        ObservableList<Idea> sourceList = ideaManager.getIdeaList();
        filteredIdeas = new FilteredList<>(sourceList, _ -> true);
        // 设置类型过滤器选项
        typeFilter.getItems().add("所有类型");
        for (Idea.IdeaType type : Idea.IdeaType.values())
            typeFilter.getItems().add(type.getDisplayName());
        // 设置重要性过滤器选项
        importanceFilter.getItems().addAll(-1, 1, 2, 3, 4, 5);
        importanceFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {return value == -1 ? "所有级别" : value + "星";}
            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        importanceFilter.setValue(-1); // 默认选择"所有级别"

        moodFilter.getItems().add("所有心情");
        for (Idea.Mood mood : Idea.Mood.values())
            moodFilter.getItems().add(mood.getDisplayName());
        moodFilter.setValue("所有心情");

        // 初始化过滤条件
        currentTypeFilter = "所有类型";
        currentImportanceFilter = "所有级别";
        currentMoodFilter = "所有心情";
        currentStartDate = null;
        currentEndDate = null;
    }

    // 更新标签筛选器选项
    private void updateTagFilterOptions() {
        tagFilter.getItems().clear();
        tagFilter.getItems().add("全部标签");
        // 收集所有不重复的标签
        List<String> allTags = ideaManager.getIdeaList().stream()
                .flatMap(idea -> idea.getTags().stream())
                .map(Tag::getName)
                .distinct()
                .sorted()
                .toList();
        tagFilter.getItems().addAll(allTags);
    }

    public void setupEventHandlers() {
        // 搜索框监听
        searchField.textProperty().addListener((_, _, newValue) -> {
            currentSearchText = newValue != null ? newValue.toLowerCase() : "";
            updateFilterPredicate();
        });
        // 类型过滤器监听
        typeFilter.valueProperty().addListener((_, _, newValue) -> {
            currentTypeFilter = newValue != null ? newValue : "所有类型";
            updateFilterPredicate();
        });
        // 重要性过滤器监听
        importanceFilter.valueProperty().addListener((_, _, newValue) -> {
            currentImportanceFilter = newValue != null && newValue != -1 ? String.valueOf(newValue) : "所有级别";
            updateFilterPredicate();
        });
        // 日期范围过滤器监听
        startDateFilter.valueProperty().addListener((_, _, newValue) -> {
            currentStartDate = newValue;
            updateFilterPredicate();
        });
        endDateFilter.valueProperty().addListener((_, _, newValue) -> {
            currentEndDate = newValue;
            updateFilterPredicate();
        });
        // 心情过滤器监听
        moodFilter.valueProperty().addListener((_, _, newValue) -> {
            currentMoodFilter = newValue != null ? newValue : "所有心情";
            updateFilterPredicate();
        });
        // 标签过滤器监听
        tagFilter.valueProperty().addListener((_, _, _) -> updateFilterPredicate());
    }

    public void updateFilterPredicate() {
        filteredIdeas.setPredicate(idea -> {
            // 检查搜索条件
            boolean matchesSearch = true;
            if (!currentSearchText.isEmpty()) {
                matchesSearch =
                        (idea.getTitle() != null && idea.getTitle().toLowerCase().contains(currentSearchText)) ||
                                (idea.getContent() != null && idea.getContent().toLowerCase().contains(currentSearchText)) ||
                                (idea.getTags() != null && idea.getTags().stream()
                                        .anyMatch(tag -> tag.getName() != null && tag.getName().toLowerCase().contains(currentSearchText)));
            }
            // 检查类型过滤条件
            boolean matchesType = true;
            if (!currentTypeFilter.equals("所有类型")) {
                matchesType = (idea.getType() != null &&
                        idea.getType().getDisplayName().equals(currentTypeFilter));
            }
            // 检查重要性过滤条件
            boolean matchesImportance = true;
            if (!currentImportanceFilter.equals("所有级别")) {
                int importanceLevel = Integer.parseInt(currentImportanceFilter);
                matchesImportance = (idea.getImportance() == importanceLevel);
            }
            // 标签筛选
            String selectedTag = tagFilter.getValue();
            if (selectedTag != null && !selectedTag.equals("全部标签")) {
                boolean hasTag = idea.getTags().stream().anyMatch(tag -> tag.getName().equals(selectedTag));
                if (!hasTag) return false;
            }
            // 检查日期范围过滤条件
            if (currentStartDate != null || currentEndDate != null) {
                LocalDateTime createdAt = idea.getCreatedAt();
                if (createdAt != null) {
                    LocalDate ideaDate = createdAt.toLocalDate();
                    if (currentStartDate != null && ideaDate.isBefore(currentStartDate)) return false;
                    if (currentEndDate != null && ideaDate.isAfter(currentEndDate)) return false;
                }
            }
            // 检查心情过滤条件
            boolean matchesMood = true;
            if (!currentMoodFilter.equals("所有心情"))
                matchesMood = (idea.getMood() != null && idea.getMood().getDisplayName().equals(currentMoodFilter));
            // 确保只显示当前项目的灵感
            Integer currentProjectId = mainController.getCurrentProject().getId();
            // 所有条件必须同时满足
            return matchesSearch && matchesType && matchesImportance && matchesMood && idea.getProjectId() != null && idea.getProjectId().equals(currentProjectId);
        });
        // 更新状态栏
        updateFilterStatus();
        // 应用过滤条件
        if (filteredIdeas != null) {
            // 关键修改：通知表格刷新
            Platform.runLater(() -> {
                if (mainController.getIdeaTableView() != null) {
                    // 1. 清除表格选择
                    mainController.getIdeaTableView().getSelectionModel().clearSelection();
                    // 2. 重新设置表格数据源
                    if (filteredIdeas != null) {
                        mainController.getIdeaTableView().setItems(null);
                        SortedList<Idea> newSortedList = new SortedList<>(filteredIdeas);
                        newSortedList.comparatorProperty().bind(mainController.getIdeaTableView().comparatorProperty());
                        // 设置到表格
                        mainController.getIdeaTableView().setItems(newSortedList);
                    }
                    // 3. 刷新表格
                    mainController.getIdeaTableView().refresh();
                    // 4. 强制重新布局
                    mainController.getIdeaTableView().requestLayout();
                }
            });
        }
    }
    private void updateFilterStatus() {
        if (mainController.getStatusLabel() == null) return;
        int filteredCount = filteredIdeas.size();
        int totalCount = ideaManager.getIdeaCount();
        mainController.getStatusLabel().setText(filteredCount == totalCount ? "显示所有 " + totalCount + " 条灵感" : "显示 " + filteredCount + " 条（共 " + totalCount + " 条）");
    }
    public void resetAllFilters() {
        searchField.clear();
        typeFilter.setValue("所有类型");
        importanceFilter.setValue(-1);
        startDateFilter.setValue(null);
        endDateFilter.setValue(null);
        tagFilter.setValue("全部标签");
        moodFilter.setValue("所有心情");

        currentSearchText = "";
        currentTypeFilter = "所有类型";
        currentImportanceFilter = "所有级别";
        currentStartDate = null;
        currentEndDate = null;
        currentMoodFilter = "所有心情";

        updateFilterPredicate();
    }

    // 新增：获取过滤列表
    public FilteredList<Idea> getFilteredIdeas() {
        return filteredIdeas;
    }
    // 更新标签选项（当添加新标签时调用）
    public void refreshTagOptions() {updateTagFilterOptions();}
    // 刷新
    public void refreshDataSource() {
        // 重新创建过滤列表
        filteredIdeas = new FilteredList<>(ideaManager.getIdeaList(), _ -> true);
        updateFilterPredicate(); // 重新应用过滤条件
    }
}