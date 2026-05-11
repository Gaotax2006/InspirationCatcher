package com.inspiration.catcher.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Idea {
    private Integer id;
    private String title;
    private String content;
    private IdeaType type;
    private Mood mood;
    private Integer importance;
    private PrivacyLevel privacy = PrivacyLevel.PRIVATE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer projectId;

    private List<Tag> tags = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();

    // 枚举类型（保持不变）
    public enum IdeaType {
        IDEA("想法"), QUOTE("引用"), QUESTION("问题"), TODO("待办"),
        DISCOVERY("发现"), CONFUSION("困惑"), HYPOTHESIS("假设");
        private final String displayName;
        IdeaType(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Mood {
        HAPPY("开心"),EXCITED("兴奋"),CALM("平静"),NEUTRAL("中性"),THOUGHTFUL("沉思"),
        CREATIVE("创意"),INSPIRED("启发"),CURIOUS("好奇"),CONFUSED("困惑"),FRUSTRATED("沮丧");
        private final String displayName;
        Mood(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PrivacyLevel {
        PUBLIC("公开"), PRIVATE("私有"), ENCRYPTED("加密");
        private final String displayName;
        PrivacyLevel(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    // 构造方法
    public Idea() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.type = IdeaType.IDEA;
        this.mood = Mood.NEUTRAL;
        this.importance = 3;
    }

    public Idea(String title, String content, IdeaType type) {
        this();
        this.title = title;
        this.content = content;
        this.type = type;
    }

    // Getters 和 Setters
    public Integer getId() { return id; }
    public void setId(Integer id) {this.id = id;this.updatedAt = LocalDateTime.now();}
    public String getTitle() { return title; }
    public void setTitle(String title) {this.title = title;this.updatedAt = LocalDateTime.now();}
    public String getContent() { return content; }
    public void setContent(String content) {this.content = content;this.updatedAt = LocalDateTime.now();}
    public IdeaType getType() { return type; }
    public void setType(IdeaType type) {this.type = type;this.updatedAt = LocalDateTime.now();}
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) {this.mood = mood;this.updatedAt = LocalDateTime.now();}
    public Integer getImportance() { return importance; }
    public void setImportance(Integer importance) {
        this.importance = (importance < 1) ? 1 : (importance > 5) ? 5 : importance;this.updatedAt = LocalDateTime.now();}
    public PrivacyLevel getPrivacy() { return privacy; }
    public void setPrivacy(PrivacyLevel privacy) {this.privacy = privacy;this.updatedAt = LocalDateTime.now();}
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) {this.projectId = projectId;this.updatedAt = LocalDateTime.now();}
    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }
    public List<Connection> getConnections() { return connections; }
    public void setConnections(List<Connection> connections) { this.connections = connections; }
    // 添加标签
    public void addTag(Tag tag) {
        if (tags == null) tags = new ArrayList<>();
        if (!tags.contains(tag)) tags.add(tag);
    }
    // 移除标签
    public void removeTag(Tag tag) {if (tags != null) tags.remove(tag);}
    // 获取标签字符串
    public String getTagsString() {
        if (tags == null || tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) sb.append(tag.getName()).append(", ");
        if (!sb.isEmpty()) sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        return sb.toString();
    }

    // 判断是否为新建灵感（未保存到数据库）
    public boolean isNew() {return id == null || id <= 0;}
    @Override
    public String toString() {
        if (title != null && !title.trim().isEmpty()) return title;
        if (content != null && !content.isEmpty()) {
            int endIndex = Math.min(50, content.length());
            return content.substring(0, endIndex) + "...";
        }
        return "无标题灵感";
    }
}