package com.inspiration.catcher.dao;

import com.inspiration.catcher.model.MindMapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MindMapConnectionDao {
    private static final Logger logger = LoggerFactory.getLogger(MindMapConnectionDao.class);
    // 保存连接
    public MindMapConnection save(MindMapConnection connection) {
        if (connection == null) return null;
        String sql = "INSERT INTO mindmap_connections (project_id, source_node_id, target_node_id, " +
                "connection_type, label, color, width, style, strength) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, connection.getProjectId());
            pstmt.setInt(2, connection.getSourceNodeId());
            pstmt.setInt(3, connection.getTargetNodeId());
            pstmt.setString(4, connection.getConnectionType().name());
            pstmt.setString(5, connection.getLabel());
            pstmt.setString(6, connection.getColor());
            pstmt.setInt(7, connection.getWidth());
            pstmt.setString(8, connection.getStyle().name());
            pstmt.setDouble(9, connection.getStrength());

            if (pstmt.executeUpdate() > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        connection.setId(generatedId);
                        return connection;
                    }
                }
            }
        } catch (SQLException e) {logger.error("保存思维导图连接失败", e);}
        return null;
    }

    // 根据项目ID查询所有连接
    public List<MindMapConnection> findByProjectId(int projectId) {
        List<MindMapConnection> connections = new ArrayList<>();
        String sql = "SELECT * FROM mindmap_connections WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                MindMapConnection connection = resultSetToConnection(rs);
                connections.add(connection);
            }
        } catch (SQLException e) {
            logger.error("查询思维导图连接失败", e);
        }
        return connections;
    }

    // 根据ID查询
    public MindMapConnection findById(int id) {
        String sql = "SELECT * FROM mindmap_connections WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToConnection(rs);
            }
        } catch (SQLException e) {
            logger.error("查询思维导图连接失败", e);
        }
        return null;
    }

    // 删除连接
    public boolean delete(int id) {
        String sql = "DELETE FROM mindmap_connections WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("删除思维导图连接失败", e);
        }
        return false;
    }

    // 删除项目的所有连接
    public boolean deleteByProjectId(int projectId) {
        String sql = "DELETE FROM mindmap_connections WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("删除项目思维导图连接失败", e);
        }
        return false;
    }
    // 辅助方法：ResultSet转对象
    private MindMapConnection resultSetToConnection(ResultSet rs) throws SQLException {
        MindMapConnection connection = new MindMapConnection();
        connection.setId(rs.getInt("id"));
        connection.setProjectId(rs.getInt("project_id"));
        connection.setSourceNodeId(rs.getInt("source_node_id"));
        connection.setTargetNodeId(rs.getInt("target_node_id"));

        // 处理枚举类型
        String typeStr = rs.getString("connection_type");
        if (typeStr != null) {
            try {
                connection.setConnectionType(MindMapConnection.ConnectionType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                connection.setConnectionType(MindMapConnection.ConnectionType.RELATED);
            }
        }

        connection.setLabel(rs.getString("label"));
        connection.setColor(rs.getString("color"));
        connection.setWidth(rs.getInt("width"));

        String styleStr = rs.getString("style");
        if (styleStr != null) {
            try {
                connection.setStyle(MindMapConnection.ConnectionStyle.valueOf(styleStr));
            } catch (IllegalArgumentException e) {
                connection.setStyle(MindMapConnection.ConnectionStyle.SOLID);
            }
        }
        connection.setStrength(rs.getDouble("strength"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) connection.setCreatedAt(createdAt.toLocalDateTime());
        return connection;
    }

    // 获取项目连接数量
    public int getConnectionCount(int projectId) {
        String sql = "SELECT COUNT(*) FROM mindmap_connections WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("获取连接数量失败", e);
        }
        return 0;
    }
}