package com.inspiration.catcher.manager;

import com.inspiration.catcher.dao.MindMapNodeDao;
import com.inspiration.catcher.dao.MindMapConnectionDao;
import com.inspiration.catcher.model.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class MindMapManager {
    private static final Logger logger = LoggerFactory.getLogger(MindMapManager.class);
    private final MindMapNodeDao nodeDao = new MindMapNodeDao();
    private final MindMapConnectionDao connectionDao = new MindMapConnectionDao();
    private final IdeaManager ideaManager;
    // 当前项目
    private final ObjectProperty<Project> currentProject = new SimpleObjectProperty<>();
    // 当前思维导图数据
    private final ObservableList<MindMapNode> nodes = FXCollections.observableArrayList();
    private final ObservableList<MindMapConnection> connections = FXCollections.observableArrayList();
    // 节点映射，便于快速查找
    private final Map<Integer, MindMapNode> nodeMap = new HashMap<>();
    // 监听器：当节点或连接变化时通知UI
    private Runnable onDataChangedListener;
    public MindMapManager(IdeaManager ideaManager) {
        this.ideaManager = ideaManager;
        // 监听当前项目变化
        currentProject.addListener((_, _, newProject) -> {
            if (newProject != null) loadProjectMindMap(newProject.getId());
        });
    }
    // 加载项目的思维导图数据
    public void loadProjectMindMap(int projectId) {
        logger.info("加载项目思维导图数据: projectId={}", projectId);
        try {
            // 清空现有数据
            clear();
            // 加载节点
            List<MindMapNode> projectNodes = nodeDao.findByProjectId(projectId);
            nodes.addAll(projectNodes);
            // 建立节点映射
            for (MindMapNode node : projectNodes) nodeMap.put(node.getId(), node);
            // 加载连接
            List<MindMapConnection> projectConnections = connectionDao.findByProjectId(projectId);
            connections.addAll(projectConnections);
            logger.info("思维导图数据加载完成: {} 个节点, {} 个连接", nodes.size(), connections.size());
            // 如果没有节点，创建根节点
            if (nodes.isEmpty()) createRootNode(projectId);
            // 触发数据变化监听
            if (onDataChangedListener != null) onDataChangedListener.run();
        } catch (Exception e) {logger.error("加载思维导图数据失败", e);}
    }
    // 清空数据
    private void clear() {nodes.clear();connections.clear();nodeMap.clear();}
    // 创建根节点
    private void createRootNode(int projectId) {
        logger.info("为项目创建根节点: projectId={}", projectId);
        MindMapNode root = new MindMapNode();
        root.setProjectId(projectId);
        root.setText("中心主题");
        root.setDescription("双击编辑主题内容");
        root.setNodeType(MindMapNode.NodeType.CONCEPT);
        root.setRoot(true);
        root.setExpanded(true);
        // 设置初始位置和尺寸
        root.setX(400);
        root.setY(300);
        root.setWidth(120);
        root.setHeight(60);
        root.setColor("#4A90E2");
        root.setFontSize(16);
        root.setFontWeight(MindMapNode.FontWeight.BOLD);
        MindMapNode savedRoot = nodeDao.save(root);
        if (savedRoot != null) {
            nodes.add(savedRoot);
            nodeMap.put(savedRoot.getId(), savedRoot);
            logger.info("根节点创建成功: ID={}", savedRoot.getId());
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 创建概念节点（不关联灵感）
    public MindMapNode createConceptNode(String text, double x, double y) {
        if (currentProject.get() == null) {
            logger.error("无法创建概念节点：当前项目为null");
            return null;
        }
        MindMapNode node = new MindMapNode();
        node.setProjectId(currentProject.get().getId());
        node.setText(text);
        node.setDescription("");
        node.setNodeType(MindMapNode.NodeType.CONCEPT);
        node.setRoot(false);
        node.setExpanded(true);
        // 设置位置和尺寸
        node.setX(x);
        node.setY(y);
        node.setWidth(100);
        node.setHeight(40);
        node.setColor("#36B37E"); // 概念节点用绿色
        node.setFontSize(14);
        MindMapNode savedNode = nodeDao.save(node);
        if (savedNode != null) {
            nodes.add(savedNode);
            nodeMap.put(savedNode.getId(), savedNode);
            logger.info("概念节点创建成功: ID={}, text={}", savedNode.getId(), text);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
        return savedNode;
    }
    // 创建灵感节点（关联现有灵感）
    public void createIdeaNode(int ideaId, double x, double y) {
        if (currentProject.get() == null) {
            logger.error("无法创建灵感节点：当前项目为null");
            return;
        }
        // 检查是否已存在关联此灵感的节点
        MindMapNode existingNode = nodeDao.findByIdeaId(ideaId);
        if (existingNode != null) {
            logger.warn("灵感已在思维导图中存在: ideaId={}, nodeId={}", ideaId, existingNode.getId());
            return;
        }
        // 获取灵感信息
        Idea idea = ideaManager.getIdeaById(ideaId);
        if (idea == null) {
            logger.error("无法找到灵感: ideaId={}", ideaId);
            return;
        }
        MindMapNode node = new MindMapNode();
        node.setProjectId(currentProject.get().getId());
        node.setIdeaId(ideaId);
        node.setText(idea.getTitle());
        node.setDescription(idea.getContent());
        node.setNodeType(MindMapNode.NodeType.IDEA);
        node.setRoot(false);
        node.setExpanded(true);
        // 设置位置和尺寸
        node.setX(x);
        node.setY(y);
        node.setWidth(120);
        node.setHeight(50);
        node.setColor("#FF6B6B"); // 灵感节点用红色
        node.setFontSize(14);
        node.setFontWeight(MindMapNode.FontWeight.BOLD);
        MindMapNode savedNode = nodeDao.save(node);
        if (savedNode != null) {
            nodes.add(savedNode);
            nodeMap.put(savedNode.getId(), savedNode);
            logger.info("灵感节点创建成功: ID={}, ideaId={}", savedNode.getId(), ideaId);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 创建外部链接节点
    public void createExternalNode(String text, String url, double x, double y) {
        if (currentProject.get() == null) {
            logger.error("无法创建外部节点：当前项目为null");
            return;
        }
        MindMapNode node = new MindMapNode();
        node.setProjectId(currentProject.get().getId());
        node.setText(text);
        node.setDescription(url); // 使用description字段存储URL
        node.setNodeType(MindMapNode.NodeType.EXTERNAL);
        node.setRoot(false);
        node.setExpanded(false);
        // 设置位置和尺寸
        node.setX(x);
        node.setY(y);
        node.setWidth(100);
        node.setHeight(40);
        node.setColor("#FFD166"); // 外部节点用黄色
        node.setFontSize(14);
        node.setFontStyle("italic");
        MindMapNode savedNode = nodeDao.save(node);
        if (savedNode != null) {
            nodes.add(savedNode);
            nodeMap.put(savedNode.getId(), savedNode);
            logger.info("外部节点创建成功: ID={}, text={}", savedNode.getId(), text);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 创建两个节点之间的连接
    public void createConnection(int sourceNodeId, int targetNodeId,
                                 MindMapConnection.ConnectionType type) {
        if (currentProject.get() == null) {
            logger.error("无法创建连接：当前项目为null");
            return;
        }
        // 检查连接是否已存在
        for (MindMapConnection conn : connections) {
            if ((conn.getSourceNodeId() == sourceNodeId && conn.getTargetNodeId() == targetNodeId) ||
                    (conn.getSourceNodeId() == targetNodeId && conn.getTargetNodeId() == sourceNodeId)) {
                logger.warn("连接已存在: source={}, target={}", sourceNodeId, targetNodeId);
                return;
            }
        }
        MindMapConnection connection = new MindMapConnection();
        connection.setProjectId(currentProject.get().getId());
        connection.setSourceNodeId(sourceNodeId);
        connection.setTargetNodeId(targetNodeId);
        connection.setConnectionType(type);
        connection.setLabel(type.getDisplayName());
        connection.setColor("#666666");
        connection.setWidth(2);
        connection.setStyle(MindMapConnection.ConnectionStyle.SOLID);
        connection.setStrength(0.5);
        MindMapConnection savedConnection = connectionDao.save(connection);
        if (savedConnection != null) {
            connections.add(savedConnection);
            logger.info("连接创建成功: ID={}, source={}, target={}",
                    savedConnection.getId(), sourceNodeId, targetNodeId);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 更新节点位置（仅持久化，不触发UI重建——JGraphX已实时更新视觉位置）
    public void updateNodePosition(int nodeId, double x, double y) {
        MindMapNode node = nodeMap.get(nodeId);
        if (node == null) {logger.error("无法更新节点位置：节点不存在, nodeId={}", nodeId);return;}
        if (nodeDao.updateNodePosition(nodeId, x, y)) {
            node.setX(x);node.setY(y);
        }
    }
    // 更新节点文本
    public void updateNodeText(int nodeId, String text) {
        MindMapNode node = nodeMap.get(nodeId);
        if (node == null) {
            logger.error("无法更新节点文本：节点不存在, nodeId={}", nodeId);
            return;
        }
        node.setText(text);
        node.setUpdatedAt(LocalDateTime.now());
        MindMapNode updatedNode = nodeDao.save(node);
        if (updatedNode != null) {
            // 更新映射
            nodeMap.put(nodeId, updatedNode);
            int index = nodes.indexOf(node);
            if (index != -1) nodes.set(index, updatedNode);
            logger.info("节点文本更新成功: nodeId={}, text={}", nodeId, text);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 删除节点及其相关连接
    public void deleteNode(int nodeId) {
        MindMapNode node = nodeMap.get(nodeId);
        if (node == null) {
            logger.error("无法删除节点：节点不存在, nodeId={}", nodeId);
            return;
        }
        // 如果是根节点且是唯一的节点，不能删除
        if (node.isRoot() && nodes.size() == 1) {logger.warn("不能删除唯一的根节点");return;}
        // 删除所有与此节点相关的连接
        List<MindMapConnection> connectionsToRemove = new ArrayList<>();
        for (MindMapConnection conn : connections)
            if (conn.getSourceNodeId() == nodeId || conn.getTargetNodeId() == nodeId) connectionsToRemove.add(conn);
        for (MindMapConnection conn : connectionsToRemove) {
            connectionDao.delete(conn.getId());connections.remove(conn);}
        // 删除节点
        boolean success = nodeDao.delete(nodeId);
        if (success) {
            nodes.remove(node);
            nodeMap.remove(nodeId);
            logger.info("节点删除成功: nodeId={}, 同时删除了 {} 个连接",
                    nodeId, connectionsToRemove.size());
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 删除连接
    public void deleteConnection(int connectionId) {
        MindMapConnection connection = findConnectionById(connectionId);
        if (connection == null) {
            logger.error("无法删除连接：连接不存在, connectionId={}", connectionId);
            return;
        }
        boolean success = connectionDao.delete(connectionId);
        if (success) {
            connections.remove(connection);
            logger.info("连接删除成功: connectionId={}", connectionId);
            if (onDataChangedListener != null) onDataChangedListener.run();
        }

    }
    // 根据ID查找连接
    private MindMapConnection findConnectionById(int connectionId) {
        for (MindMapConnection conn : connections) if(conn.getId() == connectionId) return conn;
        return null;
    }
    // 获取节点的子节点（通过连接）
    public List<MindMapNode> getChildNodes(int parentNodeId) {
        List<MindMapNode> childNodes = new ArrayList<>();
        for (MindMapConnection conn : connections)
            if (conn.getSourceNodeId() == parentNodeId) {
                MindMapNode child = nodeMap.get(conn.getTargetNodeId());
                if (child != null) childNodes.add(child);
            }
        return childNodes;
    }
    // 获取节点的父节点（通过连接）
    public List<MindMapNode> getParentNodes(int childNodeId) {
        List<MindMapNode> parentNodes = new ArrayList<>();

        for (MindMapConnection conn : connections) {
            if (conn.getTargetNodeId() == childNodeId) {
                MindMapNode parent = nodeMap.get(conn.getSourceNodeId());
                if (parent != null) {
                    parentNodes.add(parent);
                }
            }
        }

        return parentNodes;
    }
    // 应用简单的自动布局（树状布局）
    public void applyTreeLayout() {
        if (nodes.isEmpty()) return;
        // 查找根节点
        MindMapNode root = null;
        for (MindMapNode node : nodes)
            if (node.isRoot()) {
                root = node;
                break;
            }

        if (root == null && !nodes.isEmpty()) root = nodes.getFirst();
        if (root == null) return;
        // 设置根节点位置
        root.setX(400);
        root.setY(100);
        updateNodePosition(root.getId(), root.getX(), root.getY());

        // 递归布局子节点
        layoutChildren(root, 0, 400, 100, 150, 100);
    }
    // 递归布局子节点
    private void layoutChildren(MindMapNode parent, int level,
                                double parentX, double parentY,
                                double horizontalSpacing, double verticalSpacing) {
        List<MindMapNode> children = getChildNodes(parent.getId());
        if (children.isEmpty()) return;

        double startX = parentX - (children.size() - 1) * horizontalSpacing / 2;

        for (int i = 0; i < children.size(); i++) {
            MindMapNode child = children.get(i);
            double childX = startX + i * horizontalSpacing;
            double childY = parentY + verticalSpacing;

            child.setX(childX);
            child.setY(childY);
            updateNodePosition(child.getId(), childX, childY);

            layoutChildren(child, level + 1, childX, childY,
                    horizontalSpacing * 0.7, verticalSpacing);
        }
    }
    // 添加数据变化监听器
    public void setOnDataChangedListener(Runnable listener) {
        this.onDataChangedListener = listener;
    }
    // Getters
    public ObservableList<MindMapNode> getNodes() {
        return nodes;
    }
    public ObservableList<MindMapConnection> getConnections() {
        return connections;
    }
    public Map<Integer, MindMapNode> getNodeMap() {return nodeMap;}
    public void setCurrentProject(Project project) {
        currentProject.set(project);
        if (project != null) {
            // 加载思维导图数据
            loadProjectMindMap(project.getId());
            // 通知UI更新侧边栏（通过监听器）
            if (onDataChangedListener != null) onDataChangedListener.run();
        }
    }
    // 获取当前项目的统计信息
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        if (currentProject.get() == null) return stats;
        int projectId = currentProject.get().getId();
        stats.put("nodeCount", nodeDao.getNodeCount(projectId));
        stats.put("connectionCount", connectionDao.getConnectionCount(projectId));
        // 统计节点类型
        int ideaNodeCount = 0;
        int conceptNodeCount = 0;
        int externalNodeCount = 0;
        for (MindMapNode node : nodes) {
            switch (node.getNodeType()) {
                case IDEA -> ideaNodeCount++;
                case CONCEPT -> conceptNodeCount++;
                case EXTERNAL -> externalNodeCount++;
            }
        }
        stats.put("ideaNodeCount", ideaNodeCount);
        stats.put("conceptNodeCount", conceptNodeCount);
        stats.put("externalNodeCount", externalNodeCount);
        return stats;
    }
    // 递归添加子节点文本
    private void appendChildrenText(StringBuilder sb, MindMapNode parent, int level) {
        List<MindMapNode> children = getChildNodes(parent.getId());
        for (MindMapNode child : children) {
            String indent = "  ".repeat(level);
            sb.append(indent).append("- ").append(child.getText());
            if (child.getNodeType() == MindMapNode.NodeType.IDEA && child.getIdeaId() != null)
                sb.append(" [灵感ID: ").append(child.getIdeaId()).append("]");
            sb.append("\n");
            appendChildrenText(sb, child, level + 1);
        }
    }
    public MindMapNode findNodeById(int nodeId) {return nodeMap.get(nodeId);}
    // 更新连接标签
    public boolean updateConnectionLabel(int connectionId, String label) {
        for (MindMapConnection conn : connections) {
            if (conn.getId() == connectionId) {
                conn.setLabel(label);
                boolean saved = connectionDao.update(conn);
                logger.info("更新连接标签: {} -> {}, 持久化: {}", connectionId, label, saved);
                if (onDataChangedListener != null) onDataChangedListener.run();
                return saved;
            }
        }
        return false;
    }
    // 更新连接类型
    public boolean updateConnectionType(int connectionId,
                                        MindMapConnection.ConnectionType newType) {
        for (MindMapConnection conn : connections) {
            if (conn.getId() == connectionId) {
                conn.setConnectionType(newType);
                conn.setLabel(newType.getDisplayName());
                boolean saved = connectionDao.update(conn);
                logger.info("更新连接类型: {} -> {}, 持久化: {}", connectionId, newType, saved);
                if (onDataChangedListener != null) onDataChangedListener.run();
                return saved;
            }
        }
        return false;
    }
    public Project getCurrentProject() {return currentProject.get();}
}