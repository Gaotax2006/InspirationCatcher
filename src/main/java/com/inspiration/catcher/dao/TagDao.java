package com.inspiration.catcher.dao;

import com.inspiration.catcher.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagDao {
    private static final Logger logger = LoggerFactory.getLogger(TagDao.class);

    // 获取所有标签
    public List<Tag> findAll() {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                // ✅ 确保读取usage_count字段
                try {
                    tag.setUsageCount(rs.getInt("usage_count"));
                } catch (SQLException e) {
                    // 如果表结构中没有usage_count字段，设为0
                    tag.setUsageCount(0);
                }
                tags.add(tag);
            }

        } catch (SQLException e) {
            logger.error("获取标签失败", e);
        }
        return tags;
    }

    // 根据ID获取标签
    public Tag findById(int id) {
        String sql = "SELECT * FROM tags WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                return tag;
            }
        } catch (SQLException e) {
            logger.error("根据ID查找标签失败", e);
        }
        return null;
    }

    // 根据名称查找标签
    public Tag findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                // 读取usage_count
                try {
                    tag.setUsageCount(rs.getInt("usage_count"));
                } catch (SQLException e) {
                    tag.setUsageCount(0);
                }
                return tag;
            }
        } catch (SQLException e) {
            logger.error("根据名称查找标签失败", e);
        }
        return null;
    }

    // 插入新标签
    public Tag insert(Tag tag) {
        String sql = "INSERT INTO tags (name, color, usage_count) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, tag.getName());
            pstmt.setString(2, tag.getColor());
            pstmt.setInt(3, tag.getUsageCount() != null ? tag.getUsageCount() : 0);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // SQLite专用：使用 last_insert_rowid()
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        tag.setId(generatedId);
                        return tag;
                    }
                }
            }
        } catch (SQLException e) {logger.error("插入标签失败", e);}
        return tag;
    }

    // 添加标签到灵感
    public boolean addTagToIdea(int ideaId, int tagId) {
        // 检查是否已存在关联
        String checkSql = "SELECT COUNT(*) FROM idea_tags WHERE idea_id = ? AND tag_id = ?";
        String insertSql = "INSERT OR IGNORE INTO idea_tags (idea_id, tag_id) VALUES (?, ?)";  // 使用 OR IGNORE
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            // 检查是否已存在
            checkStmt.setInt(1, ideaId);
            checkStmt.setInt(2, tagId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                logger.debug("标签关联已存在: ideaId={}, tagId={}", ideaId, tagId);
                return true;  // 已存在，返回成功
            }
            // 插入新关联
            insertStmt.setInt(1, ideaId);
            insertStmt.setInt(2, tagId);
            int affectedRows = insertStmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {logger.error("添加标签到灵感失败", e);}
        return false;
    }
    // 从灵感移除标签
    public boolean removeTagFromIdea(int ideaId, int tagId) {
        String sql = "DELETE FROM idea_tags WHERE idea_id = ? AND tag_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ideaId);
            pstmt.setInt(2, tagId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("从灵感移除标签失败", e);
        }
        return false;
    }

    // 获取最常用的标签
    public List<Tag> getMostUsedTags(int limit) {
        List<Tag> tags = new ArrayList<>();
        String sql = """
            SELECT t.*, COUNT(it.tag_id) as usage_count
            FROM tags t
            JOIN idea_tags it ON t.id = it.tag_id
            GROUP BY t.id
            ORDER BY usage_count DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tag.setUsageCount(rs.getInt("usage_count"));
                tags.add(tag);
            }
        } catch (SQLException e) {logger.error("获取最常用标签失败", e);}
        return tags;
    }

    // 搜索标签
    public List<Tag> searchTags(String keyword) {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE name LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            logger.error("搜索标签失败", e);
        }
        return tags;
    }
    // 获取未使用的标签（没有任何灵感关联的标签）
    public List<Tag> findUnusedTags() {
        List<Tag> unusedTags = new ArrayList<>();
        String sql = """
        SELECT t.*
        FROM tags t
        LEFT JOIN idea_tags it ON t.id = it.tag_id
        WHERE it.tag_id IS NULL
        ORDER BY t.name
        """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                unusedTags.add(tag);
            }
        } catch (SQLException e) {
            logger.error("获取未使用标签失败", e);
        }
        return unusedTags;
    }

    /**
     * 根据ID删除标签
     */
    public boolean delete(int tagId) {
        String sql = "DELETE FROM tags WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tagId);
            int affectedRows = pstmt.executeUpdate();

            logger.info("删除标签: id={}, 影响行数={}", tagId, affectedRows);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("删除标签失败: id={}", tagId, e);
        }
        return false;
    }

    /**
     * 获取标签的使用统计（每个标签被多少个灵感使用）
     */
    public Map<Integer, Integer> getTagUsageStats() {
        Map<Integer, Integer> stats = new HashMap<>();
        String sql = """
        SELECT tag_id, COUNT(*) as usage_count
        FROM idea_tags
        GROUP BY tag_id
        """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int tagId = rs.getInt("tag_id");
                int usageCount = rs.getInt("usage_count");
                stats.put(tagId, usageCount);
            }
        } catch (SQLException e) {
            logger.error("获取标签使用统计失败", e);
        }
        return stats;
    }

    // 获取所有标签及其使用次数
    public Map<String, Integer> getAllTagUsageCounts() {
        Map<String, Integer> tagCounts = new HashMap<>();
        String sql = """
        SELECT t.name, COUNT(it.tag_id) as usage_count
        FROM tags t
        LEFT JOIN idea_tags it ON t.id = it.tag_id
        GROUP BY t.id, t.name
        ORDER BY usage_count DESC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String tagName = rs.getString("name");
                int usageCount = rs.getInt("usage_count");
                tagCounts.put(tagName, usageCount);
            }
        } catch (SQLException e) {
            logger.error("获取标签使用次数失败", e);
        }
        return tagCounts;
    }

}