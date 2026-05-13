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
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JGraphX mind map view with SwingNode embedding.
 * Optimized for: smooth rendering, no black screen, beautiful horizontal tree layout.
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
    private volatile boolean initialized = false;
    private volatile boolean needsLayout = true;

    public JGraphXMindMapView() {
        setStyle("-fx-background-color: #f8f9fa;");
        setSpacing(0);
        buildToolbar();
        VBox.setVgrow(swingNode, Priority.ALWAYS);
        getChildren().add(swingNode);
        // Defer heavy init until visible
        visibleProperty().addListener((_, _, v) -> { if (v && !initialized) initGraph(); });
        sceneProperty().addListener((_, _, scene) -> {
            if (scene != null && !initialized) Platform.runLater(this::ensureInitialized);
        });
        // Force repaint on resize to prevent black screen
        widthProperty().addListener(_ -> Platform.runLater(this::forceRepaint));
        heightProperty().addListener(_ -> Platform.runLater(this::forceRepaint));
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
        toolbarBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5 10; -fx-alignment: center-left;");
        toolbarBox.getChildren().addAll(zoomInBtn, zoomOutBtn, centerBtn,
            new Separator(), autoLayoutBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbarBox.getChildren().addAll(spacer, hint);
        getChildren().add(0, toolbarBox);
    }

    // ============================================================
    //  Lazy Graph Initialization (deferred until visible)
    // ============================================================

    private void initGraph() {
        if (initialized) return;
        initialized = true;
        logger.info("Initializing JGraphX engine (deferred)");

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

        // Vertex styles
        Map<String, Object> vs = graph.getStylesheet().getDefaultVertexStyle();
        vs.put(mxConstants.STYLE_SHAPE, "rect");
        vs.put("rounded", true);
        vs.put("arcSize", 14);
        vs.put(mxConstants.STYLE_FILLCOLOR, "#4A90E2");
        vs.put(mxConstants.STYLE_FONTCOLOR, "#FFFFFF");
        vs.put(mxConstants.STYLE_FONTSIZE, 13);
        vs.put(mxConstants.STYLE_FONTSTYLE, 0);
        vs.put(mxConstants.STYLE_ALIGN, "center");
        vs.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
        vs.put(mxConstants.STYLE_WHITE_SPACE, "wrap");

        // Edge styles
        Map<String, Object> es = graph.getStylesheet().getDefaultEdgeStyle();
        es.put(mxConstants.STYLE_STROKECOLOR, "#999999");
        es.put(mxConstants.STYLE_STROKEWIDTH, 2);
        es.put("rounded", true);
        es.put(mxConstants.STYLE_EDGE, "entityRelationEdgeStyle");
        es.put("curved", true);
        es.put(mxConstants.STYLE_ENDARROW, "classic");
        es.put("endSize", 6);
        es.put(mxConstants.STYLE_FONTSIZE, 11);
        es.put(mxConstants.STYLE_FONTCOLOR, "#888888");
        es.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        es.put(mxConstants.STYLE_EXIT_X, 1.0);
        es.put(mxConstants.STYLE_EXIT_Y, 0.5);
        es.put(mxConstants.STYLE_ENTRY_X, 0.0);
        es.put(mxConstants.STYLE_ENTRY_Y, 0.5);

        // Node type styles
        addNodeStyle("root", "#4A90E2", 16, 1);
        addNodeStyle("idea", "#FF6B6B", 14, 1);
        addNodeStyle("concept", "#36B37E", 13, 0);
        addNodeStyle("external", "#FFD166", 13, 2);

        // Build Swing component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(new Color(0xf8, 0xf9, 0xfa));
        graphComponent.setBackground(new Color(0xf8, 0xf9, 0xfa));
        graphComponent.setWheelScrollingEnabled(false);

        // Events
        graph.addListener("cellsMoved", this::onCellsMoved);
        graph.addListener("cellsConnected", this::onCellsConnected);
        graph.addListener("labelChanged", this::onLabelChanged);

        SwingUtilities.invokeLater(() -> {
            graphComponent.setVisible(true);
            graphComponent.revalidate();
            graphComponent.repaint();
            swingNode.setContent(graphComponent);
            // Sync existing data after engine is ready
            Platform.runLater(() -> {
                if (mindMapManager != null) {
                    syncAll();
                    if (needsLayout && !nodeCellMap.isEmpty()) {
                        applyAutoLayout();
                        needsLayout = false;
                    }
                }
            });
        });
    }

    private void addNodeStyle(String name, String fillColor, int fontSize, int fontStyle) {
        Map<String, Object> style = new HashMap<>(graph.getStylesheet().getDefaultVertexStyle());
        style.put(mxConstants.STYLE_FILLCOLOR, fillColor);
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
        manager.getNodes().addListener((ListChangeListener<MindMapNode>) _ ->
            Platform.runLater(this::syncNodes));
        manager.getConnections().addListener((ListChangeListener<MindMapConnection>) _ ->
            Platform.runLater(this::syncEdges));
        manager.setOnDataChangedListener(() -> Platform.runLater(this::syncAll));
    }

    public void setCurrentProject(Integer projectId) {
        if (mindMapManager != null && projectId != null)
            mindMapManager.loadProjectMindMap(projectId);
    }

    /** Trigger deferred initialization if not yet started. Called after adding to scene. */
    public void ensureInitialized() {
        if (!initialized) initGraph();
    }

    public void redraw() {
        if (graphComponent != null) {
            graphComponent.refresh();
            graphComponent.repaint();
        }
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
                Object[] cells = graph.getChildVertices(graph.getDefaultParent());
                if (cells != null) graph.removeCells(cells);
            } finally {
                graph.getModel().endUpdate();
            }
            syncNodes();
            syncEdges();
        } finally {
            isInternalUpdate = false;
        }
    }

    private void syncNodes() {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            Set<Integer> activeIds = new HashSet<>();
            mindMapManager.getNodes().forEach(n -> activeIds.add(n.getId()));

            // Remove stale
            nodeCellMap.keySet().removeIf(id -> {
                if (!activeIds.contains(id)) {
                    mxCell c = nodeCellMap.remove(id);
                    if (c != null) graph.removeCells(new Object[]{c});
                    return true;
                }
                return false;
            });

            // Add new
            graph.getModel().beginUpdate();
            try {
                for (MindMapNode node : mindMapManager.getNodes()) {
                    if (nodeCellMap.containsKey(node.getId())) continue;
                    String text = node.getText() != null ? node.getText() : "节点";
                    double w = Math.min(160, Math.max(80, text.length() * 8));
                    mxCell cell = (mxCell) graph.insertVertex(
                        graph.getDefaultParent(), String.valueOf(node.getId()),
                        text, node.getX(), node.getY(), w, 36, styleFor(node));
                    nodeCellMap.put(node.getId(), cell);
                }
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            isInternalUpdate = false;
        }
    }

    private void syncEdges() {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        isInternalUpdate = true;
        try {
            Set<Integer> activeIds = new HashSet<>();
            mindMapManager.getConnections().forEach(c -> activeIds.add(c.getId()));

            edgeCellMap.keySet().removeIf(id -> {
                if (!activeIds.contains(id)) {
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
                    if (src == null || tgt == null) {
                        logger.debug("Skipping edge: missing endpoint node");
                        continue;
                    }
                    mxCell edge = (mxCell) graph.insertEdge(
                        graph.getDefaultParent(), String.valueOf(conn.getId()),
                        conn.getLabel() != null ? conn.getLabel() : "",
                        src, tgt, "");
                    edgeCellMap.put(conn.getId(), edge);
                }
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            isInternalUpdate = false;
        }
    }

    // ============================================================
    //  Layout — horizontal tree optimized for mind maps
    // ============================================================

    public void applyAutoLayout() {
        if (!initialized || graph == null || nodeCellMap.isEmpty()) return;
        // Find root (prefer the root node, otherwise first node)
        Object root = null;
        for (Map.Entry<Integer, mxCell> e : nodeCellMap.entrySet()) {
            if (e.getValue() != null) {
                root = e.getValue();
                break;
            }
        }
        if (root == null) return;

        mxCompactTreeLayout layout = new mxCompactTreeLayout(graph, true);
        layout.setUseBoundingBox(false);
        layout.setEdgeRouting(true);
        layout.setLevelDistance(120);
        layout.setNodeDistance(40);
        layout.setResizeParent(false);

        graph.getModel().beginUpdate();
        try {
            layout.execute(graph.getDefaultParent());
        } finally {
            graph.getModel().endUpdate();
        }
        // Sync JGraphX positions back to MindMapManager
        graph.getModel().beginUpdate();
        try {
            for (Map.Entry<Integer, mxCell> e : nodeCellMap.entrySet()) {
                mxCell c = e.getValue();
                if (c != null && c.getGeometry() != null) {
                    mindMapManager.updateNodePosition(e.getKey(),
                        c.getGeometry().getX(), c.getGeometry().getY());
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
        centerView();
    }

    // ============================================================
    //  Event Handlers
    // ============================================================

    private void onCellsMoved(Object sender, mxEventObject evt) {
        if (!initialized || isInternalUpdate || mindMapManager == null) return;
        Object[] cells = (Object[]) evt.getProperty("cells");
        if (cells == null) return;
        graph.getModel().beginUpdate();
        try {
            for (Object c : cells) {
                if (c instanceof mxCell mxc && mxc.getGeometry() != null) {
                    int id = parseIntId(mxc.getId());
                    if (id > 0) {
                        mindMapManager.updateNodePosition(id,
                            mxc.getGeometry().getX(), mxc.getGeometry().getY());
                    }
                }
            }
        } finally {
            graph.getModel().endUpdate();
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
        if (si > 0 && ti > 0 && mindMapManager != null) {
            mindMapManager.createConnection(si, ti, MindMapConnection.ConnectionType.RELATED);
        }
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

    public void zoomIn() {
        if (graphComponent != null) {
            graphComponent.zoomIn();
            forceRepaint();
        }
    }

    public void zoomOut() {
        if (graphComponent != null) {
            graphComponent.zoomOut();
            forceRepaint();
        }
    }

    public void centerView() {
        if (graphComponent != null) {
            var bounds = graphComponent.getGraph().getView().getGraphBounds();
            if (bounds != null) {
                var rect = bounds.getRectangle();
                graphComponent.getGraphControl().scrollRectToVisible(rect);
            }
            forceRepaint();
        }
    }

    /** Force repaint to prevent SwingNode black screen. */
    private void forceRepaint() {
        if (graphComponent != null) {
            graphComponent.refresh();
            graphComponent.repaint();
        }
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private String styleFor(MindMapNode node) {
        if (node.isRoot()) return "root";
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
