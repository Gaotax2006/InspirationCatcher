package com.inspiration.catcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspiration.catcher.config.AIConfig;
import com.inspiration.catcher.model.Idea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static final String API_URL = AIConfig.getApiUrl();
    private static final String API_KEY = AIConfig.getApiKey();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // === Streaming API ===

    public CompletableFuture<Void> generateSuggestionsStreaming(Idea idea,
                                                                  Consumer<String> onToken,
                                                                  Runnable onComplete,
                                                                  Consumer<Throwable> onError) {
        String prompt = buildPrompt(idea);
        return callStreaming(prompt, onToken, onComplete, onError);
    }

    public CompletableFuture<Void> generateWithCustomPromptStreaming(String customPrompt,
                                                                      Consumer<String> onToken,
                                                                      Runnable onComplete,
                                                                      Consumer<Throwable> onError) {
        return callStreaming(customPrompt, onToken, onComplete, onError);
    }

    // === Non-streaming (fallback) ===

    public CompletableFuture<String> generateSuggestions(Idea idea) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(idea);
                return callNonStreaming(prompt);
            } catch (Exception e) {
                logger.error("AI generation failed", e);
                return getFallbackSuggestions();
            }
        });
    }

    public CompletableFuture<String> generateCustomPrompt(Idea idea) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callNonStreaming(buildPromptForPromptGeneration(idea));
                return parsePromptResponse(response);
            } catch (Exception e) {
                logger.error("Custom prompt generation failed", e);
                return getDefaultPrompt(idea);
            }
        });
    }

    public CompletableFuture<String> generateWithCustomPrompt(String customPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callNonStreaming(customPrompt);
            } catch (Exception e) {
                logger.error("Custom prompt generation failed", e);
                return getFallbackSuggestions();
            }
        });
    }

    // === Core streaming implementation ===

    private CompletableFuture<Void> callStreaming(String prompt,
                                                   Consumer<String> onToken,
                                                   Runnable onComplete,
                                                   Consumer<Throwable> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = buildStreamingRequest(prompt);
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines())
                        .body()
                        .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                        .forEach(line -> {
                            try {
                                String json = line.substring(6); // remove "data: " prefix
                                JsonNode root = mapper.readTree(json);
                                JsonNode delta = root.path("choices").get(0).path("delta").path("content");
                                if (!delta.isMissingNode() && delta.isTextual()) {
                                    String token = delta.asText();
                                    if (!token.isEmpty()) onToken.accept(token);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to parse streaming token", e);
                            }
                        });
                onComplete.run();
            } catch (Exception e) {
                logger.error("Streaming API call failed", e);
                onError.accept(e);
            }
        });
    }

    // === Core non-streaming implementation ===

    private String callNonStreaming(String prompt) throws Exception {
        HttpRequest request = buildNonStreamingRequest(prompt);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status " + response.statusCode() + ": " + response.body());
        }
        return parseNonStreamingResponse(response.body());
    }

    // === Request builders ===

    private HttpRequest buildStreamingRequest(String prompt) {
        String body = buildJsonBody(prompt, true);
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest buildNonStreamingRequest(String prompt) {
        String body = buildJsonBody(prompt, false);
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String buildJsonBody(String prompt, boolean stream) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", "deepseek-chat");
        root.put("max_tokens", AIConfig.getMaxTokens());
        root.put("temperature", AIConfig.getTemperature());
        root.put("stream", stream);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        return root.toString();
    }

    // === Response parsing ===

    private String parseNonStreamingResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode content = root.path("choices").get(0).path("message").path("content");
        if (content.isMissingNode() || !content.isTextual()) {
            throw new RuntimeException("Unexpected API response structure: " + responseBody);
        }
        return content.asText();
    }

    // === Prompt builders ===

    private String buildPrompt(Idea idea) {
        return String.format("""
            你是一个灵感扩展助手。请基于以下灵感内容，生成：
            1. 3个相关概念（每个1-2句话）
            2. 2个对立或挑战性观点
            3. 3个追问或探索性问题

            灵感标题：%s
            灵感内容：%s
            灵感类型：%s
            灵感心情：%s

            请以Markdown格式返回，但是不要加入```markdown```的标记，分为三个部分。
            """,
                safeTitle(idea), safeContent(idea),
                idea.getType() != null ? idea.getType().getDisplayName() : "想法",
                idea.getMood() != null ? idea.getMood().getDisplayName() : "中性"
        );
    }

    private String buildPromptForPromptGeneration(Idea idea) {
        return String.format("""
        你是一个提示词工程师，专门为创意工作设计AI提示词。

        用户有一个灵感需要AI帮助扩展：

        【灵感标题】%s
        【灵感内容】%s
        【灵感类型】%s
        【记录心情】%s

        请根据这个灵感的内容和特点，设计一个最适合的AI扩展提示词。

        要求：
        1. 先分析这个灵感的属性：它是文学性的、技术性的、哲学性的，还是实用性的？
        2. 根据分析结果，选择最适合的思考框架
        3. 设计提示词结构，至少包含3个思考维度
        4. 提示词要具体、有针对性，能引导AI生成有价值的扩展内容

        输出格式：
        ## 灵感分析
        - 类型判断：[这里写你的分析]
        - 适合框架：[这里写推荐的思考框架]

        ## 定制提示词开始
        [在这里输出完整的、可以直接使用的提示词]
        ## 定制提示词结束

        注意：提示词要用中文，语气要符合灵感的特点。
        """,
                safeTitle(idea), safeContent(idea),
                idea.getType() != null ? idea.getType().getDisplayName() : "想法",
                idea.getMood() != null ? idea.getMood().getDisplayName() : "中性"
        );
    }

    private String parsePromptResponse(String response) {
        try {
            int start = response.indexOf("## 定制提示词开始");
            int end = response.indexOf("## 定制提示词结束");
            if (start == -1) start = response.indexOf("定制提示词开始");
            if (end == -1) end = response.indexOf("定制提示词结束");
            if (start >= 0) {
                start = response.indexOf('\n', start) + 1;
                if (end > start) return response.substring(start, end).replace("*", "").trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to parse prompt response", e);
        }
        return response;
    }

    // === Test connection ===

    public boolean testConnection() {
        try {
            HttpRequest request = buildNonStreamingRequest("Hello");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            logger.warn("AI connection test failed", e);
            return false;
        }
    }

    // === Fallbacks ===

    private String getFallbackSuggestions() {
        return """
            ## AI扩展建议（离线模式）

            ### 相关概念
            1. 探索与当前灵感相关的领域
            2. 考虑与现有知识体系的联系
            3. 可能的应用场景和方向

            ### 追问问题
            1. 这个想法的核心假设是什么？
            2. 如何验证或实施这个想法？
            3. 如果...会发生什么？
            """;
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
                safeTitle(idea), safeContent(idea)
        );
    }

    private static String safeTitle(Idea idea) {
        return idea.getTitle() != null ? idea.getTitle() : "未命名灵感";
    }

    private static String safeContent(Idea idea) {
        return idea.getContent() != null ? idea.getContent() : "[内容为空]";
    }
}
