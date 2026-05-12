package com.inspiration.catcher.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 收藏夹 - 跨项目组织灵感的集合
 */
public class Collection {
    private Integer id;
    private String name;
    private String description;
    private String color = "#C4843C";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<Integer> ideaIds = new ArrayList<>();

    public Collection() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Collection(String name) {
        this();
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<Integer> getIdeaIds() { return ideaIds; }
    public void setIdeaIds(List<Integer> ideaIds) { this.ideaIds.clear(); this.ideaIds.addAll(ideaIds); }
    public void addIdeaId(int ideaId) { if (!ideaIds.contains(ideaId)) ideaIds.add(ideaId); }
    public void removeIdeaId(int ideaId) { ideaIds.remove((Integer) ideaId); }
    public boolean contains(int ideaId) { return ideaIds.contains(ideaId); }
    public int size() { return ideaIds.size(); }

    @Override
    public String toString() { return name + " (" + ideaIds.size() + ")"; }
}
