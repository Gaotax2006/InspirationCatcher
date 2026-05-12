package com.inspiration.catcher.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownUtil {
    private static final MutableDataSet options = new MutableDataSet();
    private static final Parser parser;
    private static final HtmlRenderer renderer;
    static {
        // 配置Markdown 解析选项
        options.set(Parser.EXTENSIONS, java.util.Arrays.asList(
                com.vladsch.flexmark.ext.tables.TablesExtension.create(),
                com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension.create()
        ));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }
    // 将Markdown 转换为HTML
    public static String markdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty())
            return "<div style='color: #999; font-style: italic;'>暂无内容</div>";
        try {
            com.vladsch.flexmark.util.ast.Node document = parser.parse(markdown);
            String html = renderer.render(document);
            return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    %s
                </style>
            </head>
            <body class='markdown-body'>
                %s
            </body>
            </html>
            """,getMarkdownStyles(), html);
        } catch (Exception e) {return "<pre>" + escapeHtml(markdown) + "</pre>";}
    }
    // 获取 Markdown样式（深色主题）
    private static String getMarkdownStyles() {
        return """
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body.markdown-body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial,
                "Microsoft YaHei", "微软雅黑", "PingFang SC", "Hiragino Sans GB", "Source Han Sans", "WenQuanYi Micro Hei", sans-serif;
                font-size: 16px;
                line-height: 1.6;
                word-wrap: break-word;
                padding: 20px;
                color: #E4E4E7;
                background-color: #1E1E2E;
            }
            .markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
                margin-top: 24px;
                margin-bottom: 16px;
                font-weight: 600;
                line-height: 1.25;
                border-bottom: 1px solid #2A2A3C;
                padding-bottom: 0.3em;
                color: #F0C060;
            }
            .markdown-body h1 { font-size: 2em; }
            .markdown-body h2 { font-size: 1.5em; }
            .markdown-body h3 { font-size: 1.25em; color: #E4E4E7; }
            .markdown-body p { margin-top: 0; margin-bottom: 16px; }
            .markdown-body ul, .markdown-body ol {
                padding-left: 2em;
                margin-top: 0;
                margin-bottom: 16px;
            }
            .markdown-body li { margin-bottom: 0.25em; }
            .markdown-body blockquote {
                padding: 0 1em;
                color: #A1A1AA;
                border-left: 0.25em solid #3A3A4C;
                margin: 0 0 16px 0;
            }
            .markdown-body pre {
                padding: 16px;
                overflow: auto;
                font-size: 85%;
                line-height: 1.45;
                background-color: #13131E;
                border-radius: 6px;
                margin-bottom: 16px;
                font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, "Courier New", Courier, monospace;
                border: 1px solid #2A2A3C;
            }
            .markdown-body code {
                padding: 0.2em 0.4em;
                margin: 0;
                font-size: 85%;
                background-color: rgba(240, 192, 96, 0.1);
                border-radius: 3px;
                font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, "Courier New", Courier, monospace;
                color: #F5D08A;
            }
            .markdown-body pre code {
                background-color: transparent;
                padding: 0;
                color: #E4E4E7;
            }
            .markdown-body table {
                border-spacing: 0;
                border-collapse: collapse;
                margin-bottom: 16px;
                width: 100%;
            }
            .markdown-body table th, .markdown-body table td {
                padding: 6px 13px;
                border: 1px solid #3A3A4C;
            }
            .markdown-body table th {
                font-weight: 600;
                background-color: #181825;
                color: #F0C060;
            }
            .markdown-body table td {
                color: #E4E4E7;
            }
            .markdown-body img { max-width: 100%; }
            .markdown-body a { color: #F0C060; text-decoration: none; }
            .markdown-body a:hover { text-decoration: underline; }
            .markdown-body strong,
            .markdown-body b {
                font-weight: 700;
                color: #F5D08A;
            }
            .markdown-body em,
            .markdown-body i {
                font-style: italic;
            }
            .markdown-body hr {
                height: 1px;
                padding: 0;
                margin: 24px 0;
                background-color: #3A3A4C;
                border: 0;
            }
            """;
    }
    // HTML转义
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    // 提取Markdown纯文本（用于搜索和预览）
    public static String extractPlainText(String markdown) {
        if (markdown == null) return "";
        // 移除Markdown语法标记
        String text = markdown
                .replaceAll("^#{1,6}\\s*", "") // 标题
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // 粗体
                .replaceAll("\\*([^*]+)\\*", "$1") // 斜体
                .replaceAll("`([^`]+)`", "$1") // 内联代码
                .replaceAll("~~([^~]+)~~", "$1") // 删除线
                .replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1") // 图片
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1") // 链接
                .replaceAll("^>\\s*", "") // 引用
                .replaceAll("^-\\s*", "") // 列表项
                .replaceAll("^\\d+\\.\\s*", "") // 数字列表
                .replaceAll("```[\\s\\S]*?```", "") // 代码块
                .replaceAll("`[^`]*`", "") // 内联代码
                .replaceAll("\\*\\*[^*]*\\*\\*", "") // 粗体
                .replaceAll("\\*[^*]*\\*", ""); // 斜体
        return text.trim();
    }
}