package com.inspiration.catcher.dao;

import com.inspiration.catcher.model.MindMapNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MindMapNodeDao {
    private static final Logger logger = LoggerFactory.getLogger(MindMapNodeDao.class);

    // 保存节点（插入或更新）
    public MindMapNode save(MindMapNode node) {
        return node == null ? null : node.getId() == null || node.getId() == 0 ? insert(node) : update(node);
    }
    // 插入新节点
    private MindMapNode insert(MindMapNode node) {
        String sql = "INSERT INTO mindmap_nodes (project_id, idea_id, node_type, text, description, " +
                "x, y, width, height, color, shape, font_size, font_weight, is_root, is_expanded) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setNodeParameters(pstmt, node);
            if (pstmt.executeUpdate() > 0)
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        node.setId(generatedId);
                        return node;
                    }
                }
        } catch (SQLException e) {logger.error("插入思维导图节点失败", e);}
        return null;
    }

    // 更新节点
    private MindMapNode update(MindMapNode node) {
        String sql = "UPDATE mindmap_nodes SET text = ?, description = ?, x = ?, y = ?, " +
                "width = ?, height = ?, color = ?, shape = ?, font_size = ?, " +
                "font_weight = ?, is_expanded = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, node.getText());
            pstmt.setString(2, node.getDescription());
            pstmt.setDouble(3, node.getX());
            pstmt.setDouble(4, node.getY());
            pstmt.setDouble(5, node.getWidth());
            pstmt.setDouble(6, node.getHeight());
            pstmt.setString(7, node.getColor());
            pstmt.setString(8, node.getShape() != null ? node.getShape().name() : "ROUNDED_RECT");
            pstmt.setInt(9, node.getFontSize());
            pstmt.setString(10, node.getFontWeight() != null ? node.getFontWeight().name() : "NORMAL");
            pstmt.setBoolean(11, node.isExpanded());
            pstmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(13, node.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return node;
            }
        } catch (SQLException e) {
            logger.error("更新思维导图节点失败", e);
        }
        return null;
    }

    // 根据项目ID查询所有节点
    public List<MindMapNode> findByProjectId(int projectId) {
        List<MindMapNode> nodes = new ArrayList<>();
        String sql = "SELECT * FROM mindmap_nodes WHERE project_id = ? ORDER BY is_root DESC, created_at";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                MindMapNode node = resultSetToNode(rs);
                nodes.add(node);
            }
        } catch (SQLException e) {
            logger.error("查询项目思维导图节点失败", e);
        }
        return nodes;
    }

    // 根据ID查询节点
    public MindMapNode findById(int id) {
        String sql = "SELECT * FROM mindmap_nodes WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToNode(rs);
            }
        } catch (SQLException e) {
            logger.error("查询思维导图节点失败", e);
        }
        return null;
    }

    // 根据灵感ID查询节点
    public MindMapNode findByIdeaId(int ideaId) {
        String sql = "SELECT * FROM mindmap_nodes WHERE idea_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ideaId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToNode(rs);
            }
        } catch (SQLException e) {
            logger.error("根据灵感ID查询节点失败", e);
        }
        return null;
    }

    // 删除节点
    public boolean delete(int id) {
        String sql = "DELETE FROM mindmap_nodes WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("删除思维导图节点失败", e);
        }
        return false;
    }

    // 删除项目的所有节点
    public boolean deleteByProjectId(int projectId) {
        String sql = "DELETE FROM mindmap_nodes WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("删除项目思维导图节点失败", e);
        }
        return false;
    }

    // 获取项目节点数量
    public int getNodeCount(int projectId) {
        String sql = "SELECT COUNT(*) FROM mindmap_nodes WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("获取节点数量失败", e);
        }
        return 0;
    }

    // 辅助方法：设置节点参数
    private void setNodeParameters(PreparedStatement pstmt, MindMapNode node) throws SQLException {
        pstmt.setInt(1, node.getProjectId());

        if (node.getIdeaId() != null) {
            pstmt.setInt(2, node.getIdeaId());
        } else {
            pstmt.setNull(2, Types.INTEGER);
        }

        pstmt.setString(3, node.getNodeType() != null ? node.getNodeType().name() : "CONCEPT");
        pstmt.setString(4, node.getText());
        pstmt.setString(5, node.getDescription());
        pstmt.setDouble(6, node.getX());
        pstmt.setDouble(7, node.getY());
        pstmt.setDouble(8, node.getWidth());
        pstmt.setDouble(9, node.getHeight());
        pstmt.setString(10, node.getColor());
        pstmt.setString(11, node.getShape() != null ? node.getShape().name() : "ROUNDED_RECT");
        pstmt.setInt(12, node.getFontSize());
        pstmt.setString(13, node.getFontWeight() != null ? node.getFontWeight().name() : "NORMAL");
        pstmt.setBoolean(14, node.isRoot());
        pstmt.setBoolean(15, node.isExpanded());
    }

    // 辅助方法：ResultSet转节点对象
    private MindMapNode resultSetToNode(ResultSet rs) throws SQLException {
        MindMapNode node = new MindMapNode();
        node.setId(rs.getInt("id"));
        node.setProjectId(rs.getInt("project_id"));

        int ideaId = rs.getInt("idea_id");
        if (!rs.wasNull()) {
            node.setIdeaId(ideaId);
        }

        // 处理枚举类型
        String nodeTypeStr = rs.getString("node_type");
        if (nodeTypeStr != null) {
            try {
                node.setNodeType(MindMapNode.NodeType.valueOf(nodeTypeStr));
            } catch (IllegalArgumentException e) {
                node.setNodeType(MindMapNode.NodeType.CONCEPT);
            }
        }

        node.setText(rs.getString("text"));
        node.setDescription(rs.getString("description"));
        node.setX(rs.getDouble("x"));
        node.setY(rs.getDouble("y"));
        node.setWidth(rs.getDouble("width"));
        node.setHeight(rs.getDouble("height"));
        node.setColor(rs.getString("color"));

        String shapeStr = rs.getString("shape");
        if (shapeStr != null) {
            try {
                node.setShape(MindMapNode.NodeShape.valueOf(shapeStr));
            } catch (IllegalArgumentException e) {
                node.setShape(MindMapNode.NodeShape.ROUNDED_RECT);
            }
        }

        node.setFontSize(rs.getInt("font_size"));

        String fontWeightStr = rs.getString("font_weight");
        if (fontWeightStr != null) {
            try {
                node.setFontWeight(MindMapNode.FontWeight.valueOf(fontWeightStr));
            } catch (IllegalArgumentException e) {
                node.setFontWeight(MindMapNode.FontWeight.NORMAL);
            }
        }

        node.setRoot(rs.getBoolean("is_root"));
        node.setExpanded(rs.getBoolean("is_expanded"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            node.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            node.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return node;
    }

    // 更新节点的位置
    public boolean updateNodePosition(int nodeId, double x, double y) {
        String sql = "UPDATE mindmap_nodes SET x = ?, y = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, x);
            pstmt.setDouble(2, y);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(4, nodeId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("更新节点位置失败", e);
        }
        return false;
    }

    // 更新节点的尺寸
    public boolean updateNodeSize(int nodeId, double width, double height) {
        String sql = "UPDATE mindmap_nodes SET width = ?, height = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, width);
            pstmt.setDouble(2, height);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(4, nodeId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("更新节点尺寸失败", e);
        }
        return false;
    }

    // 查找根节点
    public MindMapNode findRootNode(int projectId) {
        String sql = "SELECT * FROM mindmap_nodes WHERE project_id = ? AND is_root = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToNode(rs);
            }
        } catch (SQLException e) {
            logger.error("查找根节点失败", e);
        }
        return null;
    }
}