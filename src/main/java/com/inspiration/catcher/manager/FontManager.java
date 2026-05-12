package com.inspiration.catcher.manager;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    public enum FontSize {
        SMALL(12), MEDIUM(14), LARGE(16), XLARGE(18), XXLARGE(20);
        public final int px;
        FontSize(int px) { this.px = px; }
    }

    private FontSize currentSize = FontSize.MEDIUM;
    private Scene targetScene;
    private final UIConfigManager configManager;

    public FontManager(UIConfigManager configManager) {
        this.configManager = configManager;
        String saved = configManager.getConfig("font.size", "MEDIUM");
        try { currentSize = FontSize.valueOf(saved); }
        catch (IllegalArgumentException e) { currentSize = FontSize.MEDIUM; }
    }

    public void setTargetScene(Scene scene) { this.targetScene = scene; applyFontSize(); }

    public void increaseFontSize() {
        FontSize[] values = FontSize.values();
        int next = (currentSize.ordinal() + 1) % values.length;
        currentSize = values[next];
        applyFontSize(); save();
    }

    public void decreaseFontSize() {
        FontSize[] values = FontSize.values();
        int prev = (currentSize.ordinal() - 1 + values.length) % values.length;
        currentSize = values[prev];
        applyFontSize(); save();
    }

    public void resetFontSize() {
        currentSize = FontSize.MEDIUM;
        applyFontSize(); save();
    }

    public void showFontDialog() {
        ChoiceDialog<FontSize> dialog = new ChoiceDialog<>(currentSize, FontSize.values());
        dialog.setTitle("字体设置");
        dialog.setHeaderText("选择全局字体大小");
        dialog.setContentText("字体大小:");
        Optional<FontSize> result = dialog.showAndWait();
        result.ifPresent(size -> { currentSize = size; applyFontSize(); save(); });
    }

    /** Called by MainController for compatibility; applies via root CSS */
    public void updateAllFonts(Object controller) { applyFontSize(); }

    /** Set -fx-font-size on root so all controls inherit it */
    private void applyFontSize() {
        if (targetScene != null && targetScene.getRoot() != null) {
            targetScene.getRoot().setStyle("-fx-font-size: " + currentSize.px + "px;");
            logger.debug("Applied root font size: {}px", currentSize.px);
        }
    }

    private void save() {
        configManager.setConfig("font.size", currentSize.name());
        configManager.saveConfig();
    }
}
