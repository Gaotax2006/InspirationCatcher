package com.inspiration.catcher.service;

import com.inspiration.catcher.config.AIConfig;
import com.inspiration.catcher.model.Idea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    // 替换硬编码的配置
    private static final String API_URL = AIConfig.getApiUrl();
    private static final String API_KEY = AIConfig.getApiKey();
    // 生成灵感扩展建议
    public CompletableFuture<String> generateSuggestions(Idea idea) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(idea);
                String response = callDeepSeekAPI(prompt);
                return parseResponse(response);
            } catch (Exception e) {
                logger.error("AI生成失败", e);
                return getFallbackSuggestions();
            }
        });
    }
    // 构建提示词
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
                idea.getTitle() != null ? idea.getTitle() : "无标题",
                idea.getContent() != null ? idea.getContent() : "",
                idea.getType() != null ? idea.getType().getDisplayName() : "想法",
                idea.getMood() != null ? idea.getMood().getDisplayName() : "中性"
        );
    }
    // 分析灵感并生成个性化提示词
    public CompletableFuture<String> generateCustomPrompt(Idea idea) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPromptForPromptGeneration(idea);
                String response = callDeepSeekAPI(prompt);
                logger.info(response);
                return parsePromptResponse(response);
            } catch (Exception e) {
                logger.error("生成提示词失败", e);
                return getDefaultPrompt(idea);
            }
        });
    }
    // 构建提示词生成的提示词
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
        2. 根据分析结果，选择最适合的思考框架：
           - 文学/创意类：关注隐喻、情感、叙事、语言风格
           - 技术/发明类：关注可行性、实施步骤、技术细节
           - 哲学/思考类：关注逻辑推导、假设检验、思想实验
           - 实用/计划类：关注行动步骤、资源需求、风险评估
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
                idea.getTitle() != null ? idea.getTitle() : "未命名灵感",
                idea.getContent() != null ? idea.getContent() : "[内容为空]",
                idea.getType() != null ? idea.getType().getDisplayName() : "想法",
                idea.getMood() != null ? idea.getMood().getDisplayName() : "中性"
        );
    }
    // 解析AI返回的提示词
    private String parsePromptResponse(String response) {
        try {
            // 提取"定制提示词"部分
            int startIndex = response.indexOf("## 定制提示词开始")+10;
            int endIndex = response.indexOf("## 定制提示词结束");
            if (startIndex == 9) startIndex = response.indexOf("定制提示词开始")+7;
            if (endIndex == -1) endIndex = response.indexOf("定制提示词结束");
            if (startIndex > 0) {
                logger.info("定制提示词位置: {} ~ {}",startIndex , endIndex);
                String promptPart = response.substring(startIndex, endIndex).replace("\\n","\n").replace("*","");
                logger.info("过滤后: {}",promptPart);
                return promptPart;
            }
            return response;
        } catch (Exception e) {
            logger.warn("解析提示词失败，返回原始内容", e);
            return response;
        }
    }
    // 默认提示词模板
    private String getDefaultPrompt(Idea idea) {
        return String.format("""
        请作为创意伙伴，对以下灵感进行深度扩展：
        
        《%s》
        %s
        
        请从以下角度思考：
        1. 核心价值和独特性
        2. 可能的实现路径
        3. 潜在挑战和解决方案
        
        请用中文回答，保持专业且亲切的语气。
        """,
                idea.getTitle() != null ? idea.getTitle() : "未命名灵感",
                idea.getContent() != null ? idea.getContent() : "[内容为空]"
        );
    }
    // 使用自定义提示词调用AI
    public CompletableFuture<String> generateWithCustomPrompt(String customPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callDeepSeekAPI(customPrompt);
                return parseResponse(response);
            } catch (Exception e) {
                logger.error("使用自定义提示词生成失败", e);
                return getFallbackSuggestions();
            }
        });
    }
    // 调用DeepSeek API
    private String callDeepSeekAPI(String prompt) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000); // 30秒连接超时
        conn.setReadTimeout(1000000);    // 1000秒读取超时
        // 记录请求信息（调试用）
        logger.debug("调用DeepSeek API，URL: {}", API_URL);
        logger.debug("API密钥长度: {}", API_KEY.length());
        logger.debug("API密钥: {}", API_KEY);
        // 构建正确的JSON请求体（使用JSON库更安全）
        String requestBody = buildRequestBody(prompt);
        logger.debug("请求体: {}", requestBody);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // 检查响应码
        int responseCode = conn.getResponseCode();
        logger.debug("API响应码: {}", responseCode);
        if (responseCode != 200) {
            // 读取错误响应
            String errorResponse = readErrorResponse(conn);
            logger.error("API调用失败，响应码: {}, 错误: {}", responseCode, errorResponse);
            throw new IOException("API调用失败，响应码: " + responseCode + ", 错误: " + errorResponse);
        }
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }
        conn.disconnect();
        return response.toString();
    }
    // 解析API响应
    private String parseResponse(String jsonResponse) {
        try {
            // 简单解析JSON，获取content字段
            int start = jsonResponse.indexOf("\"content\":\"") + 11;
            int end = jsonResponse.indexOf("\"", start);
            if (start > 10 && end > start) {
                String content = jsonResponse.substring(start, end);
                // 处理转义字符
                return content.replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\t", "\t");
            }
        } catch (Exception e) {
            logger.warn("解析API响应失败，返回原始JSON", e);
        }
        return jsonResponse;
    }
    // 备用建议（API失败时使用）
    private String getFallbackSuggestions() {
        return """
            ## AI扩展建议（离线模式——未联网或者AI连接超时）
            
            ### 相关概念
            1. 概念A：基于你的灵感，可以进一步探索...
            2. 概念B：考虑与现有知识体系的联系...
            3. 概念C：可能的应用场景和方向...
            
            ### 对立观点
            1. 反方观点：这个想法可能面临...
            2. 挑战性问题：需要思考...
            
            ### 追问问题
            1. 如果...会发生什么？
            2. 这个想法的核心假设是什么？
            3. 如何验证或实施这个想法？
            
            *注：离线模式生成，建议连接网络获取更个性化建议*
            """;
    }
    public boolean testConnection() {
        try {
            String testPrompt = "Hello";
            String response = callDeepSeekAPI(testPrompt);
            return response.contains("content");
        } catch (Exception e) {
            logger.error("API连接测试失败", e);
            return false;
        }
    }
    // 构建请求体（使用字符串格式化）
    private String buildRequestBody(String prompt) {
        // 转义JSON特殊字符
        String escapedPrompt = escapeJson(prompt);
        return String.format(
                "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":3000,\"temperature\":0.7}",
                escapedPrompt
        );
    }
    // 转义JSON特殊字符
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    // 读取错误响应
    private String readErrorResponse(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) error.append(line);
                    return error.toString();
                }
            }
        } catch (Exception e) {logger.error("读取错误响应失败", e);}
        return "无法读取错误信息";
    }
}