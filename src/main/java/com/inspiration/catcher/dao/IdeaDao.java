package com.inspiration.catcher.dao;

import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IdeaDao {
    private static final Logger logger = LoggerFactory.getLogger(IdeaDao.class);

    // 获取当前项目所有灵感-null表示获取所有项目
    public List<Idea> findAll() {return findAllByProject(null);}
    public List<Idea> findAllByProject(Integer projectId) {
        List<Idea> ideas = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ideas");
        if (projectId != null) sql.append(" WHERE project_id = ?");
        sql.append(" ORDER BY created_at DESC");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            if (projectId != null) pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Idea idea = resultSetToIdea(rs);
                ideas.add(idea);
            }
            // 批量加载标签（替代N+1）
            batchLoadTags(ideas);
        } catch (SQLException e) {logger.error("查找灵感失败", e);}
        return ideas;
    }
    // 根据ID 查找灵感
    public Idea findById(int id) {
        String sql = "SELECT * FROM ideas WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Idea idea = resultSetToIdea(rs);
                idea.setTags(getTagsForIdea(idea.getId()));
                return idea;
            }
        } catch (SQLException e) {logger.error("查找灵感失败: id={}", id, e);}
        return null;
    }
    public List<Idea> findByProjectId(Integer projectId) {
        List<Idea> ideas = new ArrayList<>();
        String sql = "SELECT * FROM ideas WHERE project_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) ideas.add(resultSetToIdea(rs));
            // 批量加载标签（替代N+1）
            batchLoadTags(ideas);
        } catch (SQLException e) {
            logger.error("根据项目ID查找灵感失败", e);
        }
        return ideas;
    }
    // 插入新灵感
    public Idea insert(Idea idea) {
        String sql = "INSERT INTO ideas (title, content, idea_type, mood, importance, project_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int projectId = idea.getProjectId() != null ? idea.getProjectId() : 1;
        Connection conn;
        PreparedStatement pstmt = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            // 设置参数
            pstmt.setString(1, idea.getTitle());
            pstmt.setString(2, idea.getContent());
            pstmt.setString(3, idea.getType().name());
            pstmt.setString(4, idea.getMood() != null ? idea.getMood().name() : Idea.Mood.NEUTRAL.name());
            pstmt.setInt(5, idea.getImportance());
            pstmt.setInt(6, projectId);
            pstmt.setTimestamp(7, Timestamp.valueOf(idea.getCreatedAt()));
            pstmt.setTimestamp(8, Timestamp.valueOf(idea.getUpdatedAt()));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        idea.setId(generatedId);
                        // 保存标签关联到数据库
                        saveTagsToDatabase(idea, conn);
                        return idea;  // 直接返回设置好 ID的idea
                    }
                }
            }
            return idea;
        } catch (SQLException e) {return idea;}
        finally {try {if (pstmt != null) pstmt.close();} catch (SQLException _) {}}
    }
    // 更新灵感
    public Idea update(Idea idea) {
        @SuppressWarnings("SqlDialectInspection") String sql = "UPDATE ideas SET title = ?, content = ?, idea_type = ?, " +
                "mood = ?, importance = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, idea.getTitle());
            pstmt.setString(2, idea.getContent());
            pstmt.setString(3, idea.getType().name());
            pstmt.setString(4, idea.getMood() != null ? idea.getMood().name() : Idea.Mood.NEUTRAL.name());
            pstmt.setInt(5, idea.getImportance());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(7, idea.getId());
            if (pstmt.executeUpdate() > 0) {
                logger.info("更新灵感成功: ID={}", idea.getId());
                updateTagsForIdea(idea,conn);
                return findById(idea.getId()); // 重新加载确保数据一致
            } else {
                logger.warn("更新灵感失败，未找到记录: ID={}", idea.getId());
                return null;
            }
        } catch (SQLException e) {logger.error("更新灵感失败", e);return null;}
    }
    // 插入新记录或更新现有记录
    public Idea save(Idea idea) {return idea == null ? null : idea.getId() == null || idea.getId() == 0 ? insert(idea) : update(idea);}
    // 保存标签
    private void saveTagsToDatabase(Idea idea, Connection conn) {
        if (idea == null || idea.getId() == null || idea.getTags() == null || idea.getTags().isEmpty()) return;
        // 先删除旧的标签关联
        String deleteSql = "DELETE FROM idea_tags WHERE idea_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, idea.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {logger.error("删除旧标签关联失败", e);}
        // 插入新的标签关联
        String insertSql = "INSERT OR IGNORE INTO idea_tags (idea_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (Tag tag : idea.getTags())
                if (tag != null && tag.getId() != null) {
                    pstmt.setInt(1, idea.getId());
                    pstmt.setInt(2, tag.getId());
                    pstmt.addBatch();
                }
            pstmt.executeBatch();
            logger.info("保存了 {} 个标签关联", idea.getTags().size());
        } catch (SQLException e) {
            logger.error("保存标签关联失败", e);
            e.printStackTrace();
        }
    }
    // 更新标签-复用保存逻辑
    private void updateTagsForIdea(Idea idea, Connection conn) {saveTagsToDatabase(idea,conn);}
    // 删除灵感
    public boolean delete(int id) {
        Connection conn = null;
        PreparedStatement deleteTagsStmt = null;
        PreparedStatement deleteIdeaStmt = null;
        PreparedStatement updateTagStmt = null;
        PreparedStatement selectTagsStmt = null;
        PreparedStatement selectUsageStmt = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            // 1. 获取这个灵感的所有标签ID
            List<Integer> tagIds = new ArrayList<>();
            String selectTagsSql = "SELECT tag_id FROM idea_tags WHERE idea_id = ?";
            selectTagsStmt = conn.prepareStatement(selectTagsSql);
            selectTagsStmt.setInt(1, id);
            ResultSet tagRs = selectTagsStmt.executeQuery();
            while (tagRs.next()) tagIds.add(tagRs.getInt("tag_id"));
            tagRs.close();
            // 2. 删除灵感-标签关联
            String deleteTagsSql = "DELETE FROM idea_tags WHERE idea_id = ?";
            deleteTagsStmt = conn.prepareStatement(deleteTagsSql);
            deleteTagsStmt.setInt(1, id);
            deleteTagsStmt.executeUpdate();
            // 3. 删除灵感
            String deleteIdeaSql = "DELETE FROM ideas WHERE id = ?";
            deleteIdeaStmt = conn.prepareStatement(deleteIdeaSql);
            deleteIdeaStmt.setInt(1, id);
            int affectedRows = deleteIdeaStmt.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                logger.warn("删除灵感失败，未找到记录: id={}", id);
                return false;
            }
            // 4. 更新这些标签的使用计数（减1）
            if (!tagIds.isEmpty()) {
                // 对于每个标签，先查询当前计数，再更新
                String selectUsageSql = "SELECT usage_count FROM tags WHERE id = ?";
                String updateTagSql = "UPDATE tags SET usage_count = ? WHERE id = ?";
                for (Integer tagId : tagIds) {
                    // 查询当前使用计数
                    selectUsageStmt = conn.prepareStatement(selectUsageSql);
                    selectUsageStmt.setInt(1, tagId);
                    ResultSet usageRs = selectUsageStmt.executeQuery();
                    int currentCount = 0;
                    if (usageRs.next()) currentCount = usageRs.getInt("usage_count");
                    usageRs.close();
                    selectUsageStmt.close();
                    // 计算新计数（确保不小于0）
                    int newCount = Math.max(0, currentCount - 1);
                    // 更新计数
                    updateTagStmt = conn.prepareStatement(updateTagSql);
                    updateTagStmt.setInt(1, newCount);
                    updateTagStmt.setInt(2, tagId);
                    updateTagStmt.executeUpdate();
                    updateTagStmt.close();
                }
            }
            // 提交事务
            conn.commit();
            logger.info("删除灵感成功: id={}, 更新了 {} 个标签的计数", id, tagIds.size());
            return true;
        } catch (SQLException e) {
            logger.error("删除灵感失败: id={}", id, e);
            try {if (!conn.isClosed()) conn.rollback();
            } catch (SQLException rollbackEx) {logger.error("回滚事务失败", rollbackEx);}
            return false;
        } finally {// 清理资源
            closeResources(selectTagsStmt, deleteTagsStmt, deleteIdeaStmt, updateTagStmt, selectUsageStmt);
            if (conn != null) {
                try {conn.setAutoCommit(true);
                } catch (SQLException e) {logger.error("恢复自动提交失败", e);}
            }
        }
    }
    // 关闭资源
    private void closeResources(Statement... statements) {
        for (Statement stmt : statements) if (stmt != null)
            try {stmt.close();}
            catch (SQLException e) {logger.debug("关闭Statement失败", e);}
    }
    // 获取灵感总数
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM ideas";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {logger.error("获取灵感总数失败", e);}
        return 0;
    }
    // 获取一周内的灵感数量
    public int getCountAfter(LocalDateTime dateTime) {
        String sql = "SELECT COUNT(*) FROM ideas WHERE created_at >= ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(dateTime));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {logger.error("获取周计数失败", e);}
        return 0;
    }
    // 获取最常用的标签
    public String getMostUsedTag() {
        String sql = """
        SELECT t.name, COUNT(it.tag_id) as tag_count
        FROM idea_tags it
        JOIN tags t ON it.tag_id = t.id
        GROUP BY t.id
        ORDER BY tag_count DESC
        LIMIT 1
        """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {logger.error("获取最常用标签失败", e);}
        return "暂无数据";
    }
    // 辅助方法：从ResultSet创建Idea对象
    private Idea resultSetToIdea(ResultSet rs) throws SQLException {
        try {
            Idea idea = new Idea();
            // 获取ID、标题和内容
            idea.setId(rs.getInt("id"));
            idea.setTitle(rs.getString("title"));
            idea.setContent(rs.getString("content"));
            // 读取project_id
            try {
                int projectId = rs.getInt("project_id");
                idea.setProjectId(projectId);
            } catch (SQLException e) {
                // 兼容旧的数据库结构
                idea.setProjectId(1); // 默认项目
            }
            // 正确处理枚举类型（添加null检查）
            String typeStr = rs.getString("idea_type");
            if (typeStr != null && !typeStr.trim().isEmpty()) {
                try {
                    idea.setType(Idea.IdeaType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    idea.setType(Idea.IdeaType.IDEA); // 默认值
                    logger.warn("无法识别的idea_type: {}, 使用默认值IDEA", typeStr);
                }
            } else idea.setType(Idea.IdeaType.IDEA); // 默认值
            // 修复：正确处理心情字段（从数据库读取）
            String moodStr = rs.getString("mood");
            if (moodStr != null && !moodStr.trim().isEmpty()) {
                try {
                    idea.setMood(Idea.Mood.valueOf(moodStr));
                } catch (IllegalArgumentException e) {
                    idea.setMood(Idea.Mood.NEUTRAL); // 默认值
                    logger.warn("无法识别的mood: {}, 使用默认值NEUTRAL", moodStr);
                }
            } else idea.setMood(Idea.Mood.NEUTRAL); // 默认值
            // 获取重要性（防止null）
            int importance = rs.getInt("importance");
            if (importance < 1 || importance > 5) importance = 3; // 默认值
            idea.setImportance(importance);
            // 获取时间（确保不是null）
            Timestamp createdAt = rs.getTimestamp("created_at");
            idea.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now());
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            idea.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : LocalDateTime.now());
            return idea;
        } catch (Exception e) {
            logger.error("转换ResultSet 到Idea 对象失败", e);
            throw new SQLException("转换数据失败: " + e.getMessage());
        }
    }
    // 批量加载标签（替代N+1查询）
    private void batchLoadTags(List<Idea> ideas) {
        if (ideas == null || ideas.isEmpty()) return;
        List<Integer> ids = new ArrayList<>();
        for (Idea idea : ideas) if (idea.getId() != null) ids.add(idea.getId());
        if (ids.isEmpty()) return;
        // 构建IN子句
        StringBuilder sql = new StringBuilder(
                "SELECT it.idea_id, t.id, t.name, t.color FROM idea_tags it JOIN tags t ON t.id = it.tag_id WHERE it.idea_id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(") ORDER BY it.idea_id");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < ids.size(); i++) pstmt.setInt(i + 1, ids.get(i));
            ResultSet rs = pstmt.executeQuery();
            // 按 idea_id 分组
            java.util.Map<Integer, List<Tag>> tagMap = new java.util.HashMap<>();
            while (rs.next()) {
                int ideaId = rs.getInt("idea_id");
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tagMap.computeIfAbsent(ideaId, _ -> new ArrayList<>()).add(tag);
            }
            // 为每个 Idea 设置标签
            for (Idea idea : ideas) {
                if (idea.getId() != null) {
                    List<Tag> tags = tagMap.get(idea.getId());
                    if (tags != null) idea.setTags(tags);
                }
            }
        } catch (SQLException e) {
            logger.error("批量加载标签失败", e);
        }
    }

    // 获取灵感的标签
    private List<Tag> getTagsForIdea(int ideaId) {
        List<Tag> tags = new ArrayList<>();
        if (ideaId <= 0) return tags;
        // 使用独立的连接
        Connection conn;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT t.* FROM tags t " +
                    "JOIN idea_tags it ON t.id = it.tag_id " +
                    "WHERE it.idea_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, ideaId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            logger.error("获取标签失败: ideaId={}", ideaId, e);
        } finally {// 关闭资源 注意：不要关闭连接
            try { if (rs != null) rs.close(); } catch (SQLException _) { }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException _) { }
        }
        return tags;
    }
    // 搜索灵感
    public List<Idea> searchByKeyword(String keyword) {
        List<Idea> results = new ArrayList<>();
        String sql = "SELECT * FROM ideas WHERE title LIKE ? OR content LIKE ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Idea idea = resultSetToIdea(rs);
                results.add(idea);
            }
            batchLoadTags(results);
        } catch (SQLException e) {logger.error("搜索灵感失败", e);}
        return results;
    }
}