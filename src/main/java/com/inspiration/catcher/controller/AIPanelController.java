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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
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
            statusLabel.setText("🤖 AI思考中...");
            aiSuggestions.setText("正在生成AI建议，请稍候...");
            disableAIButtons();
            ideaManager.generateAISuggestions(selected)
                    .thenAccept(suggestions -> Platform.runLater(() -> {
                        aiSuggestions.setText(suggestions);
                        statusLabel.setText("AI建议生成完成");
                        enableAIButtons();
                        addInsertButtons();
                        synchronized (aiLock) { isAIGenerating = false; }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            aiSuggestions.setText("AI建议生成失败: " + e.getMessage());
                            statusLabel.setText("AI生成失败");
                            enableAIButtons();
                            synchronized (aiLock) { isAIGenerating = false; }
                        });
                        return null;
                    });
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

    public void showSettings() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI设置");
        dialog.setHeaderText("配置AI API密钥");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("输入DeepSeek API密钥");
        apiKeyField.setText(AIConfig.getApiKey());
        Label infoLabel = new Label("获取API密钥：https://platform.deepseek.com/api_keys");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(new Label("API密钥:"), 0, 0);
        grid.add(apiKeyField, 1, 0);
        grid.add(infoLabel, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? apiKeyField.getText() : null);
        dialog.showAndWait().ifPresent(apiKey -> {
            saveApiKey(apiKey);
            statusLabel.setText("API密钥已保存");
        });
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
            if (buttonType == sendButton) generateWithCustomPrompt(idea, promptEditor.getText());
            return buttonType;
        });
        dialog.showAndWait();
    }

    private void generateWithCustomPrompt(Idea idea, String customPrompt) {
        statusLabel.setText("🤖 AI正在根据您的提示词思考...");
        aiSuggestions.setText("正在生成AI扩展，请稍候...");
        disableAIButtons();
        aiService.generateWithCustomPrompt(customPrompt)
                .thenAccept(suggestions -> Platform.runLater(() -> {
                    displayAIResults(idea, suggestions, customPrompt);
                    enableAIButtons();
                    synchronized (aiLock) { isAIGenerating = false; }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        aiSuggestions.setText("AI扩展生成失败: " + e);
                        statusLabel.setText("AI扩展失败");
                        enableAIButtons();
                        synchronized (aiLock) { isAIGenerating = false; }
                    });
                    return null;
                });
    }

    private void displayAIResults(Idea idea, String suggestions, String usedPrompt) {
        aiSuggestions.setText(suggestions);
        statusLabel.setText("AI扩展生成完成");
        String metaInfo = String.format("\n\n---\n\n*提示词定制信息*\n- 灵感ID: %d\n- 生成时间: %s\n- 提示词长度: %d字符",
                idea.getId() != null ? idea.getId() : 0,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                usedPrompt.length());
        aiSuggestions.appendText(metaInfo);
        addAIResultActions(idea, usedPrompt);
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

    private void saveApiKey(String apiKey) {
        try {
            Properties props = new Properties();
            props.setProperty("ai.api.key", apiKey);
            File configFile = new File(System.getProperty("user.home") + "/.inspiration-catcher/ai.properties");
            configFile.getParentFile().mkdirs();
            try (OutputStream output = new FileOutputStream(configFile)) { props.store(output, "AI Configuration"); }
        } catch (IOException e) {
            logger.error("保存API密钥失败", e);
            statusManager.showAlert("保存失败\n\n无法保存API密钥: " + e.getMessage());
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
