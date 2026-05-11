package com.inspiration.catcher.dao;


import com.inspiration.catcher.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectDao {
    private static final Logger logger = LoggerFactory.getLogger(ProjectDao.class);

    // 创建默认项目（系统初始化时调用）
    public void createDefaultProject() {
        String sql = "INSERT OR IGNORE INTO projects (id, name, description, color) VALUES (1, '默认项目', '系统默认项目，存放未分类的灵感', '#4A90E2')";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("默认项目创建成功");
        } catch (SQLException e) {
            logger.error("创建默认项目失败", e);
        }
    }

    // 获取所有项目
    public List<Project> findAll() {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects ORDER BY updated_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Project project = resultSetToProject(rs);
                projects.add(project);
            }
        } catch (SQLException e) {
            logger.error("获取项目列表失败", e);
        }
        return projects;
    }

    // 根据ID获取项目
    public Project findById(Integer id) {
        if (id == null) return null;

        String sql = "SELECT * FROM projects WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToProject(rs);
            }
        } catch (SQLException e) {
            logger.error("根据ID查找项目失败", e);
        }
        return null;
    }

    // 保存项目（新建或更新）
    public Project save(Project project) {
        return project == null ? null : project.getId() == null || project.getId() == 0 ? insert(project) : update(project);
    }
    // 插入新项目
    private Project insert(Project project) {
        String sql = "INSERT INTO projects (name, description, color, tag_strategy, mindmap_config) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setString(3, project.getColor());
            pstmt.setString(4, project.getTagStrategy() != null ? project.getTagStrategy().name() : Project.TagStrategy.GLOBAL.name());
            pstmt.setString(5, project.getMindmapConfig());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        project.setId(generatedId);
                        project.setCreatedAt(LocalDateTime.now());
                        project.setUpdatedAt(LocalDateTime.now());
                        return project;
                    }
                }
            }
        } catch (SQLException e) {logger.error("插入项目失败", e);}
        return null;
    }

    // 更新项目
    private Project update(Project project) {
        String sql = "UPDATE projects SET name = ?, description = ?, color = ?, tag_strategy = ?, " +
                "mindmap_config = ?, status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setString(3, project.getColor());
            pstmt.setString(4, project.getTagStrategy() != null ? project.getTagStrategy().name() : Project.TagStrategy.GLOBAL.name());
            pstmt.setString(5, project.getMindmapConfig());
            pstmt.setString(6, project.getStatus() != null ? project.getStatus().name() : Project.ProjectStatus.ACTIVE.name());
            pstmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(8, project.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                project.setUpdatedAt(LocalDateTime.now());
                return project;
            }
        } catch (SQLException e) {
            logger.error("更新项目失败", e);
        }
        return null;
    }

    // 删除项目
    public boolean delete(Integer id) {
        if (id == null || id <= 0 || id == 1) return false; // 不能删除默认项目

        String sql = "DELETE FROM projects WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("删除项目失败", e);
        }
        return false;
    }

    // 更新项目统计信息
    public void updateProjectStats(Integer projectId) {
        if (projectId == null) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            // 更新灵感数量
            String updateIdeaCount = "UPDATE projects SET idea_count = (SELECT COUNT(*) FROM ideas WHERE project_id = ?) WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateIdeaCount)) {
                pstmt.setInt(1, projectId);
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }

            // 更新节点数量
            String updateNodeCount = "UPDATE projects SET node_count = (SELECT COUNT(*) FROM mindmap_nodes WHERE project_id = ?) WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateNodeCount)) {
                pstmt.setInt(1, projectId);
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }

            // 更新连接数量
            String updateConnectionCount = "UPDATE projects SET connection_count = (SELECT COUNT(*) FROM mindmap_connections WHERE project_id = ?) WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateConnectionCount)) {
                pstmt.setInt(1, projectId);
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }

            // 更新更新时间
            String updateTime = "UPDATE projects SET updated_at = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateTime)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.error("更新项目统计信息失败", e);
        }
    }

    // 辅助方法：ResultSet转Project对象
    private Project resultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();

        project.setId(rs.getInt("id"));
        project.setName(rs.getString("name"));
        project.setDescription(rs.getString("description"));
        project.setColor(rs.getString("color"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            project.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            project.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // 枚举字段
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                project.setStatus(Project.ProjectStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                project.setStatus(Project.ProjectStatus.ACTIVE);
            }
        }

        String tagStrategyStr = rs.getString("tag_strategy");
        if (tagStrategyStr != null) {
            try {
                project.setTagStrategy(Project.TagStrategy.valueOf(tagStrategyStr));
            } catch (IllegalArgumentException e) {
                project.setTagStrategy(Project.TagStrategy.GLOBAL);
            }
        }

        project.setMindmapConfig(rs.getString("mindmap_config"));
        project.setIdeaCount(rs.getInt("idea_count"));
        project.setNodeCount(rs.getInt("node_count"));
        project.setConnectionCount(rs.getInt("connection_count"));

        return project;
    }
}