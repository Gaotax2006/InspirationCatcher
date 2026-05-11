package com.inspiration.catcher.model;

public class Tag {
    private Integer id;
    private String name;
    private String color;
    private String description;
    private Integer usageCount = 0;

    // 构造方法
    public Tag() {}

    public Tag(String name) {
        this.name = name;
        this.color = generateRandomColor();
    }

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // Getters 和 Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getUsageCount() {return usageCount != null ? usageCount : 0;}
    public void setUsageCount(Integer usageCount) {this.usageCount = usageCount != null ? usageCount : 0;}

    // 随机生成颜色
    private String generateRandomColor() {
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#FFD166", "#06D6A0", "#118AB2",
                "#EF476F", "#FFD166", "#06D6A0", "#118AB2", "#073B4C"
        };
        return colors[(int)(Math.random() * colors.length)];
    }

    // 增加使用次数（从数据库更新时使用）
    public void incrementUsage() {this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;}
    // 减少使用次数
    public void decrementUsage() {this.usageCount = Math.max(0, (this.usageCount != null ? this.usageCount : 0) - 1);}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        if (id != null && tag.id != null) return id.equals(tag.id);
        return name.equals(tag.name);
    }

    @Override
    public int hashCode() {return id != null ? id.hashCode() : name.hashCode();}

    @Override
    public String toString() {
        return name;
    }
}