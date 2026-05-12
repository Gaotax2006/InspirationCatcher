package com.inspiration.catcher.component;

import com.inspiration.catcher.manager.MindMapManager;
import com.inspiration.catcher.model.MindMapConnection;
import com.inspiration.catcher.model.MindMapNode;
import com.inspiration.catcher.model.Project;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MindMapView extends Pane {
    private static final Logger logger = LoggerFactory.getLogger(MindMapView.class);
    // 管理器引用
    private MindMapManager mindMapManager;
    private Pane rightCanvasPanel;
    // 右侧画布组件
    private Canvas canvas;
    private Group nodeGroup;
    // 交互状态
    private boolean isPanning = false;
    private double panStartX, panStartY;
    private double scale = 1.0;
    private final DoubleProperty translateX = new SimpleDoubleProperty(0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0);
    // 节点UI映射
    private final Map<MindMapNode, Node> nodeUIMap = new HashMap<>();
    // 存储菜单的引用
    private ContextMenu currentContextMenu = null;
    // 选中的节点
    private MindMapNode selectedNode = null;
    // 当前项目ID
    private Integer currentProjectId = null;

    private MindMapNode connectionSourceNode = null;
    private boolean isCreatingConnection = false;
    private MindMapNode connectingSource = null;
    private Line tempConnectionLine = null; // 临时连接线预览
    // 常量
    private static final double DEFAULT_NODE_WIDTH = 120;
    private static final double DEFAULT_NODE_HEIGHT = 50;
    private static final double MIN_SCALE = 0.3;
    private static final double MAX_SCALE = 3.0;
    private static final double SCALE_STEP = 0.04;

    public MindMapView() {
        initializeView();
        setupEventHandlers();
        setupDoubleClickHandler();
    }
    // 添加灵感跳转回调接口
    public interface IdeaJumpCallback { void jumpToIdea(Integer ideaId);}
    private IdeaJumpCallback ideaJumpCallback;
    // 设置回调
    public void setIdeaJumpCallback(IdeaJumpCallback callback) {this.ideaJumpCallback = callback;}
    // 初始化视图
    private void initializeView() {
        // 设置样式
        setStyle("-fx-background-color: #f8f9fa;");
        // 创建右侧画布面板
        rightCanvasPanel = createRightCanvasPanel();
        // 直接添加右侧画布面板，不再有左侧面板
        getChildren().add(rightCanvasPanel);
        // 监听画布大小变化，重新绘制
        canvas.widthProperty().addListener((_, _, _) -> redraw());
        canvas.heightProperty().addListener((_, _, _) -> redraw());
        logger.info("思维导图视图初始化完成（仅画布）");
    }
    // 设置管理器
    public void setMindMapManager(MindMapManager manager) {
        this.mindMapManager = manager;
        if (manager != null) {// 监听数据变化
            manager.getNodes().addListener((ListChangeListener<MindMapNode>) _ -> Platform.runLater(this::updateNodes));
            manager.getConnections().addListener((ListChangeListener<MindMapConnection>) _ -> Platform.runLater(this::updateConnections));
            // 设置数据变化监听器
            manager.setOnDataChangedListener(() -> Platform.runLater(this::updateNodes));
        }
    }
    // 设置当前项目
    public void setCurrentProject(Integer projectId) {
        this.currentProjectId = projectId;
        clear();
        // 加载项目思维导图数据
        if (mindMapManager != null && projectId != null) mindMapManager.loadProjectMindMap(projectId);
    }
    // 清空视图
    private void clear() {
        nodeUIMap.clear();
        nodeGroup.getChildren().clear();
        // connectionGroup removed
        selectedNode = null;
    }
    // 更新节点显示
    private void updateNodes() {
        if (mindMapManager == null) return;
        // 移除不存在的节点
        nodeUIMap.entrySet().removeIf(entry -> {
            if (!mindMapManager.getNodes().contains(entry.getKey())) {
                nodeGroup.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
        // 添加或更新节点
        for (MindMapNode node : mindMapManager.getNodes()) {
            if (!nodeUIMap.containsKey(node)) {
                Node uiNode = createNodeUI(node);
                nodeUIMap.put(node, uiNode);
                nodeGroup.getChildren().add(uiNode);
            } else updateNodeUI(node, nodeUIMap.get(node));
        }
        // 如果没有节点，显示提示
        if (mindMapManager.getNodes().isEmpty()) showEmptyState();
        else hideEmptyState();
        // 更新连接
        updateConnections();
    }
    // 显示空白状态提示
    private void showEmptyState() {
        // 如果已经存在提示，不重复添加
        for (javafx.scene.Node child : nodeGroup.getChildren())
            if (child.getUserData() != null && child.getUserData().equals("emptyState")) return;
        Text emptyText = new Text("当前项目思维导图为空\n\n双击画布添加第一个节点\n或从左侧列表拖拽灵感到此处");
        emptyText.setFont(Font.font("Arial", 14));
        emptyText.setFill(Color.GRAY);
        emptyText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        // 居中显示
        emptyText.setUserData("emptyState");
        emptyText.setTranslateX(getWidth() / 2 - 150);
        emptyText.setTranslateY(getHeight() / 2 - 50);
        nodeGroup.getChildren().add(emptyText);
    }
    // 隐藏空白状态提示
    private void hideEmptyState() {
        nodeGroup.getChildren().removeIf(node ->
                node.getUserData() != null && node.getUserData().equals("emptyState"));
    }
    // 创建节点UI
    private Node createNodeUI(MindMapNode node) {
        // 根据节点类型创建不同的UI
        return switch (node.getNodeType()) {
            case IDEA -> createIdeaNodeUI(node);
            case EXTERNAL -> createExternalNodeUI(node);
            default -> // CONCEPT
                    createConceptNodeUI(node);
        };
    }
    // 创建概念节点UI
    private Node createConceptNodeUI(MindMapNode node) {
        // 创建矩形
        Rectangle rect = new Rectangle();
        rect.setWidth(node.getWidth() > 0 ? node.getWidth() : DEFAULT_NODE_WIDTH);
        rect.setHeight(node.getHeight() > 0 ? node.getHeight() : DEFAULT_NODE_HEIGHT);
        // 设置样式（12px圆角 + 弥散阴影）
        rect.setFill(Color.web(node.getColor() != null ? node.getColor() : "#36B37E"));
        rect.setStroke(Color.web("#2E7D32"));
        rect.setStrokeWidth(1);
        rect.setArcWidth(24);
        rect.setArcHeight(24);
        rect.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.15)));
        // 创建文本
        Text text = new Text(node.getText());
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Arial",
                node.getFontWeight() == MindMapNode.FontWeight.BOLD ? FontWeight.BOLD : FontWeight.NORMAL,
                node.getFontSize() > 0 ? node.getFontSize() : 14));
        // 创建组
        Group group = new Group(rect, text);
        group.setTranslateX(node.getX());
        group.setTranslateY(node.getY());
        // 设置用户数据
        group.setUserData(node);
        // 设置文本居中
        Bounds textBounds = text.getBoundsInLocal();
        text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
        text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
        // 设置交互
        setupNodeInteraction(group, node);
        return group;
    }
    // 创建灵感节点UI
    private Node createIdeaNodeUI(MindMapNode node) {
        // 创建矩形（灵感节点有不同样式）
        Rectangle rect = new Rectangle();
        rect.setWidth(node.getWidth() > 0 ? node.getWidth() : DEFAULT_NODE_WIDTH + 20);
        rect.setHeight(node.getHeight() > 0 ? node.getHeight() : DEFAULT_NODE_HEIGHT + 10);

        // 设置样式（12px圆角 + 弥散阴影）
        rect.setFill(Color.web(node.getColor() != null ? node.getColor() : "#FF6B6B"));
        rect.setStroke(Color.web("#C62828"));
        rect.setStrokeWidth(2);
        rect.setArcWidth(24);
        rect.setArcHeight(24);
        rect.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.15)));
        // 创建标题文本
        Text titleText = new Text(node.getText());
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font("Arial", FontWeight.BOLD,
                node.getFontSize() > 0 ? node.getFontSize() : 14));
        // 创建组
        Group group = new Group(rect, titleText);
        group.setTranslateX(node.getX());
        group.setTranslateY(node.getY());
        // 设置用户数据
        group.setUserData(node);
        // 设置文本居中
        Bounds textBounds = titleText.getBoundsInLocal();
        titleText.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
        titleText.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
        // 设置交互
        setupNodeInteraction(group, node);
        return group;
    }
    // 创建外部节点UI
    private Node createExternalNodeUI(MindMapNode node) {
        // 创建圆角矩形
        Rectangle rect = new Rectangle();
        rect.setWidth(node.getWidth() > 0 ? node.getWidth() : 100);
        rect.setHeight(node.getHeight() > 0 ? node.getHeight() : 40);
        // 设置样式（12px圆角 + 弥散阴影）
        rect.setFill(Color.web(node.getColor() != null ? node.getColor() : "#FFD166"));
        rect.setStroke(Color.web("#FF9800"));
        rect.setStrokeWidth(1);
        rect.setArcWidth(24);
        rect.setArcHeight(24);
        rect.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.12)));
        // 创建文本
        Text text = new Text(node.getText());
        text.setFill(Color.BLACK);
        text.setFont(Font.font("Arial"));
        // 创建组
        Group group = new Group(rect, text);
        group.setTranslateX(node.getX());
        group.setTranslateY(node.getY());
        // 设置用户数据
        group.setUserData(node);
        // 设置文本居中
        Bounds textBounds = text.getBoundsInLocal();
        text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
        text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
        // 设置交互
        setupNodeInteraction(group, node);
        return group;
    }
    // 设置节点交互 - 修正拖拽公式
    private void setupNodeInteraction(Group group, MindMapNode node) {
        // 鼠标进入/离开效果
        group.setOnMouseEntered(e -> {
            // Ctrl按下时，显示连接模式的鼠标样式
            if (e.isControlDown()) group.setCursor(Cursor.CROSSHAIR);
            else {
                group.setScaleX(1.05);
                group.setScaleY(1.05);
                group.setCursor(Cursor.HAND);
            }
        });
        group.setOnMouseExited(_ -> {
            group.setScaleX(1.0);
            group.setScaleY(1.0);
            setCursor(Cursor.DEFAULT);
        });
        // 添加Ctrl键状态监听
        this.setOnKeyPressed(e -> {// Ctrl按下时，如果鼠标在节点上，改变鼠标样式
            if (e.isControlDown() && selectedNode != null)this.setCursor(Cursor.CROSSHAIR);
        });
        this.setOnKeyReleased(e -> {if (!e.isControlDown()) this.setCursor(Cursor.DEFAULT);});
        // 拖拽功能
        final double[] dragDelta = new double[2]; // 存储屏幕坐标系中的偏移
        final double[] initialScreenPos = new double[2]; // 存储按下时节点的屏幕位置
        group.setOnMousePressed(e -> {
            // 存储鼠标按下时的屏幕坐标
            initialScreenPos[0] = e.getSceneX();
            initialScreenPos[1] = e.getSceneY();
            // 选中节点
            selectedNode = node;
            highlightSelectedNode();
            if (e.isControlDown()) { // Ctrl+点击进入连接模式
                connectionSourceNode = node;
                isCreatingConnection = true;
                // 设置连接模式的鼠标样式
                group.setCursor(Cursor.CROSSHAIR);
                logger.info("开始创建连接，源节点: {}", node.getText());
                e.consume();
                return;
            }
            // 正常拖拽模式
            isCreatingConnection = false;
            connectionSourceNode = null;
            // 存储鼠标按下时的画布逻辑坐标和屏幕坐标
            dragDelta[0] = node.getX(); // 逻辑X
            dragDelta[1] = node.getY(); // 逻辑Y
            applyTransform();
            // 如果是右键，显示上下文菜单
            if (e.isSecondaryButtonDown()) {
                closeCurrentContextMenu();
                showNodeContextMenu(node, e.getScreenX(), e.getScreenY());
                e.consume();
            }
            e.consume();
        });
        group.setOnMouseDragged(e -> {
            // 如果在连接模式中，更新临时线
            if (isCreatingConnection && connectionSourceNode != null) {
                // 计算源节点中心点
                double sourceScreenX = connectionSourceNode.getX() * scale + translateX.get();
                double sourceScreenY = connectionSourceNode.getY() * scale + translateY.get();
                double sourceCenterX = sourceScreenX + (connectionSourceNode.getWidth() * scale) / 2;
                double sourceCenterY = sourceScreenY + (connectionSourceNode.getHeight() * scale) / 2;
                double mouseX = e.getSceneX() - initialScreenPos[0] + sourceCenterX;
                double mouseY = e.getSceneY() - initialScreenPos[1] + sourceCenterY;
                drawTemporaryConnection(sourceCenterX, sourceCenterY, mouseX, mouseY);
                e.consume();
                return;
            }
            // 计算鼠标移动的屏幕距离
            double deltaScreenX = e.getSceneX() - initialScreenPos[0];
            double deltaScreenY = e.getSceneY() - initialScreenPos[1];
            // 转换为画布逻辑距离
            double deltaCanvasX = deltaScreenX / scale;
            double deltaCanvasY = deltaScreenY / scale;
            // 计算新的画布逻辑坐标
            double newCanvasX = dragDelta[0] + deltaCanvasX;
            double newCanvasY = dragDelta[1] + deltaCanvasY;
            // 更新节点数据
            node.setX(newCanvasX);
            node.setY(newCanvasY);
            // 更新节点矩形大小
            if (group.getChildren().getFirst() instanceof Rectangle rect) {
                rect.setWidth(node.getWidth() * scale);
                rect.setHeight(node.getHeight() * scale);
                // 更新文本位置
                if (group.getChildren().size() > 1) {
                    Node textNode = group.getChildren().get(1);
                    if (textNode instanceof Text text) {
                        // 更新文本字体大小
                        double fontSize = node.getFontSize() * scale;
                        Font font = Font.font(text.getFont().getFamily(),
                                node.getFontWeight() == MindMapNode.FontWeight.BOLD ?
                                        FontWeight.BOLD : FontWeight.NORMAL,
                                fontSize);
                        text.setFont(font);
                        // 重新居中文本
                        Bounds textBounds = text.getBoundsInLocal();
                        text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
                        text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
                    }
                }
            }
            // 更新数据模型
            if (mindMapManager != null) mindMapManager.updateNodePosition(node.getId(), newCanvasX, newCanvasY);
            e.consume();
        });
        group.setOnMouseReleased(e -> {
            // 如果是创建连接模式
            if (isCreatingConnection && connectionSourceNode != null) {
                // 检查是否释放到了另一个节点上
                Node target = e.getPickResult().getIntersectedNode();
                // 向上查找，直到找到Group（节点UI）
                while (target != null && !(target instanceof Group && target.getUserData() instanceof MindMapNode))
                    target = target.getParent();
                if (target != null) {
                    Object userData = target.getUserData();
                    if (userData instanceof MindMapNode targetNode) // 确保不是同一个节点
                        if (targetNode != connectionSourceNode)// 创建连接
                             createConnectionBetweenNodes(connectionSourceNode, targetNode);
                }
                // 重置连接模式
                isCreatingConnection = false;
                connectionSourceNode = null;
                redrawConnections();
                group.setCursor(Cursor.DEFAULT);
                logger.info("连接创建完成或取消");
                e.consume();
                return;
            }
            // 确保UI完全更新
            redraw();
            e.consume();
        });
    }
    // 创建两个节点之间的连接
    private void createConnectionBetweenNodes(MindMapNode source, MindMapNode target) {
        if (mindMapManager != null) {
            // 检查连接是否已存在
            boolean connectionExists = false;
            for (MindMapConnection conn : mindMapManager.getConnections())
                if ((Objects.equals(conn.getSourceNodeId(), source.getId()) && Objects.equals(conn.getTargetNodeId(), target.getId())) ||
                        (Objects.equals(conn.getSourceNodeId(), target.getId()) && Objects.equals(conn.getTargetNodeId(), source.getId()))) {
                    connectionExists = true;
                    break;
                }
            if (!connectionExists) {
                // 弹出对话框让用户选择连接类型
                ChoiceDialog<String> dialog = new ChoiceDialog<>("相关",
                        "相关", "依赖", "扩展", "矛盾", "类比", "因果");
                dialog.setTitle("创建连接");
                dialog.setHeaderText("在节点之间创建连接");
                dialog.setContentText("选择连接类型:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(connectionTypeStr -> {
                    // 转换连接类型
                    MindMapConnection.ConnectionType connectionType =
                            MindMapConnection.ConnectionType.RELATED; // 默认
                    switch (connectionTypeStr) {
                        case "依赖" -> connectionType = MindMapConnection.ConnectionType.DEPENDS_ON;
                        case "扩展" -> connectionType = MindMapConnection.ConnectionType.EXTENDS;
                        case "矛盾" -> connectionType = MindMapConnection.ConnectionType.CONTRADICTS;
                        case "类比" -> connectionType = MindMapConnection.ConnectionType.ANALOGY;
                        case "因果" -> connectionType = MindMapConnection.ConnectionType.CAUSAL;
                    }
                    // 创建连接
                    mindMapManager.createConnection(source.getId(), target.getId(), connectionType);
                    // 更新UI
                    redraw();
                    logger.info("创建连接: {} -> {} (类型: {})", source.getText(), Optional.ofNullable(target.getText()), connectionTypeStr);
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("连接已存在");
                alert.setHeaderText("无法创建连接");
                alert.setContentText("这两个节点之间已经存在连接。");
                alert.showAndWait();
            }
        }
    }
    // 临时连接线绘制（在拖拽过程中显示）
    private void drawTemporaryConnection(double startX, double startY, double endX, double endY) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        redrawConnections();
        // 保存当前状态
        gc.save();
        // 绘制临时连接线（蓝色虚线）
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2 * Math.max(1, scale / 2));
        gc.setLineDashes(5 * Math.max(1, scale), 5 * Math.max(1, scale)); // 虚线
        // 绘制贝塞尔曲线
        double controlX = (endX + startX) * 0.5;
        gc.beginPath();
        gc.moveTo(startX, startY);
        gc.bezierCurveTo(controlX, startY, controlX, endY, endX, endY);
        gc.stroke();
        // 恢复状态
        gc.restore();
    }
    // 更新节点UI
    private void updateNodeUI(MindMapNode node, Node uiNode) {
        if (uiNode instanceof Group group) {
            // 更新位置
            group.setTranslateX(node.getX() * scale + translateX.get());
            group.setTranslateY(node.getY() * scale + translateY.get());
            // 更新文本
            if (group.getChildren().size() > 1) {
                Node textNode = group.getChildren().get(1);
                if (textNode instanceof Text text) {
                    text.setText(node.getText());
                    // 重新居中
                    Node rectNode = group.getChildren().getFirst();
                    if (rectNode instanceof Rectangle rect) {
                        Bounds textBounds = text.getBoundsInLocal();
                        text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
                        text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
                    }
                }
            }
        }
    }
    // 更新连接显示
    private void updateConnections() {
        if (mindMapManager == null) return;
        // 在画布上绘制连接线
        redrawConnections();
    }
    // 重绘连接线（在画布上绘制）
    private void redrawConnections() {
        if (mindMapManager == null || canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // 应用与节点组相同的变换
        gc.save();
        gc.scale(scale, scale);
        gc.translate(translateX.get() / scale, translateY.get() / scale);
        // 绘制所有连接
        for (MindMapConnection conn : mindMapManager.getConnections()) {
            MindMapNode sourceNode = mindMapManager.getNodeMap().get(conn.getSourceNodeId());
            MindMapNode targetNode = mindMapManager.getNodeMap().get(conn.getTargetNodeId());
            if (sourceNode != null && targetNode != null) drawConnection(gc, conn, sourceNode, targetNode);
        }
        gc.restore();
    }
    // 获取连接类型的颜色
    private Color getConnectionColor(MindMapConnection conn) {
        if (conn.getColor() != null && !conn.getColor().equals("#666666")) {
            return Color.web(conn.getColor());
        }
        return switch (conn.getConnectionType()) {
            case RELATED -> Color.web("#6C8EBF");       // 蓝色
            case DEPENDS_ON -> Color.web("#E67E22");    // 橙色
            case EXTENDS -> Color.web("#27AE60");       // 绿色
            case CONTRADICTS -> Color.web("#E74C3C");   // 红色
            case ANALOGY -> Color.web("#8E44AD");       // 紫色
            case CAUSAL -> Color.web("#2C3E50");        // 深灰
            case USES -> Color.web("#16A085");          // 青绿
        };
    }

    // 绘制单个连接
    private void drawConnection(GraphicsContext gc, MindMapConnection conn,
                                MindMapNode source, MindMapNode target) {
        // 计算连接线的起点和终点（从节点边缘开始）
        double startX = source.getX() + source.getWidth() / 2;
        double startY = source.getY() + source.getHeight() / 2;
        double endX = target.getX() + target.getWidth() / 2;
        double endY = target.getY() + target.getHeight() / 2;
        // 设置线条颜色（根据连接类型）
        Color lineColor = getConnectionColor(conn);
        gc.setStroke(lineColor);
        gc.setLineWidth(conn.getWidth());
        // 设置线条样式
        switch (conn.getStyle()) {
            case DASHED -> gc.setLineDashes(5, 5);
            case DOTTED -> gc.setLineDashes(2, 2);
            default -> gc.setLineDashes((double[]) null);// SOLID
        }
        // 绘制贝塞尔曲线
        double controlX = (startX + endX) * 0.5;
        gc.beginPath();
        gc.moveTo(startX, startY);
        gc.bezierCurveTo(controlX, startY, controlX, endY, endX, endY);
        gc.stroke();
        // 绘制箭头
        drawArrow(gc, startX, startY, endX, endY, lineColor);
    }
    // 绘制箭头
    private void drawArrow(GraphicsContext gc, double startX, double startY, double endX, double endY, Color color) {
        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowLength = 10;
        // 计算箭头点
        double arrowX1 = endX - arrowLength * Math.cos(angle - Math.PI / 6);
        double arrowY1 = endY - arrowLength * Math.sin(angle - Math.PI / 6);
        double arrowX2 = endX - arrowLength * Math.cos(angle + Math.PI / 6);
        double arrowY2 = endY - arrowLength * Math.sin(angle + Math.PI / 6);
        // 绘制箭头
        gc.setFill(color);
        gc.beginPath();
        gc.moveTo(endX, endY);
        gc.lineTo(arrowX1, arrowY1);
        gc.lineTo(arrowX2, arrowY2);
        gc.closePath();
        gc.fill();
    }
    // 显示节点上下文菜单
    private void showNodeContextMenu(MindMapNode node, double screenX, double screenY) {
        // 先关闭所有其他菜单
        closeCurrentContextMenu();
        ContextMenu contextMenu = new ContextMenu();
        // 编辑节点
        MenuItem editItem = new MenuItem("编辑节点");
        editItem.setOnAction(_ -> editNode(node));
        // 添加子节点
        MenuItem addChildItem = new MenuItem("添加子节点");
        addChildItem.setOnAction(_ -> addChildNode(node));
        // 删除节点
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(_ -> deleteNode(node));
        // 如果是灵感节点，添加跳转到灵感
        if (node.getNodeType() == MindMapNode.NodeType.IDEA && node.getIdeaId() != null) {
            MenuItem gotoIdeaItem = new MenuItem("跳转到灵感编辑");
            gotoIdeaItem.setOnAction(_ -> gotoIdea(node));
            contextMenu.getItems().add(gotoIdeaItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
        }
        contextMenu.getItems().addAll(editItem, addChildItem, deleteItem);
        contextMenu.show(this, screenX, screenY);
        // 记录当前菜单
        currentContextMenu = contextMenu;
        // 设置菜单关闭时的清理
        contextMenu.setOnHidden(_ -> {if (currentContextMenu == contextMenu) currentContextMenu = null;});
    }
    // 编辑节点
    private void editNode(MindMapNode node) {
        TextInputDialog dialog = new TextInputDialog(node.getText());
        dialog.setTitle("编辑节点");
        dialog.setHeaderText("修改节点文本");
        dialog.setContentText("请输入新的节点文本:");
        dialog.showAndWait().ifPresent(newText -> {
            if (!newText.trim().isEmpty() && !newText.equals(node.getText()))
                if (mindMapManager != null) mindMapManager.updateNodeText(node.getId(), newText.trim());
        });
    }
    // 添加子节点
    private void addChildNode(MindMapNode parentNode) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加子节点");
        dialog.setHeaderText("为 \"" + parentNode.getText() + "\" 添加子节点");
        dialog.setContentText("请输入子节点文本:");

        dialog.showAndWait().ifPresent(childText -> {
            if (!childText.trim().isEmpty()) {
                // 计算子节点位置（在父节点下方）
                double childX = parentNode.getX();
                double childY = parentNode.getY() + parentNode.getHeight() + 50;

                if (mindMapManager != null) {
                    // 创建概念节点
                    MindMapNode childNode = mindMapManager.createConceptNode(childText.trim(), childX, childY);
                    // 创建连接
                    if (childNode != null) mindMapManager.createConnection(parentNode.getId(), childNode.getId(),
                            MindMapConnection.ConnectionType.RELATED);
                }
            }
        });
    }
    // 删除节点
    private void deleteNode(MindMapNode node) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除节点");
        alert.setHeaderText("确认删除节点");
        alert.setContentText("确定要删除节点 \"" + node.getText() + "\" 吗？\n此操作将同时删除所有相关连接。");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) if (mindMapManager != null) mindMapManager.deleteNode(node.getId());
        });
    }
    // 跳转到灵感
    private void gotoIdea(MindMapNode node) {
        if (node.getIdeaId() != null && ideaJumpCallback != null) {
            logger.info("跳转到灵感: {}", node.getIdeaId());
            ideaJumpCallback.jumpToIdea(node.getIdeaId());
        } else logger.warn("无法跳转: 节点没有关联的灵感ID或回调未设置");
    }
    // 高亮选中节点
    private void highlightSelectedNode() {
        // 清除之前的高亮
        for (Node uiNode : nodeUIMap.values()) {
            if (uiNode instanceof Group group) {
                group.setScaleX(1.0);
                group.setScaleY(1.0);
                // 移除高亮边框
                if (group.getChildren().getFirst() instanceof Rectangle rect) rect.setStrokeWidth(1);
            }
        }

        // 高亮当前选中节点（放大+发光边框）
        Node selectedUINode = nodeUIMap.get(selectedNode);
        if (selectedUINode instanceof Group group) {
            group.setScaleX(1.1);
            group.setScaleY(1.1);
            // 添加高亮边框和阴影
            if (group.getChildren().getFirst() instanceof Rectangle rect) {
                rect.setStrokeWidth(3);
                rect.setStroke(Color.web("#4A90E2"));
                rect.setEffect(new javafx.scene.effect.DropShadow(12, Color.rgb(74, 144, 226, 0.4)));
            }
        }
    }
    // 设置双击处理器
    private void setupDoubleClickHandler() {// 双击延迟处理
        setOnMouseClicked(e -> {if (e.getClickCount() == 2) handleDoubleClick(e);});
    }
    // 处理双击
    private void handleDoubleClick(MouseEvent e) {
        // 关键修改：接受所有双击，不只是空白处
        if (mindMapManager != null && currentProjectId != null) {
            // 计算画布坐标（考虑缩放和平移）
            double canvasX = (e.getX() - translateX.get()) / scale;
            double canvasY = (e.getY() - translateY.get()) / scale;
            // 如果是双击节点，编辑节点
            if (e.getTarget() instanceof Node target)
                if (target.getUserData() instanceof MindMapNode node) {
                    editNode(node);
                    return;
                }
            // 否则创建新节点
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("新建节点");
            dialog.setHeaderText("在 (" + (int)canvasX + ", " + (int)canvasY + ") 创建新节点");
            dialog.setContentText("请输入节点文本:");
            dialog.showAndWait().ifPresent(text -> {
                if (!text.trim().isEmpty()) mindMapManager.createConceptNode(text.trim(), canvasX, canvasY);
            });
        }
    }
    // 应用变换（缩放和平移）- 修复版本
    private void applyTransform() {
        // 1移除节点的独立缩放，将缩放和平移都应用到节点位置上
        for (Map.Entry<MindMapNode, Node> entry : nodeUIMap.entrySet()) {
            MindMapNode node = entry.getKey();
            Node uiNode = entry.getValue();
            if (uiNode instanceof Group group) {
                // 计算节点在缩放后的屏幕坐标
                double screenX = node.getX() * scale + translateX.get();
                double screenY = node.getY() * scale + translateY.get();
                // 只设置平移，不设置缩放（缩放由画布统一处理）
                group.setScaleX(1.0);  // 移除独立缩放
                group.setScaleY(1.0);  // 移除独立缩放
                group.setTranslateX(screenX);
                group.setTranslateY(screenY);
                // 节点的视觉大小也要缩放
                if (group.getChildren().getFirst() instanceof Rectangle rect) {
                    rect.setWidth(node.getWidth() * scale);
                    rect.setHeight(node.getHeight() * scale);
                    // 更新文本位置（考虑缩放）
                    if (group.getChildren().size() > 1) {
                        Node textNode = group.getChildren().get(1);
                        if (textNode instanceof Text text) {
                            // 文本字体大小也要缩放
                            double fontSize = node.getFontSize() * scale;
                            Font font = Font.font(text.getFont().getFamily(),
                                    node.getFontWeight() == MindMapNode.FontWeight.BOLD ?
                                            FontWeight.BOLD : FontWeight.NORMAL,
                                    fontSize);
                            text.setFont(font);
                            // 重新居中文本
                            Bounds textBounds = text.getBoundsInLocal();
                            text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
                            text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
                        }
                    }
                }
            }
        }
    }
    // 重绘
    public void redraw() {applyTransform();redrawConnections();}
    // 自动布局
    public void applyAutoLayout() {if (mindMapManager != null) mindMapManager.applyTreeLayout();}
    // 居中视图
    public void centerView() {
        if (nodeUIMap.isEmpty()) return;

        // 计算所有节点的逻辑边界（使用节点数据而非UI组件）
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (MindMapNode node : mindMapManager.getNodes()) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX() + node.getWidth());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY() + node.getHeight());
        }

        // 计算中心点
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        // 获取画布尺寸
        Pane contentPane = (Pane) ((ScrollPane) ((BorderPane) rightCanvasPanel).getCenter()).getContent();
        double viewWidth = contentPane.getWidth();
        double viewHeight = contentPane.getHeight();

        // 将中心点移动到视图中心
        translateX.set(viewWidth / 2 - centerX * scale);
        translateY.set(viewHeight / 2 - centerY * scale);

        redraw();
    }
    // 重置视图
    public void resetView() {scale = 1.0;translateX.set(0);translateY.set(0);redraw();}
    // 放大
    public void zoomIn() {scale = Math.min(MAX_SCALE, scale + SCALE_STEP);redraw();}
    // 缩小
    public void zoomOut() {scale = Math.max(MIN_SCALE, scale - SCALE_STEP);redraw();}
    // 导出为图片
    public void exportToImage() {
        logger.info("导出思维导图为图片");
        try {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出思维导图为图片");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG 图片", "*.png"),
                    new FileChooser.ExtensionFilter("所有文件", "*.*")
            );

            // 设置默认文件名
            if (currentProjectId != null && mindMapManager != null) {
                Project project = mindMapManager.getCurrentProject();
                if (project != null) {
                    String projectName = project.getName().replaceAll("[^a-zA-Z0-9\\-]", "_");
                    fileChooser.setInitialFileName(projectName + "_思维导图.png");
                } else fileChooser.setInitialFileName("思维导图.png");
            }
            // 显示保存对话框
            File file = fileChooser.showSaveDialog(this.getScene().getWindow());
            if (file != null) {
                // 确保文件以 .png 结尾
                if (!file.getName().toLowerCase().endsWith(".png"))
                    file = new File(file.getAbsolutePath() + ".png");
                // 导出图片
                boolean success = saveMindMapToImage(file);
                if (success) {
                    logger.info("思维导图已成功导出到: {}", file.getAbsolutePath());
                    // 显示成功消息
                    File finalFile = file;
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("导出成功");
                        alert.setHeaderText("图片导出成功");
                        alert.setContentText("思维导图已成功导出到:\n" + finalFile.getAbsolutePath());
                        alert.showAndWait();
                    });
                } else throw new IOException("图片保存失败");
            } else logger.info("用户取消了导出操作");
        } catch (Exception e) {
            logger.error("导出思维导图为图片失败", e);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("导出失败");
                alert.setHeaderText("无法导出图片");
                alert.setContentText("导出过程中发生错误:\n" + e.getMessage());
                alert.showAndWait();
            });
        }
    }
    // 保存思维导图为图片（纯 JavaFX 实现）
    private boolean saveMindMapToImage(File file) {
        try {
            // 1. 创建临时的场景和舞台来渲染思维导图
            BorderPane exportContainer = new BorderPane();
            exportContainer.setStyle("-fx-background-color: white;");
            // 获取内容面板（包含所有节点和连接）
            Pane contentPane = (Pane) ((ScrollPane) ((BorderPane) rightCanvasPanel).getCenter()).getContent();
            if (contentPane == null) {logger.error("无法获取内容面板");return false;}
            // 2. 计算所有节点的边界
            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            boolean hasNodes = false;
            for (MindMapNode node : mindMapManager.getNodes()) {
                hasNodes = true;
                minX = Math.min(minX, node.getX());
                maxX = Math.max(maxX, node.getX() + node.getWidth());
                minY = Math.min(minY, node.getY());
                maxY = Math.max(maxY, node.getY() + node.getHeight());
            }
            if (!hasNodes) {logger.warn("没有节点可以导出");return false;}
            // 添加边距
            double margin = 50;
            double contentWidth = maxX - minX + 2 * margin;
            double contentHeight = maxY - minY + 2 * margin;
            // 3. 创建缩放因子，使图片大小合适
            double maxImageSize = 4000; // 最大图片尺寸
            double scale = 1.0;
            if (contentWidth > maxImageSize || contentHeight > maxImageSize) {
                scale = Math.min(maxImageSize / contentWidth, maxImageSize / contentHeight);
                contentWidth *= scale;
                contentHeight *= scale;
            }
            // 4. 创建一个 Group 来包含所有导出的元素
            Group exportGroup = new Group();
            // 复制所有节点到临时组
            for (MindMapNode node : mindMapManager.getNodes()) {
                Node uiNode = createNodeUIForExport(node);
                // 调整位置并应用缩放
                uiNode.setTranslateX((node.getX() - minX + margin) * scale);
                uiNode.setTranslateY((node.getY() - minY + margin) * scale);
                uiNode.setScaleX(scale);
                uiNode.setScaleY(scale);
                exportGroup.getChildren().add(uiNode);
            }
            // 5. 创建画布用于绘制连接线
            Canvas exportCanvas = new Canvas(contentWidth, contentHeight);
            GraphicsContext gc = exportCanvas.getGraphicsContext2D();
            // 设置白色背景
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, contentWidth, contentHeight);
            gc.save();
            gc.scale(scale, scale);
            gc.translate( margin- minX , margin- minY );
            // 绘制所有连接
            for (MindMapConnection conn : mindMapManager.getConnections()) {
                MindMapNode sourceNode = mindMapManager.getNodeMap().get(conn.getSourceNodeId());
                MindMapNode targetNode = mindMapManager.getNodeMap().get(conn.getTargetNodeId());
                drawConnection(gc, conn, sourceNode, targetNode);
            }
            // 6. 将画布和节点组添加到容器
            exportContainer.setCenter(new StackPane(exportCanvas, exportGroup));
            // 7. 创建临时的舞台和场景
            new Scene(exportContainer, contentWidth, contentHeight);
            // 8. 创建快照参数
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            // 9. 创建快照
            WritableImage image = exportContainer.snapshot(params, null);
            // 10. 使用 JavaFX 的 ImageIO 保存图片
            return saveImageWithJavaFX(image, file);
        } catch (Exception e) {logger.error("保存图片失败", e);return false;}
    }
    // 使用 Java 标准库保存图片（不依赖 SwingFXUtils）
    private boolean saveImageWithJavaFX(WritableImage fxImage, File file) {
        try {
            // 获取图片尺寸
            int width = (int) fxImage.getWidth();
            int height = (int) fxImage.getHeight();
            // 创建 BufferedImage
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                    width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB
            );
            // 使用 PixelReader 读取每个像素
            PixelReader pixelReader = fxImage.getPixelReader();
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                    bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            // 保存为 PNG
            javax.imageio.ImageIO.write(bufferedImage, "png", file);
            return true;
        } catch (Exception e) {logger.error("保存图片文件失败", e);return false;}
    }
    // 为导出创建节点UI
    private Node createNodeUIForExport(MindMapNode node) {
        // 创建矩形
        Rectangle rect = new Rectangle();
        rect.setWidth(node.getWidth());
        rect.setHeight(node.getHeight());
        // 设置颜色和样式
        rect.setFill(Color.web(node.getColor() != null ? node.getColor() : "#4A90E2"));
        rect.setStroke(Color.web("#2E7D32"));
        rect.setStrokeWidth(1);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        // 创建文本
        Text text = new Text(node.getText());
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Arial",
                node.getFontWeight() == MindMapNode.FontWeight.BOLD ? FontWeight.BOLD : FontWeight.NORMAL,
                node.getFontSize() > 0 ? node.getFontSize() : 14));
        // 创建组
        Group group = new Group(rect, text);
        // 设置文本居中
        Bounds textBounds = text.getBoundsInLocal();
        text.setTranslateX((rect.getWidth() - textBounds.getWidth()) / 2);
        text.setTranslateY((rect.getHeight() + textBounds.getHeight()) / 2 - 4);
        return group;
    }
    // 创建右侧画布面板
    private Pane createRightCanvasPanel() {
        // 创建主容器
        BorderPane canvasContainer = new BorderPane();
        canvasContainer.setStyle("-fx-background-color: #f8f9fa;");
        // 创建画布工具栏
        Node canvasToolbar = createCanvasToolbar();
        canvasContainer.setTop(canvasToolbar);
        // 创建画布容器
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        // 创建内容容器
        Pane contentPane = new Pane();
        contentPane.setPrefSize(950, 650); // 设置较大的固定大小，支持滚动
        // 创建画布用于绘制连接线
        canvas = new Canvas(950,650);
        canvas.widthProperty().bind(contentPane.prefWidthProperty());
        canvas.heightProperty().bind(contentPane.prefHeightProperty());
        // 创建分组用于管理节点和连接
        nodeGroup = new Group();
        // connectionGroup removed
        // 添加组件到内容容器
        contentPane.getChildren().addAll(canvas, nodeGroup);
        // 设置ScrollPane的内容
        scrollPane.setContent(contentPane);
        // 监听画布大小变化，重新绘制
        canvas.widthProperty().addListener((_, _, _) -> redraw());
        canvas.heightProperty().addListener((_, _, _) -> redraw());
        // 在 Canvas 上添加点击检测
        canvas.setOnMouseClicked(e -> {
            double clickX = (e.getX() - translateX.get()) / scale;
            double clickY = (e.getY() - translateY.get()) / scale;
            // 检测是否点击了连接线（简化方案：检测附近点）
            for (MindMapConnection conn : mindMapManager.getConnections()) {
                if (isPointNearConnection(clickX, clickY, conn)) {
                    showConnectionContextMenu(conn, e.getScreenX(), e.getScreenY());
                    e.consume();
                    return;
                }
            }closeCurrentContextMenu();
        });
        // 设置画布面板到容器
        canvasContainer.setCenter(scrollPane);
        // 设置画布面板可以扩展
        BorderPane.setMargin(scrollPane, new Insets(10));
        // 保存滚动面板引用
        return canvasContainer;
    }
    // 连接线附近检测（简化版）
    private boolean isPointNearConnection(double px, double py, MindMapConnection conn) {
        MindMapNode source = mindMapManager.findNodeById(conn.getSourceNodeId());
        MindMapNode target = mindMapManager.findNodeById(conn.getTargetNodeId());
        if (source == null || target == null) return false;
        // 计算点到直线距离（贝塞尔曲线近似）
        double startX = source.getX() + source.getWidth() / 2;
        double startY = source.getY() + source.getHeight() / 2;
        double endX = target.getX() + target.getWidth() / 2;
        double endY = target.getY() + target.getHeight() / 2;
        return distanceToLine(px, py, startX, startY, endX, endY) < 5; // 5像素阈值
    }
    private void showConnectionContextMenu(MindMapConnection conn, double screenX, double screenY) {
        closeCurrentContextMenu();
        ContextMenu menu = new ContextMenu();
        currentContextMenu = menu;
        MenuItem editItem = new MenuItem("编辑连接");
        editItem.setOnAction(_ -> editConnection(conn));
        MenuItem deleteItem = new MenuItem("删除连接");
        deleteItem.setOnAction(_ -> deleteConnection(conn));
        // 连接类型子菜单
        Menu typeMenu = new Menu("更改类型");
        for (MindMapConnection.ConnectionType type :
                MindMapConnection.ConnectionType.values()) {
            MenuItem typeItem = new MenuItem(type.getDisplayName());
            typeItem.setOnAction(_ -> changeConnectionType(conn, type));
            typeMenu.getItems().add(typeItem);
        }
        menu.getItems().addAll(editItem, typeMenu, new SeparatorMenuItem(), deleteItem);
        menu.show(canvas, screenX, screenY);
    }
    // 更新临时连接线
    private void updateTempConnectionLine(double endX, double endY) {
        if (tempConnectionLine != null) {
            tempConnectionLine.setEndX(endX);
            tempConnectionLine.setEndY(endY);
        }
    }
    // 取消连接
    private void cancelConnection() {
        if (tempConnectionLine != null) {
            // connectionGroup removed
            tempConnectionLine = null;
        }
        connectingSource = null;
        setCursor(Cursor.DEFAULT);
    }
    private double distanceToLine(double px, double py, double x1, double y1, double x2, double y2) {
        // 点到直线距离公式: |(y2-y1)*px - (x2-x1)*py + x2*y1 - y2*x1| / sqrt((y2-y1)^2 + (x2-x1)^2)
        double numerator = Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1);
        double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        // 避免除以零
        return denominator == 0 ? Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2)) : numerator / denominator;
    }
    // 编辑连接
    private void editConnection(MindMapConnection conn) {
        TextInputDialog dialog = new TextInputDialog(conn.getLabel());
        dialog.setTitle("编辑连接标签");
        dialog.setHeaderText("修改连接标签");
        dialog.setContentText("请输入连接标签:");

        dialog.showAndWait().ifPresent(newLabel -> {
            if (!newLabel.trim().equals(conn.getLabel())) {
                // TODO: 实现连接标签更新
                 mindMapManager.updateConnectionLabel(conn.getId(), newLabel);
                logger.info("连接标签更新: {}", newLabel);
            }
        });
    }
    // 删除连接
    private void deleteConnection(MindMapConnection conn) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除连接");
        alert.setHeaderText("确认删除连接");
        alert.setContentText("确定要删除这个连接吗？");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) if (mindMapManager != null) mindMapManager.deleteConnection(conn.getId());
        });
    }
    // 更改连接类型
    private void changeConnectionType(MindMapConnection conn,
                                      MindMapConnection.ConnectionType newType) {
        if (mindMapManager != null) {
            mindMapManager.updateConnectionType(conn.getId(), newType);
            redraw();
        }
    }
    // 创建画布工具栏
    private Node createCanvasToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5;");
        // 视图控制按钮
        Button zoomInBtn = new Button("🔍 放大");
        zoomInBtn.setTooltip(new Tooltip("放大视图"));
        zoomInBtn.setOnAction(_ -> zoomIn());
        Button zoomOutBtn = new Button("🔍 缩小");
        zoomOutBtn.setTooltip(new Tooltip("缩小视图"));
        zoomOutBtn.setOnAction(_ -> zoomOut());
        Button centerBtn = new Button("🎯 居中");
        centerBtn.setTooltip(new Tooltip("居中视图"));
        centerBtn.setOnAction(_ -> centerView());
        Button resetBtn = new Button("🔄 重置");
        resetBtn.setTooltip(new Tooltip("重置视图"));
        resetBtn.setOnAction(_ -> resetView());
        // 布局按钮
        Button autoLayoutBtn = new Button("📐 自动布局");
        autoLayoutBtn.setTooltip(new Tooltip("自动排列节点"));
        autoLayoutBtn.setOnAction(_ -> applyAutoLayout());
        // 导出按钮
        Button exportBtn = new Button("💾 导出图片");
        exportBtn.setTooltip(new Tooltip("导出为图片"));
        exportBtn.setOnAction(_ -> exportToImage());
        // 布局
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // 提示标签 - 单独放在一行
        Label hintLabel = new Label("从左侧可拖入灵感节点 | 双击空白处新建节点 | 按住Ctrl拖拽节点建立联系 | 中键拖拽平移 | 滚轮缩放");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-padding: 2 0 0 0;");
        toolbar.getItems().addAll(
                zoomInBtn, zoomOutBtn, centerBtn, resetBtn,
                new Separator(),
                autoLayoutBtn,
                exportBtn,
                spacer
        );
        VBox toolbarContainer = new VBox();
        toolbarContainer.getChildren().addAll(
                toolbar,
                hintLabel
        );
        toolbarContainer.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5;");
        return toolbarContainer;
    }

    // 调整事件处理中的坐标转换
    private void setupEventHandlers() {
        Pane contentPane = (Pane) ((ScrollPane) ((BorderPane) rightCanvasPanel).getCenter()).getContent();
        // 键盘事件处理
        this.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (selectedNode != null) {
                switch (e.getCode()) {
                    case DELETE -> {
                        deleteNode(selectedNode);
                        selectedNode = null;// 取消选中
                        e.consume();
                    }
                    case ESCAPE -> {
                        highlightSelectedNode(); // 清除高亮
                        selectedNode = null;// 取消选中
                        e.consume();
                    }
                }
            }
            // 全局快捷键
            if (e.isControlDown()) switch (e.getCode()) {
                case MINUS -> {zoomOut();e.consume();}
                case EQUALS -> {zoomIn();e.consume();}
            }
            // 键盘事件 - 按ESC取消连接
            if (e.getCode() == KeyCode.ESCAPE && connectingSource != null) {
                cancelConnection();e.consume();}
            if (currentContextMenu != null && currentContextMenu.isShowing()){
                closeCurrentContextMenu();e.consume();}
        });
        // 为右侧画布面板设置事件处理
        contentPane.setOnMousePressed(e -> {
            if (e.isMiddleButtonDown()) {
                isPanning = true;
                panStartX = e.getSceneX() - translateX.get();
                panStartY = e.getSceneY() - translateY.get();
                rightCanvasPanel.setCursor(Cursor.MOVE);
                e.consume();
            }
        });
        contentPane.setOnMouseDragged(e -> {
            if (isPanning) {
                translateX.set(e.getSceneX() - panStartX);
                translateY.set(e.getSceneY() - panStartY);
                redraw();
                e.consume();
            }
        });
        contentPane.setOnMouseReleased(e -> {
            if (isPanning) {
                isPanning = false;
                rightCanvasPanel.setCursor(Cursor.DEFAULT);
                e.consume();
            }
        });
        // 缩放（鼠标滚轮）- 用 addEventFilter 防止被 ScrollPane 截获
        contentPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY() > 0 ? SCALE_STEP : -SCALE_STEP;
            double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale + delta));
            if (newScale != scale) {
                // 更精确的坐标转换
                Point2D contentLocal = contentPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                double canvasMouseX = contentLocal.getX();
                double canvasMouseY = contentLocal.getY();
                double scaleFactor = newScale / scale;
                translateX.set(canvasMouseX - (canvasMouseX - translateX.get()) * scaleFactor);
                translateY.set(canvasMouseY - (canvasMouseY - translateY.get()) * scaleFactor);
                scale = newScale;
                redraw();
            }
            e.consume();
        });
        // 拖拽目标设置（右侧画布接受拖拽）
        setupDragTarget();
        // 全局鼠标移动事件（更新临时连接线）
        setOnMouseMoved(e -> {
            if (connectingSource != null && tempConnectionLine != null)
                updateTempConnectionLine(e.getSceneX(), e.getSceneY());
        });
    }
    // 设置右侧画布为拖拽目标
    private void setupDragTarget() {
        Pane contentPane = (Pane) ((ScrollPane) ((BorderPane) rightCanvasPanel).getCenter()).getContent();
        contentPane.setOnDragOver(event -> {
            if (event.getGestureSource() != contentPane &&
                    event.getDragboard().hasString() &&
                    event.getDragboard().getString().startsWith("idea:")) {
                logger.debug("拖拽进入画布区域: {}", event.getDragboard().getString());
                event.acceptTransferModes(TransferMode.COPY);
                contentPane.setCursor(Cursor.CLOSED_HAND);
            }
            event.consume();
        });
        contentPane.setOnDragExited(event -> {
            logger.debug("拖拽离开画布区域");
            contentPane.setCursor(Cursor.DEFAULT);
            event.consume();
        });
        contentPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString() && db.getString().startsWith("idea:")) {
                String data = db.getString();
                String[] parts = data.split(":");
                if (parts.length == 2 && mindMapManager != null) {
                    try {
                        int ideaId = Integer.parseInt(parts[1]);
                        // 将屏幕坐标转换为画布逻辑坐标（反转 scale 和 translate）
                        Point2D paneLocal = contentPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                        double canvasX = (paneLocal.getX() - translateX.get()) / scale;
                        double canvasY = (paneLocal.getY() - translateY.get()) / scale;
                        mindMapManager.createIdeaNode(ideaId, canvasX, canvasY);
                        success = true;
                    } catch (NumberFormatException e) {logger.error("解析灵感ID失败: {}", data, e);}
                }
            }
            event.setDropCompleted(success);
            contentPane.setCursor(Cursor.DEFAULT);
            redraw();
            event.consume();
        });
    }
    // 关闭当前打开的上下文菜单
    private void closeCurrentContextMenu() {
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            currentContextMenu.hide();currentContextMenu = null;
        }
    }
}