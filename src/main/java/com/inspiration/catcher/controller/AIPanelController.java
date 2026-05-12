package com.inspiration.catcher.controller;

import com.inspiration.catcher.config.AIConfig;
import com.inspiration.catcher.manager.IdeaManager;
import com.inspiration.catcher.manager.StatusManager;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.service.AIService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Optional;
import java.util.Properties;

public class AIPanelController {
    private static final Logger logger = LoggerFactory.getLogger(AIPanelController.class);

    private final AIService aiService = new AIService();
    private boolean isAIGenerating = false;
    private Node aiButtonContainer = null;
    private final Object aiLock = new Object();

    private final TextArea aiSuggestions;
    private final TextArea markdownEditor;
    private final Label statusLabel;
    private final TableView<Idea> ideaTableView;
    private final TabPane mainTabPane;
    private final IdeaManager ideaManager;
    private final StatusManager statusManager;
    private final Runnable onSave;

    public AIPanelController(TextArea aiSuggestions, TextArea markdownEditor,
                              Label statusLabel, TableView<Idea> ideaTableView,
                              TabPane mainTabPane, IdeaManager ideaManager,
                              StatusManager statusManager, Runnable onSave) {
        this.aiSuggestions = aiSuggestions;
        this.markdownEditor = markdownEditor;
        this.statusLabel = statusLabel;
        this.ideaTableView = ideaTableView;
        this.mainTabPane = mainTabPane;
        this.ideaManager = ideaManager;
        this.statusManager = statusManager;
        this.onSave = onSave;
    }

    public void generateSuggestions() {
        synchronized (aiLock) {
            if (isAIGenerating) { statusManager.showAlert("AI正在思考中，请稍后再试"); return; }
            isAIGenerating = true;
        }
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            statusLabel.setText("🤖 AI生成中...");
            aiSuggestions.setText("");
            disableAIButtons();
            aiService.generateSuggestionsStreaming(selected,
                token -> Platform.runLater(() -> aiSuggestions.appendText(token)),
                () -> Platform.runLater(() -> {
                    statusLabel.setText("AI建议生成完成");
                    enableAIButtons();
                    addInsertButtons();
                    synchronized (aiLock) { isAIGenerating = false; }
                }),
                error -> Platform.runLater(() -> {
                    statusLabel.setText("AI生成失败: " + error.getMessage());
                    enableAIButtons();
                    synchronized (aiLock) { isAIGenerating = false; }
                })
            );
        } else {
            statusManager.showAlert("请先选择一个灵感");
            synchronized (aiLock) { isAIGenerating = false; }
        }
    }

    public void generateExpansion() {
        synchronized (aiLock) {
            if (isAIGenerating) { statusManager.showAlert("AI正在思考中，请稍后再试"); return; }
            isAIGenerating = true;
        }
        Idea selected = ideaTableView.getSelectionModel().getSelectedItem();
        if (selected != null) generateAndEditPrompt(selected);
        else {
            statusManager.showAlert("请先选择一个灵感");
            synchronized (aiLock) { isAIGenerating = false; }
        }
    }

    public void testConnection() {
        statusLabel.setText("测试AI连接...");
        new Thread(() -> Platform.runLater(() ->
                statusLabel.setText(aiService.testConnection() ? "✅ AI连接正常" : "❌ AI连接失败，请检查API密钥")
        )).start();
    }

    /** Multi-section settings dialog */
    public void showSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("设置");
        dialog.setHeaderText("灵感捕手 — 偏好设置");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: AI Settings
        tabs.getTabs().add(createAISettingsTab());
        // Tab 2: General
        tabs.getTabs().add(createGeneralSettingsTab());
        // Tab 3: About
        tabs.getTabs().add(createAboutTab());

        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().setPrefSize(520, 400);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private Tab createAISettingsTab() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        // API Key
        grid.add(new Label("API密钥:"), 0, 0);
        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setText(AIConfig.getApiKey());
        apiKeyField.setPrefWidth(300);
        grid.add(apiKeyField, 1, 0);

        // API URL
        grid.add(new Label("API地址:"), 0, 1);
        TextField urlField = new TextField(AIConfig.getApiUrl());
        urlField.setPrefWidth(300);
        grid.add(urlField, 1, 1);

        // Model
        grid.add(new Label("模型:"), 0, 2);
        ComboBox<String> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll("deepseek-chat", "deepseek-reasoner", "gpt-4o", "gpt-4o-mini");
        modelCombo.setValue(AIConfig.getModel());
        modelCombo.setEditable(true);
        grid.add(modelCombo, 1, 2);

        // Max Tokens
        grid.add(new Label("最大Token:"), 0, 3);
        Spinner<Integer> tokensSpinner = new Spinner<>(256, 8192, AIConfig.getMaxTokens(), 256);
        tokensSpinner.setEditable(true);
        grid.add(tokensSpinner, 1, 3);

        // Temperature
        grid.add(new Label("温度(T):"), 0, 4);
        Slider tempSlider = new Slider(0, 2, AIConfig.getTemperature());
        tempSlider.setShowTickLabels(true);
        tempSlider.setShowTickMarks(true);
        tempSlider.setMajorTickUnit(0.5);
        tempSlider.setBlockIncrement(0.1);
        Label tempLabel = new Label(String.format("%.1f", AIConfig.getTemperature()));
        tempSlider.valueProperty().addListener((_, _, v) -> tempLabel.setText(String.format("%.1f", v)));
        HBox tempBox = new HBox(10, tempSlider, tempLabel);
        HBox.setHgrow(tempSlider, Priority.ALWAYS);
        grid.add(tempBox, 1, 4);

        // Fallback toggle
        grid.add(new Label("离线备用:"), 0, 5);
        CheckBox fallbackCheck = new CheckBox("AI连接失败时使用离线模板");
        fallbackCheck.setSelected(AIConfig.isFallbackEnabled());
        grid.add(fallbackCheck, 1, 5);

        // Test & Save buttons
        HBox buttons = new HBox(10);
        Button testBtn = new Button("测试连接");
        Button saveBtn = new Button("保存");
        saveBtn.setDefaultButton(true);
        buttons.getChildren().addAll(testBtn, saveBtn);

        testBtn.setOnAction(_ -> {
            statusLabel.setText("测试连接...");
            new Thread(() -> {
                boolean ok = aiService.testConnection();
                Platform.runLater(() -> statusLabel.setText(ok ? "✅ 连接成功" : "❌ 连接失败"));
            }).start();
        });

        saveBtn.setOnAction(_ -> {
            Properties props = new Properties();
            props.setProperty("ai.api.key", apiKeyField.getText());
            props.setProperty("ai.api.url", urlField.getText());
            props.setProperty("ai.model", modelCombo.getValue());
            props.setProperty("ai.max_tokens", String.valueOf(tokensSpinner.getValue()));
            props.setProperty("ai.temperature", String.format("%.1f", tempSlider.getValue()));
            props.setProperty("ai.fallback.enabled", String.valueOf(fallbackCheck.isSelected()));
            saveProperties(props);
            statusLabel.setText("设置已保存");
        });

        content.getChildren().addAll(grid, buttons);
        Tab tab = new Tab("AI 设置");
        tab.setContent(content);
        return tab;
    }

    private Tab createGeneralSettingsTab() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.getChildren().add(new Label("通用设置（开发中）"));
        content.getChildren().add(new Label("• 默认字体大小可通过工具栏 A− / A / A+ 调节"));
        content.getChildren().add(new Label("• 默认项目在侧边栏顶部选择器切换"));
        Tab tab = new Tab("通用");
        tab.setContent(content);
        return tab;
    }

    private Tab createAboutTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(25));
        Label title = new Label("灵感捕手");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -fx-primary;");
        Label subtitle = new Label("Inspiration Catcher");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-secondary;");
        Separator sep1 = new Separator();
        Label version = new Label("版本 1.0.0");
        Label tech = new Label("技术栈: JavaFX 24 · AtlantaFX · SQLite · DeepSeek AI");
        Label author = new Label("作者: 高天翔");
        author.setStyle("-fx-font-size: 13px;");
        Separator sep2 = new Separator();
        Label desc = new Label("一款面向创作者和知识工作者的灵感管理工具，\n支持 Markdown 编辑、思维导图可视化和 AI 辅助扩展。");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-secondary;");
        Label stats = new Label("代码统计: 34 Java + 2 FXML + 1 CSS = ~9,300 行");
        stats.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-tertiary;");
        content.getChildren().addAll(title, subtitle, sep1, version, tech, author, sep2, desc, stats);
        Tab tab = new Tab("关于");
        tab.setContent(content);
        return tab;
    }

    private void saveProperties(Properties props) {
        File configFile = new File(System.getProperty("user.home") + "/.inspiration-catcher/ai.properties");
        configFile.getParentFile().mkdirs();
        try (OutputStream output = new FileOutputStream(configFile)) {
            props.store(output, "AI Configuration for Inspiration Catcher");
        } catch (IOException e) {
            logger.error("保存设置失败", e);
            statusManager.showAlert("保存失败: " + e.getMessage());
        }
    }

    // === Private helpers ===

    private void generateAndEditPrompt(Idea idea) {
        statusLabel.setText("🤖 正在分析灵感特点，生成个性化提示词...");
        disableAIButtons();
        aiService.generateCustomPrompt(idea)
                .thenAccept(customPrompt -> Platform.runLater(() -> {
                    showPromptEditor(idea, customPrompt);
                    enableAIButtons();
                }))
                .exceptionally(_ -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("提示词生成失败");
                        statusManager.showAlert("提示词生成失败\n使用默认模板继续？");
                        showPromptEditor(idea, getDefaultPrompt(idea));
                        enableAIButtons();
                    });
                    return null;
                });
    }

    private void showPromptEditor(Idea idea, String initialPrompt) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("定制AI提示词");
        dialog.setHeaderText("已根据您的灵感生成个性化提示词，可修改后发送给AI");
        TextArea promptEditor = new TextArea(initialPrompt);
        promptEditor.setWrapText(true);
        promptEditor.setPrefSize(600, 400);
        promptEditor.setStyle("-fx-font-family: 'Monaco', 'Consolas', monospace; -fx-font-size: 12px;");
        Label hintLabel = new Label("提示：您可以根据需要修改提示词，调整AI的思考方向");
        hintLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px; -fx-padding: 0 0 5 0;");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(hintLabel, promptEditor);
        dialog.getDialogPane().setContent(content);
        ButtonType sendButton = new ButtonType("发送给AI", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, cancelButton);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == sendButton) generateCustomPromptStreaming(idea, promptEditor.getText());
            return buttonType;
        });
        dialog.showAndWait();
    }

    private void generateCustomPromptStreaming(Idea idea, String customPrompt) {
        statusLabel.setText("🤖 AI生成中...");
        aiSuggestions.setText("");
        disableAIButtons();
        aiService.generateWithCustomPromptStreaming(customPrompt,
            token -> Platform.runLater(() -> aiSuggestions.appendText(token)),
            () -> Platform.runLater(() -> {
                statusLabel.setText("AI扩展生成完成");
                enableAIButtons();
                addAIResultActions(idea, customPrompt);
                synchronized (aiLock) { isAIGenerating = false; }
            }),
            error -> Platform.runLater(() -> {
                statusLabel.setText("AI扩展失败: " + error.getMessage());
                enableAIButtons();
                synchronized (aiLock) { isAIGenerating = false; }
            })
        );
    }

    private void addAIResultActions(Idea idea, String usedPrompt) {
        VBox aiContainer = (VBox) aiSuggestions.getParent();
        if (aiButtonContainer != null && aiContainer.getChildren().contains(aiButtonContainer)) {
            aiContainer.getChildren().remove(aiButtonContainer);
            aiButtonContainer = null;
        }
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        Button insertBtn = new Button("📝 插入到编辑器");
        insertBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertBtn.setOnAction(_ -> insertAIToEditor());
        Button savePromptBtn = new Button("💾 保存提示词模板");
        savePromptBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        savePromptBtn.setOnAction(_ -> savePromptTemplate(idea, usedPrompt));
        Button regenerateBtn = new Button("🔄 重新生成");
        regenerateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        regenerateBtn.setOnAction(_ -> {
            synchronized (aiLock) {
                if (isAIGenerating) { statusManager.showAlert("AI正在思考中，请稍后再试"); return; }
                isAIGenerating = true;
            }
            showPromptEditor(idea, usedPrompt);
        });
        buttonBox.getChildren().addAll(insertBtn, savePromptBtn, regenerateBtn);
        aiButtonContainer = buttonBox;
        aiContainer.getChildren().add(buttonBox);
    }

    private void addInsertButtons() {
        VBox aiContainer = (VBox) aiSuggestions.getParent();
        if (aiButtonContainer != null && aiContainer.getChildren().contains(aiButtonContainer)) {
            for (Node node : ((HBox) aiButtonContainer).getChildren()) {
                if (node instanceof Button) { node.setVisible(true); node.setDisable(false); }
            }
            return;
        }
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        Button insertToEditor = new Button("📝 插入到编辑器");
        insertToEditor.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertToEditor.setOnAction(_ -> insertAIToEditor());
        Button copyToClipboard = new Button("📋 复制建议");
        copyToClipboard.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        copyToClipboard.setOnAction(_ -> copyAIToClipboard());
        buttonBox.getChildren().addAll(insertToEditor, copyToClipboard);
        aiButtonContainer = buttonBox;
        aiContainer.getChildren().add(buttonBox);
    }

    private void insertAIToEditor() {
        String aiContent = aiSuggestions.getText();
        if (aiContent != null && !aiContent.trim().isEmpty()) {
            String currentContent = markdownEditor.getText();
            String separator = "\n\n---\n\n## AI扩展建议\n\n";
            markdownEditor.setText(currentContent + separator + aiContent + "\n\n---\n\n");
            mainTabPane.getSelectionModel().select(2);
            statusLabel.setText("AI建议已插入编辑器");
            onSave.run();
        }
    }

    private void copyAIToClipboard() {
        String aiContent = aiSuggestions.getText();
        if (aiContent != null && !aiContent.trim().isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(aiContent);
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("AI建议已复制到剪贴板");
        }
    }

    private void savePromptTemplate(Idea idea, String prompt) {
        TextInputDialog dialog = new TextInputDialog("提示词模板-" + idea.getTitle());
        dialog.setTitle("保存提示词模板");
        dialog.setHeaderText("为这个提示词模板命名");
        dialog.setContentText("模板名称:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                File templatesDir = new File(System.getProperty("user.home") + "/.inspiration-catcher/prompt-templates/");
                templatesDir.mkdirs();
                File templateFile = new File(templatesDir, name + ".txt");
                try (PrintWriter writer = new PrintWriter(templateFile)) {
                    writer.println("## " + name);
                    writer.println("灵感类型: " + idea.getType().getDisplayName());
                    writer.println("生成时间: " + java.time.LocalDateTime.now());
                    writer.println();
                    writer.println(prompt);
                }
                statusLabel.setText("提示词模板已保存: " + name);
            } catch (IOException e) {
                logger.error("保存提示词模板失败", e);
                statusManager.showAlert("保存失败\n无法保存模板: " + e.getMessage());
            }
        });
    }

    private String getDefaultPrompt(Idea idea) {
        return String.format("""
        作为创意扩展助手，请对以下灵感进行深入思考：

        《%s》
        %s

        请从多个角度分析这个灵感，包括：
        1. 核心价值：这个想法的独特之处是什么？
        2. 连接扩展：它可以与哪些现有概念或领域连接？
        3. 实现路径：如果要实现它，需要哪些步骤和资源？

        请用中文回答，语气亲切而专业。
        """,
                idea.getTitle() != null ? idea.getTitle() : "未命名灵感",
                idea.getContent() != null ? idea.getContent() : "[内容为空]"
        );
    }

    private void disableAIButtons() {
        Platform.runLater(() -> {
            VBox aiContainer = (VBox) aiSuggestions.getParent();
            for (Node node : aiContainer.getChildren()) {
                if (node instanceof Button) { node.setDisable(true); node.setStyle("-fx-opacity: 0.5;"); }
                else if (node instanceof HBox) {
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Button) { child.setDisable(true); child.setStyle("-fx-opacity: 0.5;"); }
                    }
                }
            }
        });
    }

    private void enableAIButtons() {
        Platform.runLater(() -> {
            VBox aiContainer = (VBox) aiSuggestions.getParent();
            for (Node node : aiContainer.getChildren()) {
                if (node instanceof Button) { node.setDisable(false); node.setStyle(""); }
                else if (node instanceof HBox) {
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Button) { child.setDisable(false); child.setStyle(""); }
                    }
                }
            }
        });
    }
}
