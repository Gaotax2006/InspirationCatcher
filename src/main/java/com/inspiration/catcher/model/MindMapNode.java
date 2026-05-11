package com.inspiration.catcher.model;

import java.time.LocalDateTime;

public class MindMapNode {
    private Integer id;
    private Integer projectId;
    private Integer ideaId;
    private Idea idea; // 关联的灵感对象
    private NodeType nodeType = NodeType.CONCEPT;
    private String text;
    private String description;
    private double x = 0.0;
    private double y = 0.0;
    private double width = 120.0;
    private double height = 50.0;
    private String color = "#4A90E2";
    private NodeShape shape = NodeShape.ROUNDED_RECT;
    private Integer fontSize = 14;
    private FontWeight fontWeight = FontWeight.NORMAL;
    private String fontStyle = "italic";
    private boolean isRoot = false;
    private boolean isExpanded = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 用于布局算法的临时变量
    private transient double forceX = 0.0;
    private transient double forceY = 0.0;
    // 枚举类型
    public enum NodeType {
        IDEA("灵感"),
        CONCEPT("概念"),
        EXTERNAL("外部链接");

        private final String displayName;
        NodeType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum NodeShape {
        RECTANGLE("矩形"),
        CIRCLE("圆形"),
        ELLIPSE("椭圆"),
        ROUNDED_RECT("圆角矩形");

        private final String displayName;
        NodeShape(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum FontWeight {
        NORMAL("正常"),
        BOLD("加粗");

        private final String displayName;
        FontWeight(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    // 构造方法
    public MindMapNode() {this.createdAt = LocalDateTime.now();this.updatedAt = LocalDateTime.now();}
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getProjectId() {return projectId;}
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Integer getIdeaId() {return ideaId;}
    public void setIdeaId(Integer ideaId) { this.ideaId = ideaId; }
    public NodeType getNodeType() {return nodeType;}
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public Idea getIdea() { return idea; }
    public void setIdea(Idea idea) { this.idea = idea; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public NodeShape getShape() {return shape;}
    public void setShape(NodeShape shape) {this.shape = shape;}
    public Integer getFontSize() {return fontSize;}
    public void setFontSize(Integer fontSize) {this.fontSize = fontSize;}
    public FontWeight getFontWeight() {return fontWeight;}
    public void setFontWeight(FontWeight fontWeight) {this.fontWeight = fontWeight;}
    public String getFontStyle() {return fontStyle;}
    public void setFontStyle(String fontStyle) {this.fontStyle = fontStyle;}
    public boolean isRoot() { return isRoot; }
    public void setRoot(boolean isRoot) { this.isRoot = isRoot; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean isExpanded) { this.isExpanded = isExpanded; }
    public LocalDateTime getCreatedAt() { return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    // 布局算法的临时方法（不在数据库中保存）
    public double getForceX() { return forceX; }
    public void setForceX(double forceX) { this.forceX = forceX; }
    public double getForceY() { return forceY; }
    public void setForceY(double forceY) { this.forceY = forceY; }
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    @Override
    public String toString() {
        return text;
    }
}