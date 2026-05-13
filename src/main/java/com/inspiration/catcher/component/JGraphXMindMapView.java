package com.inspiration.catcher.component;

import com.inspiration.catcher.manager.MindMapManager;
import com.inspiration.catcher.model.*;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
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
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JGraphX-powered mind map view via SwingNode embedding.
 * Provides auto layout (mxCompactTreeLayout), node drag, edge creation, styling.
 */
public class JGraphXMindMapView extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(JGraphXMindMapView.class);

    private final SwingNode swingNode = new SwingNode();
    private mxGraph graph;
    private mxGraphComponent graphComponent;
    private MindMapManager mindMapManager;

    private final Map<Integer, mxCell> nodeCellMap = new ConcurrentHashMap<>();
    private final Map<Integer, mxCell> edgeCellMap = new ConcurrentHashMap<>();
    private volatile boolean isInternalUpdate = false;

    public JGraphXMindMapView() {
        setStyle("-fx-background-color: #f8f9fa;");
        buildToolbar();
        VBox.setVgrow(swingNode, Priority.ALWAYS);
        getChildren().add(swingNode);
        initGraph();
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

        Label hint = new Label("拖拽移动 · 双击编辑 · 滚轮缩放 · 从左侧拖入灵感");
        hint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        ToolBar tb = new ToolBar(zoomInBtn, zoomOutBtn, centerBtn,
            new Separator(), autoLayoutBtn,
            new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
            hint);
        tb.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5;");
        getChildren().add(tb);
    }

    // ============================================================
    //  Graph Initialization
    // ============================================================

    private void initGraph() {
        graph = new mxGraph();
        graph.setCellsResizable(false);
        graph.setCellsMovable(true);
        graph.setAllowDanglingEdges(false);
        graph.setConnectableEdges(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        graph.setAutoSizeCells(true);
        graph.setMultigraph(false);
        graph.setAllowLoops(false);
        graph.setBorder(60);

        // Default vertex style
        Map<String, Object> vs = graph.getStylesheet().getDefaultVertexStyle();
        vs.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        vs.put("rounded", true);
        vs.put("arcSize", 12);
        vs.put(mxConstants.STYLE_FILLCOLOR, "#4A90E2");
        vs.put(mxConstants.STYLE_FONTCOLOR, "#FFFFFF");
        vs.put(mxConstants.STYLE_FONTSIZE, 13);
        vs.put(mxConstants.STYLE_ALIGN, "center");
        vs.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
        vs.put(mxConstants.STYLE_WHITE_SPACE, "wrap");
        vs.put(mxConstants.STYLE_SHADOW, true);

        // Default edge style
        Map<String, Object> es = graph.getStylesheet().getDefaultEdgeStyle();
        es.put(mxConstants.STYLE_STROKECOLOR, "#666666");
        es.put(mxConstants.STYLE_STROKEWIDTH, 2);
        es.put(mxConstants.STYLE_ROUNDED, true);
        es.put(mxConstants.STYLE_EDGE, "entityRelationEdgeStyle");
        es.put("curved", true);
        es.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        es.put("endSize", 8);
        es.put(mxConstants.STYLE_FONTSIZE, 11);
        es.put(mxConstants.STYLE_FONTCOLOR, "#666666");
        es.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");

        // Custom styles
        Map<String, Object> rootStyle = new HashMap<>(vs);
        rootStyle.put(mxConstants.STYLE_FONTSIZE, 16);
        rootStyle.put(mxConstants.STYLE_FONTSTYLE, 1); // BOLD
        rootStyle.put(mxConstants.STYLE_FILLCOLOR, "#4A90E2");
        graph.getStylesheet().putCellStyle("root", rootStyle);

        Map<String, Object> ideaStyle = new HashMap<>(vs);
        ideaStyle.put(mxConstants.STYLE_FILLCOLOR, "#FF6B6B");
        ideaStyle.put(mxConstants.STYLE_FONTSTYLE, 1);
        graph.getStylesheet().putCellStyle("idea", ideaStyle);

        Map<String, Object> conceptStyle = new HashMap<>(vs);
        conceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#36B37E");
        graph.getStylesheet().putCellStyle("concept", conceptStyle);

        Map<String, Object> externalStyle = new HashMap<>(vs);
        externalStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFD166");
        externalStyle.put(mxConstants.STYLE_FONTCOLOR, "#333333");
        externalStyle.put(mxConstants.STYLE_FONTSTYLE, 2); // ITALIC
        graph.getStylesheet().putCellStyle("external", externalStyle);

        // Build Swing component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.getViewport().setOpaque(false);
        graphComponent.setBackground(new Color(0xf8, 0xf9, 0xfa));
        // Note: JGraphX's AWT DragSource conflicts with SwingNode DnD bridge;
        // the InvalidDnDOperationException is caught by the global exception handler

        // Events (using string literals for cross-version compatibility)
        graph.addListener("cellsMoved", this::onCellsMoved);
        graph.addListener("cellsConnected", this::onCellsConnected);
        graph.addListener("labelChanged", this::onLabelChanged);

        SwingUtilities.invokeLater(() -> {
            graphComponent.zoomAndCenter();
            swingNode.setContent(graphComponent);
        });
    }

    // ============================================================
    //  Public API
    // ============================================================

    public void setMindMapManager(MindMapManager manager) {
        this.mindMapManager = manager;
        if (manager == null) return;
        manager.getNodes().addListener((ListChangeListener<MindMapNode>) _ ->
            Platform.runLater(this::syncNodes));
        manager.getConnections().addListener((ListChangeListener<MindMapConnection>) _ ->
            Platform.runLater(this::syncEdges));
        manager.setOnDataChangedListener(() -> Platform.runLater(this::syncAll));
    }

    public void setCurrentProject(Integer projectId) {
        clear();
        if (mindMapManager != null && projectId != null)
            mindMapManager.loadProjectMindMap(projectId);
    }

    public void redraw() {
        if (graphComponent != null) graphComponent.refresh();
    }

    // ============================================================
    //  Graph Sync
    // ============================================================

    private void syncAll() {
        if (isInternalUpdate || mindMapManager == null) return;
        clear();
        syncNodes();
        syncEdges();
    }

    private void clear() {
        nodeCellMap.clear();
        edgeCellMap.clear();
        graph.getModel().beginUpdate();
        try {
            Object[] cells = graph.getChildVertices(graph.getDefaultParent());
            if (cells != null) graph.removeCells(cells);
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void syncNodes() {
        if (isInternalUpdate || mindMapManager == null) return;
        graph.getModel().beginUpdate();
        try {
            // Remove stale
            Set<Integer> active = new HashSet<>();
            mindMapManager.getNodes().forEach(n -> active.add(n.getId()));
            nodeCellMap.keySet().removeIf(id -> {
                if (!active.contains(id)) {
                    mxCell c = nodeCellMap.get(id);
                    if (c != null) graph.removeCells(new Object[]{c});
                    return true;
                }
                return false;
            });

            for (MindMapNode node : mindMapManager.getNodes()) {
                if (nodeCellMap.containsKey(node.getId())) continue;
                String style = styleFor(node);
                String text = node.getText() != null ? node.getText() : "节点";
                double w = Math.max(80, Math.min(160, text.length() * 8));
                mxCell cell = (mxCell) graph.insertVertex(
                    graph.getDefaultParent(), String.valueOf(node.getId()),
                    text, node.getX(), node.getY(), w, 36, style);
                nodeCellMap.put(node.getId(), cell);
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void syncEdges() {
        if (isInternalUpdate || mindMapManager == null) return;
        graph.getModel().beginUpdate();
        try {
            Set<Integer> active = new HashSet<>();
            mindMapManager.getConnections().forEach(c -> active.add(c.getId()));
            edgeCellMap.keySet().removeIf(id -> {
                if (!active.contains(id)) {
                    mxCell c = edgeCellMap.get(id);
                    if (c != null) graph.removeCells(new Object[]{c});
                    return true;
                }
                return false;
            });

            for (MindMapConnection conn : mindMapManager.getConnections()) {
                if (edgeCellMap.containsKey(conn.getId())) continue;
                mxCell src = nodeCellMap.get(conn.getSourceNodeId());
                mxCell tgt = nodeCellMap.get(conn.getTargetNodeId());
                if (src == null || tgt == null) continue;
                mxCell edge = (mxCell) graph.insertEdge(
                    graph.getDefaultParent(), String.valueOf(conn.getId()),
                    conn.getLabel() != null ? conn.getLabel() : "",
                    src, tgt, "");
                edgeCellMap.put(conn.getId(), edge);
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    // ============================================================
    //  Layout
    // ============================================================

    public void applyAutoLayout() {
        if (graph == null || nodeCellMap.isEmpty()) return;
        mxCompactTreeLayout layout = new mxCompactTreeLayout(graph, false);
        layout.setUseBoundingBox(false);
        layout.setEdgeRouting(false);
        layout.setLevelDistance(80);
        layout.setNodeDistance(30);
        graph.getModel().beginUpdate();
        try {
            layout.execute(graph.getDefaultParent());
        } finally {
            graph.getModel().endUpdate();
        }
        centerView();
    }

    // ============================================================
    //  Event Handlers
    // ============================================================

    private void onCellsMoved(Object sender, mxEventObject evt) {
        if (isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            Object[] cells = (Object[]) evt.getProperty("cells");
            if (cells == null) return;
            for (Object c : cells) {
                if (c instanceof mxCell mxc) {
                    int id = parseIntId(mxc.getId());
                    if (id > 0 && mxc.getGeometry() != null) {
                        mindMapManager.updateNodePosition(id,
                            mxc.getGeometry().getX(), mxc.getGeometry().getY());
                    }
                }
            }
        } finally { isInternalUpdate = false; }
    }

    private void onCellsConnected(Object sender, mxEventObject evt) {
        if (isInternalUpdate || mindMapManager == null) return;
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
        if (isInternalUpdate || mindMapManager == null) return;
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

    public void zoomIn() { if (graphComponent != null) graphComponent.zoomIn(); }
    public void zoomOut() { if (graphComponent != null) graphComponent.zoomOut(); }
    public void centerView() { if (graphComponent != null) graphComponent.zoomAndCenter(); }
    public void resetView() { if (graphComponent != null) graphComponent.zoomAndCenter(); }

    // ============================================================
    //  Helpers
    // ============================================================

    private String styleFor(MindMapNode node) {
        return switch (node.getNodeType()) {
            case IDEA -> "idea";
            case CONCEPT -> "concept";
            case EXTERNAL -> "external";
        };
    }

    private int parseIntId(String id) {
        if (id == null || id.isEmpty()) return -1;
        try { return Integer.parseInt(id); }
        catch (NumberFormatException e) { return -1; }
    }
}
