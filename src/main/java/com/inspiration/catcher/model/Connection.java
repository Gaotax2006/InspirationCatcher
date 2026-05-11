package com.inspiration.catcher.model;

import java.time.LocalDateTime;

public class Connection {
    private Integer id;
    private Integer sourceIdeaId;
    private Integer targetIdeaId;
    private ConnectionType relationship;
    private Double strength = 0.5;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivated;

    // 枚举类型
    public enum ConnectionType {
        SUPPORTS("支持"),
        OPPOSES("反对"),
        EXTENDS("扩展"),
        ANALOGY("类比"),
        CAUSAL("因果"),
        TEMPORAL("时间序列");
        private final String displayName;
        ConnectionType(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }
    // 构造方法
    public Connection() {this.createdAt = LocalDateTime.now();this.lastActivated = LocalDateTime.now();}
    public Connection(Integer sourceIdeaId, Integer targetIdeaId, ConnectionType relationship) {
        this();
        this.sourceIdeaId = sourceIdeaId;
        this.targetIdeaId = targetIdeaId;
        this.relationship = relationship;
    }
    // Getters 和 Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getSourceIdeaId() { return sourceIdeaId; }
    public void setSourceIdeaId(Integer sourceIdeaId) { this.sourceIdeaId = sourceIdeaId; }
    public Integer getTargetIdeaId() { return targetIdeaId; }
    public void setTargetIdeaId(Integer targetIdeaId) { this.targetIdeaId = targetIdeaId; }
    public ConnectionType getRelationship() { return relationship; }
    public void setRelationship(ConnectionType relationship) { this.relationship = relationship; }
    public Double getStrength() { return strength; }
    public void setStrength(Double strength) {this.strength = Math.max(0.0, Math.min(1.0, strength)); }// 限制在0-1之间
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastActivated() { return lastActivated; }
    public void setLastActivated(LocalDateTime lastActivated) { this.lastActivated = lastActivated; }
    // 激活连接（更新最后激活时间）
    public void activate() {this.lastActivated = LocalDateTime.now();}
    @Override
    public String toString() {return String.format("连接 %d -> %d (%s)", sourceIdeaId, targetIdeaId, relationship.getDisplayName());}
}