package com.inspiration.catcher.model;

import java.time.LocalDateTime;

public class Project {
    private Integer id;
    private String name;
    private String description;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ProjectStatus status = ProjectStatus.ACTIVE;

    private String mindmapConfig = "{}";
    private TagStrategy tagStrategy = TagStrategy.GLOBAL;
    private Integer ideaCount = 0;
    private Integer nodeCount = 0;
    private Integer connectionCount = 0;

    // 枚举类型
    public enum ProjectStatus {
        ACTIVE("进行中"),
        PAUSED("暂停"),
        COMPLETED("完成"),
        ARCHIVED("归档");

        private final String displayName;
        ProjectStatus(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    // 标签策略枚举
    public enum TagStrategy {
        GLOBAL("全局标签"),
        PROJECT("项目独立标签");
        private final String displayName;
        TagStrategy(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // 构造方法
    public Project() {
        this.createdAt = LocalDateTime.now();
        this.color = "#36B37E"; // 默认颜色
    }

    public Project(String name) {
        this();
        this.name = name;
    }

    // Getters 和 Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public TagStrategy getTagStrategy() { return tagStrategy; }
    public void setTagStrategy(TagStrategy tagStrategy) { this.tagStrategy = tagStrategy; }

    public String getMindmapConfig() { return mindmapConfig; }
    public void  setMindmapConfig(String mindmapConfig) {this.mindmapConfig = mindmapConfig; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Integer getIdeaCount() { return ideaCount; }
    public void setIdeaCount(Integer ideaCount) { this.ideaCount = ideaCount; }
    public Integer getNodeCount() { return nodeCount; }
    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }
    public Integer getConnectionCount() { return connectionCount; }
    public void setConnectionCount(Integer connectionCount) { this.connectionCount = connectionCount; }


    @Override
    public String toString() {
        return name;
    }
}