package com.inspiration.catcher.manager;

import com.inspiration.catcher.controller.MainController;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    private final UIConfigManager configManager;
    private final MainController mainController;
    private FontSize currentFontSize;

    public enum FontSize {
        SMALL(12), MEDIUM(14), LARGE(16), XLARGE(18);
        private final int size;
        FontSize(int size) {
            this.size = size;
        }
        public int getSize() { return size; }
    }

    public FontManager(UIConfigManager configManager, MainController mainController) {
        this.configManager = configManager;
        this.mainController = mainController;
        // 从配置加载字体设置
        String fontSetting = configManager.getConfig("font.size", "MEDIUM");
        try {
            currentFontSize = FontSize.valueOf(fontSetting);
        } catch (IllegalArgumentException e) {
            currentFontSize = FontSize.MEDIUM;
        }
    }
    // 添加获取当前字体大小的方法
    public int getCurrentFontSize() {
        return currentFontSize.getSize();
    }

    public void updateAllFonts(MainController mainController) {
        String fontFamily = "Microsoft YaHei, Arial, sans-serif";
        int fontSize = currentFontSize.getSize();
        // 更新主控制器中的控件字体
        updateControllerFonts(mainController, fontFamily, fontSize);
        saveFontSize();
    }
    // 添加更新具体控件的方法
    private void updateControllerFonts(MainController controller, String fontFamily, int fontSize) {
        // 更新文本区域
        if (controller.ideaDetail != null) {
            controller.ideaDetail.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    fontFamily, fontSize));
        }
        if (controller.aiSuggestions != null) {
            controller.aiSuggestions.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    fontFamily, fontSize));
        }
        if (controller.markdownEditor != null) {
            controller.markdownEditor.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    fontFamily, fontSize));
        }
        // 更新表格
        if (controller.ideaTableView != null) {
            controller.ideaTableView.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    fontFamily, fontSize));
        }
        // 更新状态标签
        if (controller.statusLabel != null) {
            controller.statusLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    fontFamily, fontSize));
        }
    }

    public void increaseFontSize() {
        FontSize[] sizes = FontSize.values();
        int currentIndex = currentFontSize.ordinal();
        if (currentIndex < sizes.length - 1) {
            currentFontSize = sizes[currentIndex + 1];
            updateAllFonts(mainController);
        }
    }

    public void decreaseFontSize() {
        FontSize[] sizes = FontSize.values();
        int currentIndex = currentFontSize.ordinal();
        if (currentIndex > 0) {
            currentFontSize = sizes[currentIndex - 1];
            updateAllFonts(mainController);
        }
    }

    public void resetFontSize() {
        currentFontSize = FontSize.MEDIUM;
        updateAllFonts(mainController);
        showAlert("字体已重置为默认大小");
    }

    public void showFontDialog() {
        ChoiceDialog<FontSize> dialog = new ChoiceDialog<>(currentFontSize, FontSize.values());
        dialog.setTitle("字体设置");
        dialog.setHeaderText("选择字体大小");
        dialog.setContentText("请选择字体大小:");

        Optional<FontSize> result = dialog.showAndWait();
        result.ifPresent(size -> {
            currentFontSize = size;
            updateAllFonts(mainController);
        });
    }

    private void saveFontSize() {
        configManager.setConfig("font.size", currentFontSize.name());
        configManager.saveConfig();
        logger.info("保存字体设置: {}", currentFontSize);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}