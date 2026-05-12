package com.inspiration.catcher.manager;

import com.inspiration.catcher.model.Idea;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages statistics dashboard: charts, metrics, type distribution, weekly trends.
 */
public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);

    private final IdeaManager ideaManager;
    private PieChart typePieChart;
    private BarChart<String, Number> weeklyBarChart;

    public StatisticsManager(IdeaManager ideaManager) {
        this.ideaManager = ideaManager;
    }

    /** Create the type distribution pie chart. */
    public PieChart createTypePieChart() {
        typePieChart = new PieChart();
        typePieChart.setTitle("灵感类型分布");
        typePieChart.setLegendVisible(false);
        typePieChart.setLabelsVisible(true);
        typePieChart.setLabelLineLength(10);
        typePieChart.setStartAngle(90);
        typePieChart.setAnimated(true);
        typePieChart.setPrefHeight(180);
        typePieChart.setStyle("-fx-padding: 4;");
        refreshTypePieChart();
        return typePieChart;
    }

    /** Create the weekly trend bar chart. */
    public BarChart<String, Number> createWeeklyBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("本周");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("条数");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        weeklyBarChart = new BarChart<>(xAxis, yAxis);
        weeklyBarChart.setTitle("本周趋势");
        weeklyBarChart.setLegendVisible(false);
        weeklyBarChart.setAnimated(true);
        weeklyBarChart.setPrefHeight(160);
        weeklyBarChart.setStyle("-fx-padding: 4;");
        refreshWeeklyBarChart();
        return weeklyBarChart;
    }

    /** Refresh all charts with latest data. */
    public void refresh() {
        if (typePieChart != null) refreshTypePieChart();
        if (weeklyBarChart != null) refreshWeeklyBarChart();
    }

    private void refreshTypePieChart() {
        if (typePieChart == null) return;
        List<Idea> ideas = ideaManager.getIdeaList();
        Map<Idea.IdeaType, Long> counts = ideas.stream()
                .collect(Collectors.groupingBy(Idea::getType, Collectors.counting()));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Idea.IdeaType type : Idea.IdeaType.values()) {
            long count = counts.getOrDefault(type, 0L);
            if (count > 0) {
                PieChart.Data slice = new PieChart.Data(type.getDisplayName() + " (" + count + ")", count);
                slice.setName(type.getDisplayName());
                pieData.add(slice);
            }
        }
        typePieChart.setData(pieData);
    }

    private void refreshWeeklyBarChart() {
        if (weeklyBarChart == null) return;
        List<Idea> ideas = ideaManager.getIdeaList();
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();

        // 本周日期范围（周一 ~ 周日）
        java.time.LocalDate monday = java.time.LocalDate.now().minusDays(today.getValue() - 1);
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int[] counts = new int[7];

        for (Idea idea : ideas) {
            if (idea.getCreatedAt() == null) continue;
            java.time.LocalDate d = idea.getCreatedAt().toLocalDate();
            if (!d.isBefore(monday) && !d.isAfter(monday.plusDays(6))) {
                int idx = d.getDayOfWeek().getValue() - 1; // Monday=1 -> index 0
                if (idx >= 0 && idx < 7) counts[idx]++;
            }
        }

        ObservableList<XYChart.Series<String, Number>> barData = FXCollections.observableArrayList();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < 7; i++) {
            series.getData().add(new XYChart.Data<>(dayNames[i], counts[i]));
        }
        barData.add(series);
        weeklyBarChart.setData(barData);
    }
}
