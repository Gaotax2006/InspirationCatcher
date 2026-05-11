package com.inspiration.catcher.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:inspiration.db";
    private static Connection connection = null;

    // 获取数据库连接
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("数据库连接失败", e);
        }
        return connection;
    }

    // 初始化数据库表
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // 创建项目表
            String createProjectsTable = """
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    color TEXT DEFAULT '#36B37E',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    status TEXT CHECK(status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')) DEFAULT 'ACTIVE',
                    -- 思维导图配置（JSON格式存储布局、样式等）
                    mindmap_config TEXT DEFAULT '{}',
                    -- 标签策略：GLOBAL（使用全局标签）或 PROJECT（项目独立标签）
                    tag_strategy TEXT CHECK(tag_strategy IN ('GLOBAL', 'PROJECT')) DEFAULT 'GLOBAL',
                    -- 项目统计（缓存，提高查询性能）
                    idea_count INTEGER DEFAULT 0,
                    node_count INTEGER DEFAULT 0,
                    connection_count INTEGER DEFAULT 0
                );
            """;
            // 创建灵感表
            String createIdeasTable = """
                CREATE TABLE IF NOT EXISTS ideas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL DEFAULT 1,
                    title TEXT,
                    content TEXT NOT NULL,
                    idea_type TEXT CHECK(idea_type IN ('IDEA', 'QUOTE', 'QUESTION', 'TODO', 'DISCOVERY', 'CONFUSION', 'HYPOTHESIS')) DEFAULT 'IDEA',
                    mood TEXT CHECK(mood IN ('HAPPY','EXCITED','CALM','NEUTRAL', 'THOUGHTFUL', 'CREATIVE', 'INSPIRED','CURIOUS', 'CONFUSED','FRUSTRATED')) DEFAULT 'NEUTRAL',
                    importance INTEGER CHECK(importance BETWEEN 1 AND 5) DEFAULT 3,
                    privacy TEXT CHECK(privacy IN ('PUBLIC', 'PRIVATE', 'ENCRYPTED')) DEFAULT 'PRIVATE',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );
            """;

            // 创建标签表
            String createTagsTable = """
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    color TEXT DEFAULT '#4A90E2',
                    description TEXT,
                    usage_count INTEGER DEFAULT 0
                );
            """;

            // 创建灵感-标签关联表
            String createIdeaTagsTable = """
                CREATE TABLE IF NOT EXISTS idea_tags (
                    idea_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (idea_id, tag_id),
                    FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                );
            """;
            // 创建思维导图表
            String createMindMapNodesTable = """
                CREATE TABLE IF NOT EXISTS mindmap_nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    idea_id INTEGER,
                    node_type TEXT CHECK(node_type IN ('IDEA', 'CONCEPT', 'EXTERNAL')) DEFAULT 'CONCEPT',
                    text TEXT NOT NULL,
                    description TEXT,
                    x REAL DEFAULT 0.0,
                    y REAL DEFAULT 0.0,
                    width REAL DEFAULT 140.0,
                    height REAL DEFAULT 50.0,
                    color TEXT DEFAULT '#4A90E2',
                    shape TEXT CHECK(shape IN ('RECTANGLE', 'CIRCLE', 'ELLIPSE', 'ROUNDED_RECT')) DEFAULT 'ROUNDED_RECT',
                    font_size INTEGER DEFAULT 14,
                    font_weight TEXT CHECK(font_weight IN ('NORMAL', 'BOLD')) DEFAULT 'NORMAL',
                    is_root BOOLEAN DEFAULT 0,
                    is_expanded BOOLEAN DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                    FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE SET NULL
                );
            """;
            //思维导图链接表
            String createMindMapConnectionsTable = """
                CREATE TABLE IF NOT EXISTS mindmap_connections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    source_node_id INTEGER NOT NULL,
                    target_node_id INTEGER NOT NULL,
                    connection_type TEXT CHECK(connection_type IN ('RELATED', 'DEPENDS_ON', 'EXTENDS', 'CONTRADICTS', 'ANALOGY', 'CAUSAL', 'USES')) DEFAULT 'RELATED',
                    label TEXT,
                    color TEXT DEFAULT '#666666',
                    width INTEGER DEFAULT 2,
                    style TEXT CHECK(style IN ('SOLID', 'DASHED', 'DOTTED')) DEFAULT 'SOLID',
                    strength REAL DEFAULT 0.5,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                    FOREIGN KEY (source_node_id) REFERENCES mindmap_nodes(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_node_id) REFERENCES mindmap_nodes(id) ON DELETE CASCADE,
                    UNIQUE(source_node_id, target_node_id)
                );
            """;
            // 创建项目独立标签表
            String createProjectTagsTable = """
                CREATE TABLE IF NOT EXISTS project_tags (
                    project_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (project_id, tag_id),
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                );
            """;
            // 创建灵感关联表
            String createConnectionsTable = """
                CREATE TABLE IF NOT EXISTS connections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_idea_id INTEGER NOT NULL,
                    target_idea_id INTEGER NOT NULL,
                    relationship TEXT CHECK(relationship IN ('SUPPORTS', 'OPPOSES', 'EXTENDS', 'ANALOGY', 'CAUSAL', 'TEMPORAL')) NOT NULL,
                    strength REAL DEFAULT 0.5 CHECK(strength BETWEEN 0.0 AND 1.0),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    last_activated DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (source_idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_idea_id) REFERENCES ideas(id) ON DELETE CASCADE
                );
            """;

            // 执行所有SQL语句
            stmt.execute(createIdeasTable);
            stmt.execute(createTagsTable);
            stmt.execute(createIdeaTagsTable);
            stmt.execute(createProjectsTable);
            stmt.execute(createConnectionsTable);
            stmt.execute(createMindMapNodesTable);
            stmt.execute(createMindMapConnectionsTable);

            // 创建索引以提高查询性能
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ideas_created_at ON ideas(created_at);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ideas_importance ON ideas(importance);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connections_source ON connections(source_idea_id);");

            logger.info("数据库初始化完成");

        } catch (SQLException e) {
            logger.error("数据库初始化失败", e);
        }
    }

    // 关闭连接
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("数据库连接已关闭");
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
            }
        }
    }
}