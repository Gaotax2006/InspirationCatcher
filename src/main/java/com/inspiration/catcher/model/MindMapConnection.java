package com.inspiration.catcher.model;

import java.time.LocalDateTime;

public class MindMapConnection {
    private Integer id;
    private Integer projectId;
    private Integer sourceNodeId;
    private Integer targetNodeId;
    private ConnectionType connectionType = ConnectionType.RELATED;
    private String label;
    private String color = "#666666";
    private Integer width = 2;
    private ConnectionStyle style = ConnectionStyle.SOLID;
    private double strength = 0.5;
    private LocalDateTime createdAt;

    // 枚举类型
    public enum ConnectionType {
        RELATED("相关"),
        DEPENDS_ON("依赖"),
        EXTENDS("扩展"),
        CONTRADICTS("矛盾"),
        ANALOGY("类比"),
        CAUSAL("因果"),
        USES("使用");

        private final String displayName;
        ConnectionType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ConnectionStyle {
        SOLID("实线"),
        DASHED("虚线"),
        DOTTED("点线");
        private final String displayName;
        ConnectionStyle(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // 构造方法
    public MindMapConnection() {this.createdAt = LocalDateTime.now();}
//    // Getters and Setters
    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}
    public Integer getProjectId() {return projectId;}
    public void setProjectId(Integer projectId) {this.projectId = projectId;}
    public Integer getSourceNodeId() {return sourceNodeId;}
    public void setSourceNodeId(Integer sourceNodeId) {this.sourceNodeId = sourceNodeId;}
    public Integer getTargetNodeId() {return targetNodeId;}
    public void setTargetNodeId(Integer targetNodeId) {this.targetNodeId = targetNodeId;}
    public ConnectionType getConnectionType() {return connectionType;}
    public void setConnectionType(ConnectionType connectionType) {this.connectionType = connectionType;}
    public String getLabel() {return label;}
    public void setLabel(String label) {this.label = label;}
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getWidth(){return width;}
    public void setWidth(Integer width){this.width = width;}
    public ConnectionStyle getStyle() {return style;}
    public void setStyle(ConnectionStyle style) {this.style = style;}
    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Override
    public String toString() {
        return label != null ? label : connectionType.getDisplayName();
    }
}