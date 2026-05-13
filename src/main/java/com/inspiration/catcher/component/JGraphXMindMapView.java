package com.inspiration.catcher.component;

import com.inspiration.catcher.manager.MindMapManager;
import com.inspiration.catcher.model.*;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.view.mxGraph;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JGraphXMindMapView extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(JGraphXMindMapView.class);

    private final SwingNode swingNode = new SwingNode();
    private mxGraph graph;
    private mxGraphComponent graphComponent;
    private MindMapManager mindMapManager;
    private final Map<Integer, mxCell> nodeCellMap = new ConcurrentHashMap<>();
    private final Map<Integer, mxCell> edgeCellMap = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    private volatile boolean isInternalUpdate = false;
    private volatile boolean syncQueued = false;


    public JGraphXMindMapView() {
        setFillWidth(true);
        setStyle("-fx-background-color: #f8f9fa;");
        buildToolbar();
        VBox.setVgrow(swingNode, Priority.ALWAYS);
        getChildren().add(swingNode);
    }

    private void buildToolbar() {
        Button zoomInBtn = new Button("放大");
        zoomInBtn.setOnAction(_ -> zoomIn());
        Button zoomOutBtn = new Button("缩小");
        zoomOutBtn.setOnAction(_ -> zoomOut());
        Button autoLayoutBtn = new Button("自动布局");
        autoLayoutBtn.setOnAction(_ -> applyAutoLayout());
        Button centerBtn = new Button("居中");
        centerBtn.setOnAction(_ -> centerView());

        Label hint = new Label("拖拽移动 · 双击编辑 · 滚轮缩放");
        hint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        HBox toolbarBox = new HBox(6);
        toolbarBox.setFillHeight(true);
        toolbarBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5 10; -fx-alignment: center-left;");
        toolbarBox.getChildren().addAll(zoomInBtn, zoomOutBtn, centerBtn, new Separator(), autoLayoutBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbarBox.getChildren().addAll(spacer, hint);
        getChildren().add(toolbarBox);
    }

    // ============================================================
    //  Initialization — call once after view is attached to scene
    // ============================================================

    /** Must be called once after the view is added to a scene. */
    public void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        logger.info("Initializing JGraphX engine");

        graph = new mxGraph();
        graph.setCellsResizable(false);
        graph.setCellsMovable(true);
        graph.setAllowDanglingEdges(false);
        graph.setConnectableEdges(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        graph.setMultigraph(false);
        graph.setAllowLoops(false);
        graph.setBorder(100);
        graph.setAutoSizeCells(true);
        graph.setHtmlLabels(true);

        // Vertex styles — warm palette matching "Warm Paper" theme
        Map<String, Object> vs = graph.getStylesheet().getDefaultVertexStyle();
        vs.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        vs.put(mxConstants.STYLE_ARCSIZE, 16);
        vs.put(mxConstants.STYLE_FILLCOLOR, "#C4843C");
        vs.put(mxConstants.STYLE_GRADIENTCOLOR, "#D4A76A");
        vs.put(mxConstants.STYLE_FONTCOLOR, "#FFFFFF");
        vs.put(mxConstants.STYLE_FONTSIZE, 13);
        vs.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        vs.put(mxConstants.STYLE_ALIGN, "center");
        vs.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
        vs.put(mxConstants.STYLE_WHITE_SPACE, "wrap");
        vs.put(mxConstants.STYLE_SHADOW, true);

        // Edge styles — warm grey, organic curve
        Map<String, Object> es = graph.getStylesheet().getDefaultEdgeStyle();
        es.put(mxConstants.STYLE_STROKECOLOR, "#C4B8A8");
        es.put(mxConstants.STYLE_STROKEWIDTH, 2);
        es.put(mxConstants.STYLE_ROUNDED, true);
        es.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ENTITY_RELATION);
        es.put("curved", true);
        es.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        es.put(mxConstants.STYLE_ENDSIZE, 8);
        es.put(mxConstants.STYLE_FONTSIZE, 11);
        es.put(mxConstants.STYLE_FONTCOLOR, "#9A9088");
        es.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#F7F4F0");
        es.put(mxConstants.STYLE_EXIT_X, 1.0);
        es.put(mxConstants.STYLE_EXIT_Y, 0.5);
        es.put(mxConstants.STYLE_ENTRY_X, 0.0);
        es.put(mxConstants.STYLE_ENTRY_Y, 0.5);

        graphComponent = new mxGraphComponent(graph);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(new Color(0xf8, 0xf9, 0xfa));
        graphComponent.setBackground(new Color(0xf8, 0xf9, 0xfa));
        graphComponent.setWheelScrollingEnabled(false);

        graph.addListener("cellsMoved", this::onCellsMoved);
        graph.addListener("cellsConnected", this::onCellsConnected);
        graph.addListener("labelChanged", this::onLabelChanged);

        // Defer SwingNode content assignment to AWT event thread via Platform.runLater
        // so JavaFX layout is complete before Swing component is attached.
        Platform.runLater(() -> {
            SwingUtilities.invokeLater(() -> {
                graphComponent.setVisible(true);
                graphComponent.revalidate();
                graphComponent.repaint();
                swingNode.setContent(graphComponent);
                // Sync data after content is set
                Platform.runLater(() -> { if (mindMapManager != null) syncAll(); });
            });
        });
    }

    /** Force a full repaint (call when the view becomes visible, e.g. tab switch). */
    public void forceRefresh() {
        if (graphComponent != null) {
            graphComponent.refresh();
            graphComponent.repaint();
            if (graph != null) graph.refresh();
        }
    }

    // ============================================================
    //  Public API
    // ============================================================

    public void setMindMapManager(MindMapManager manager) {
        this.mindMapManager = manager;
        if (manager == null) return;
        manager.getNodes().addListener((ListChangeListener<MindMapNode>) _ -> Platform.runLater(this::syncNodes));
        manager.getConnections().addListener((ListChangeListener<MindMapConnection>) _ -> Platform.runLater(this::syncEdges));
        // 防抖：连续的数据变更只触发一次 syncAll
        manager.setOnDataChangedListener(() -> {
            if (!syncQueued) {
                syncQueued = true;
                Platform.runLater(() -> {
                    syncQueued = false;
                    syncAll();
                });
            }
        });
    }

    public void setCurrentProject(Integer projectId) {
        if (mindMapManager != null && projectId != null)
            mindMapManager.loadProjectMindMap(projectId);
    }

    // ============================================================
    //  Graph Sync
    // ============================================================

    private void syncAll() {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            nodeCellMap.clear();
            edgeCellMap.clear();
            graph.getModel().beginUpdate();
            try {
                graph.removeCells(graph.getChildVertices(graph.getDefaultParent()));
                for (MindMapNode node : mindMapManager.getNodes()) {
                    String text = node.getText() != null ? node.getText() : "节点";
                    double w = node.getWidth() > 0 ? node.getWidth() : Math.min(160, Math.max(80, text.length() * 8));
                    double h = node.getHeight() > 0 ? node.getHeight() : 36;
                    mxCell cell = (mxCell) graph.insertVertex(graph.getDefaultParent(),
                        String.valueOf(node.getId()), text, node.getX(), node.getY(), w, h,
                        styleMapToString(buildStyleFor(node)));
                    nodeCellMap.put(node.getId(), cell);
                }
                for (MindMapConnection conn : mindMapManager.getConnections()) {
                    mxCell src = nodeCellMap.get(conn.getSourceNodeId());
                    mxCell tgt = nodeCellMap.get(conn.getTargetNodeId());
                    if (src == null || tgt == null) continue;
                    Object edgeObj = graph.insertEdge(graph.getDefaultParent(), String.valueOf(conn.getId()),
                        conn.getLabel() != null ? conn.getLabel() : "", src, tgt,
                        styleMapToString(buildEdgeStyleFor(conn)));
                    if (edgeObj instanceof mxCell me) edgeCellMap.put(conn.getId(), me);
                }
            } finally { graph.getModel().endUpdate(); }
        } finally { isInternalUpdate = false; }
    }

    private void syncNodes() {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            Set<Integer> active = new HashSet<>();
            mindMapManager.getNodes().forEach(n -> active.add(n.getId()));
            nodeCellMap.keySet().removeIf(id -> {
                if (!active.contains(id)) {
                    mxCell c = nodeCellMap.remove(id);
                    if (c != null) graph.removeCells(new Object[]{c});
                    return true;
                }
                return false;
            });
            graph.getModel().beginUpdate();
            try {
                for (MindMapNode node : mindMapManager.getNodes()) {
                    mxCell existing = nodeCellMap.get(node.getId());
                    if (existing != null) {
                        // Update style of existing cell in case model properties changed
                        existing.setStyle(styleMapToString(buildStyleFor(node)));
                        if (existing.getGeometry() != null) {
                            existing.getGeometry().setWidth(node.getWidth() > 0 ? node.getWidth() : 120);
                            existing.getGeometry().setHeight(node.getHeight() > 0 ? node.getHeight() : 40);
                        }
                        continue;
                    }
                    String text = node.getText() != null ? node.getText() : "节点";
                    double w = node.getWidth() > 0 ? node.getWidth() : Math.min(160, Math.max(80, text.length() * 8));
                    double h = node.getHeight() > 0 ? node.getHeight() : 36;
                    mxCell cell = (mxCell) graph.insertVertex(graph.getDefaultParent(),
                        String.valueOf(node.getId()), text, node.getX(), node.getY(), w, h,
                        styleMapToString(buildStyleFor(node)));
                    nodeCellMap.put(node.getId(), cell);
                }
            } finally { graph.getModel().endUpdate(); }
        } finally { isInternalUpdate = false; }
    }

    private void syncEdges() {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            Set<Integer> active = new HashSet<>();
            mindMapManager.getConnections().forEach(c -> active.add(c.getId()));
            edgeCellMap.keySet().removeIf(id -> {
                if (!active.contains(id)) {
                    mxCell c = edgeCellMap.remove(id);
                    if (c != null) graph.removeCells(new Object[]{c});
                    return true;
                }
                return false;
            });
            graph.getModel().beginUpdate();
            try {
                for (MindMapConnection conn : mindMapManager.getConnections()) {
                    mxCell existing = edgeCellMap.get(conn.getId());
                    if (existing != null) {
                        existing.setStyle(styleMapToString(buildEdgeStyleFor(conn)));
                        continue;
                    }
                    mxCell src = nodeCellMap.get(conn.getSourceNodeId());
                    mxCell tgt = nodeCellMap.get(conn.getTargetNodeId());
                    if (src == null || tgt == null) continue;
                    Object edgeObj = graph.insertEdge(graph.getDefaultParent(), String.valueOf(conn.getId()),
                        conn.getLabel() != null ? conn.getLabel() : "", src, tgt,
                        styleMapToString(buildEdgeStyleFor(conn)));
                    if (edgeObj instanceof mxCell me) edgeCellMap.put(conn.getId(), me);
                }
            } finally { graph.getModel().endUpdate(); }
        } finally { isInternalUpdate = false; }
    }

    // ============================================================
    //  Layout
    // ============================================================

    public void applyAutoLayout() {
        if (!initialized || nodeCellMap.isEmpty()) return;
        mxCompactTreeLayout layout = new mxCompactTreeLayout(graph, true);
        layout.setUseBoundingBox(false);
        layout.setEdgeRouting(true);
        layout.setLevelDistance(160);
        layout.setNodeDistance(60);
        layout.setResizeParent(false);
        graph.getModel().beginUpdate();
        try { layout.execute(graph.getDefaultParent()); }
        finally { graph.getModel().endUpdate(); }
        // Sync positions back to model
        graph.getModel().beginUpdate();
        try {
            for (Map.Entry<Integer, mxCell> e : nodeCellMap.entrySet()) {
                if (e.getValue().getGeometry() != null)
                    mindMapManager.updateNodePosition(e.getKey(),
                        e.getValue().getGeometry().getX(), e.getValue().getGeometry().getY());
            }
        } finally { graph.getModel().endUpdate(); }
        centerView();
    }

    // ============================================================
    //  Event Handlers
    // ============================================================

    private void onCellsMoved(Object sender, mxEventObject evt) {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        Object[] cells = (Object[]) evt.getProperty("cells");
        if (cells == null) return;
        for (Object c : cells) {
            if (c instanceof mxCell mxc && mxc.getGeometry() != null) {
                int id = parseIntId(mxc.getId());
                // nodeCellMap 是 ConcurrentHashMap，此处并发安全
                if (id > 0 && nodeCellMap.containsKey(id))
                    mindMapManager.updateNodePosition(id, mxc.getGeometry().getX(), mxc.getGeometry().getY());
            }
        }
    }

    private void onCellsConnected(Object sender, mxEventObject evt) {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        mxCell edge = (mxCell) evt.getProperty("edge");
        mxCell src = (mxCell) evt.getProperty("source");
        mxCell tgt = (mxCell) evt.getProperty("target");
        if (edge == null || src == null || tgt == null) return;
        int si = parseIntId(src.getId());
        int ti = parseIntId(tgt.getId());
        if (si > 0 && ti > 0)
            mindMapManager.createConnection(si, ti, MindMapConnection.ConnectionType.RELATED);
    }

    private void onLabelChanged(Object sender, mxEventObject evt) {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        mxCell cell = (mxCell) evt.getProperty("cell");
        String val = (String) evt.getProperty("value");
        if (cell == null || val == null) return;
        int id = parseIntId(cell.getId());
        if (id > 0) {
            isInternalUpdate = true;
            try { mindMapManager.updateNodeText(id, val); }
            finally { isInternalUpdate = false; }
        }
    }

    // ============================================================
    //  View Controls
    // ============================================================

    public void zoomIn() { if (graphComponent != null) { graphComponent.zoomIn(); forceRefresh(); } }
    public void zoomOut() { if (graphComponent != null) { graphComponent.zoomOut(); forceRefresh(); } }
    public void centerView() {
        if (graphComponent != null) {
            var bounds = graph.getView().getGraphBounds();
            if (bounds != null) graphComponent.getGraphControl().scrollRectToVisible(bounds.getRectangle());
            forceRefresh();
        }
    }

    /**
     * 根据 MindMapNode 模型属性动态构建 JGraphX 样式 Map。
     * Freeplane 风格：每个节点的颜色/形状/字体均来自模型而非硬编码。
     */
    private Map<String, Object> buildStyleFor(MindMapNode node) {
        Map<String, Object> style = new HashMap<>(graph.getStylesheet().getDefaultVertexStyle());

        // Shape mapping from model
        MindMapNode.NodeShape shape = node.getShape() != null ? node.getShape() : MindMapNode.NodeShape.ROUNDED_RECT;
        switch (shape) {
            case RECTANGLE -> {
                style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
                style.put(mxConstants.STYLE_ROUNDED, false);
            }
            case CIRCLE, ELLIPSE -> {
                style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
                style.put(mxConstants.STYLE_ROUNDED, false);
            }
            case ROUNDED_RECT -> {
                style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
                style.put(mxConstants.STYLE_ROUNDED, true);
                style.put(mxConstants.STYLE_ARCSIZE, 16);
            }
        }

        // Fill color from model
        String fillColor = node.getColor() != null ? node.getColor() : "#C4843C";
        style.put(mxConstants.STYLE_FILLCOLOR, fillColor);
        style.put(mxConstants.STYLE_GRADIENTCOLOR, deriveLighterColor(fillColor));

        // Root node gets a thicker border
        if (node.isRoot()) {
            style.put(mxConstants.STYLE_STROKECOLOR, "#8B5E2A");
            style.put(mxConstants.STYLE_STROKEWIDTH, 3);
        } else {
            style.put(mxConstants.STYLE_STROKECOLOR, "#D0C8BC");
            style.put(mxConstants.STYLE_STROKEWIDTH, 1);
        }

        // Font size
        style.put(mxConstants.STYLE_FONTSIZE, node.getFontSize() != null ? node.getFontSize() : 13);

        // Font style (bold + italic combined)
        int fontStyle = 0;
        if (node.getFontWeight() == MindMapNode.FontWeight.BOLD) {
            fontStyle |= mxConstants.FONT_BOLD;
        }
        if (node.getFontStyle() != null && node.getFontStyle().toLowerCase().contains("italic")) {
            fontStyle |= mxConstants.FONT_ITALIC;
        }
        style.put(mxConstants.STYLE_FONTSTYLE, fontStyle);

        return style;
    }

    /**
     * 根据 MindMapConnection 模型属性动态构建 JGraphX 边样式 Map。
     * Freeplane 风格：边颜色/线型/箭头按 ConnectionType 区分。
     */
    private Map<String, Object> buildEdgeStyleFor(MindMapConnection conn) {
        Map<String, Object> style = new HashMap<>(graph.getStylesheet().getDefaultEdgeStyle());

        // Stroke color from model
        String color = conn.getColor() != null ? conn.getColor() : "#C4B8A8";
        style.put(mxConstants.STYLE_STROKECOLOR, color);

        // Stroke width: base from model, scaled by strength
        int baseWidth = conn.getWidth() != null ? conn.getWidth() : 2;
        double sf = Math.max(0.0, Math.min(1.0, conn.getStrength()));
        style.put(mxConstants.STYLE_STROKEWIDTH, Math.max(1, (int) Math.round(baseWidth + sf * 2)));

        // Dash pattern from ConnectionStyle
        if (conn.getStyle() != null) {
            switch (conn.getStyle()) {
                case DASHED -> style.put(mxConstants.STYLE_DASHED, true);
                case DOTTED -> {
                    style.put(mxConstants.STYLE_DASHED, true);
                    style.put(mxConstants.STYLE_DASH_PATTERN, "1 4");
                }
                default -> style.put(mxConstants.STYLE_DASHED, false);
            }
        }

        // Arrow/color variations by ConnectionType
        if (conn.getConnectionType() != null) {
            switch (conn.getConnectionType()) {
                case DEPENDS_ON -> {
                    style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
                    style.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_DIAMOND);
                }
                case CONTRADICTS -> {
                    style.put(mxConstants.STYLE_STROKECOLOR, "#D45555");
                    style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
                    style.put(mxConstants.STYLE_DASHED, false);
                }
                case USES -> {
                    style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_BLOCK);
                    style.put(mxConstants.STYLE_DASHED, false);
                }
                case EXTENDS -> {
                    style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
                    style.put(mxConstants.STYLE_DASHED, true);
                }
                case CAUSAL -> {
                    style.put(mxConstants.STYLE_STROKECOLOR, "#7B68B8");
                    style.put(mxConstants.STYLE_STROKEWIDTH, 2);
                    style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_BLOCK);
                }
                case ANALOGY -> {
                    style.put(mxConstants.STYLE_DASHED, false);
                    style.put(mxConstants.STYLE_STROKECOLOR, "#C4A84C");
                    style.put(mxConstants.STYLE_STROKEWIDTH, 1);
                }
                default -> {} // RELATED keeps default
            }
        }

        // Edge label colors
        if (conn.getLabel() != null && !conn.getLabel().isEmpty()) {
            style.put(mxConstants.STYLE_FONTCOLOR, color);
            style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#F7F4F0");
        }

        return style;
    }

    /** Lighten a hex color by blending with white (40% mix). */
    private String deriveLighterColor(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() < 7) return "#FFFFFF";
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = Math.min(255, r + (int)((255 - r) * 0.4));
            g = Math.min(255, g + (int)((255 - g) * 0.4));
            b = Math.min(255, b + (int)((255 - b) * 0.4));
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            return "#FFFFFF";
        }
    }

    /** Convert a style Map to JGraphX style string "key=value;key=value". */
    private String styleMapToString(Map<String, Object> style) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : style.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private int parseIntId(String id) {
        if (id == null || id.isEmpty()) return -1;
        try { return Integer.parseInt(id); }
        catch (NumberFormatException e) { return -1; }
    }
}
