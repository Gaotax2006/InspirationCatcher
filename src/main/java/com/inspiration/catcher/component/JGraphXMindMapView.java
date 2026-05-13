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

        addNodeStyle("root", "#C4843C", "#E8B86A", 16, mxConstants.FONT_BOLD);
        addNodeStyle("idea", "#E86868", "#F09090", 14, mxConstants.FONT_BOLD);
        addNodeStyle("concept", "#4A9E6B", "#70C08A", 13, 0);
        addNodeStyle("external", "#D4A84C", "#E8C870", 13, mxConstants.FONT_ITALIC);

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

    private void addNodeStyle(String name, String fillColor, String gradColor, int fontSize, int fontStyle) {
        Map<String, Object> style = new HashMap<>(graph.getStylesheet().getDefaultVertexStyle());
        style.put(mxConstants.STYLE_FILLCOLOR, fillColor);
        style.put(mxConstants.STYLE_GRADIENTCOLOR, gradColor);
        style.put(mxConstants.STYLE_FONTSIZE, fontSize);
        style.put(mxConstants.STYLE_FONTSTYLE, fontStyle);
        graph.getStylesheet().putCellStyle(name, style);
    }

    // ============================================================
    //  Public API
    // ============================================================

    public void setMindMapManager(MindMapManager manager) {
        this.mindMapManager = manager;
        if (manager == null) return;
        manager.getNodes().addListener((ListChangeListener<MindMapNode>) _ -> Platform.runLater(this::syncNodes));
        manager.getConnections().addListener((ListChangeListener<MindMapConnection>) _ -> Platform.runLater(this::syncEdges));
        manager.setOnDataChangedListener(() -> Platform.runLater(this::syncAll));
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
                    double w = Math.min(160, Math.max(80, text.length() * 8));
                    mxCell cell = (mxCell) graph.insertVertex(graph.getDefaultParent(),
                        String.valueOf(node.getId()), text, node.getX(), node.getY(), w, 36, styleFor(node));
                    nodeCellMap.put(node.getId(), cell);
                }
                for (MindMapConnection conn : mindMapManager.getConnections()) {
                    mxCell src = nodeCellMap.get(conn.getSourceNodeId());
                    mxCell tgt = nodeCellMap.get(conn.getTargetNodeId());
                    if (src == null || tgt == null) continue;
                    Object edgeObj = graph.insertEdge(graph.getDefaultParent(), String.valueOf(conn.getId()),
                        conn.getLabel() != null ? conn.getLabel() : "", src, tgt, "");
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
                    if (nodeCellMap.containsKey(node.getId())) continue;
                    String text = node.getText() != null ? node.getText() : "节点";
                    double w = Math.min(160, Math.max(80, text.length() * 8));
                    mxCell cell = (mxCell) graph.insertVertex(graph.getDefaultParent(),
                        String.valueOf(node.getId()), text, node.getX(), node.getY(), w, 36, styleFor(node));
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
                    if (edgeCellMap.containsKey(conn.getId())) continue;
                    mxCell src = nodeCellMap.get(conn.getSourceNodeId());
                    mxCell tgt = nodeCellMap.get(conn.getTargetNodeId());
                    if (src == null || tgt == null) continue;
                    Object edgeObj = graph.insertEdge(graph.getDefaultParent(), String.valueOf(conn.getId()),
                        conn.getLabel() != null ? conn.getLabel() : "", src, tgt, "");
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
                if (id > 0) mindMapManager.updateNodePosition(id, mxc.getGeometry().getX(), mxc.getGeometry().getY());
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

    private String styleFor(MindMapNode node) {
        if (node.isRoot()) return "root";
        return switch (node.getNodeType()) {
            case IDEA -> "idea"; case CONCEPT -> "concept"; case EXTERNAL -> "external";
        };
    }

    private int parseIntId(String id) {
        if (id == null || id.isEmpty()) return -1;
        try { return Integer.parseInt(id); }
        catch (NumberFormatException e) { return -1; }
    }
}
