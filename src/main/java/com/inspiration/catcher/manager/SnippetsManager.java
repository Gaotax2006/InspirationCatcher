package com.inspiration.catcher.manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages code/text snippets for quick insertion into the markdown editor.
 */
public class SnippetsManager {

    private final ObservableList<Snippet> snippets = FXCollections.observableArrayList();

    public SnippetsManager() {
        loadDefaults();
    }

    private void loadDefaults() {
        snippets.addAll(
            new Snippet("代码块", "```java\n// code here\n```", "编程"),
            new Snippet("表格", "| 列1 | 列2 | 列3 |\n|-----|-----|-----|\n| 内容 | 内容 | 内容 |", "格式化"),
            new Snippet("待办列表", "- [x] 已完成\n- [ ] 未完成", "格式化"),
            new Snippet("数学公式", "$$ E = mc^2 $$", "科学"),
            new Snippet("引用", "> 这是一段引用文字\n> 第二行引用", "格式化"),
            new Snippet("分隔线", "---\n", "格式化"),
            new Snippet("图片", "![alt text](image.jpg \"title\")", "媒体"),
            new Snippet("链接", "[显示文字](https://example.com)", "媒体"),
            new Snippet("折叠详情", "<details>\n<summary>点击展开</summary>\n\n隐藏内容\n</details>", "HTML"),
            new Snippet("提示块", "> **提示:** 这是一条提示信息", "写作"),
            new Snippet("警告块", "> **警告:** 请注意这一点", "写作"),
            new Snippet("Todo 模板", "## TODO\n- [ ] 任务1\n- [ ] 任务2\n\n## 笔记\n", "模板"),
            new Snippet("读书笔记", "# 书名\n\n**作者:** \n**评分:** ★★★☆☆\n\n## 摘要\n\n## 想法\n", "模板"),
            new Snippet("周报模板", "## 本周工作\n\n## 下周计划\n\n## 遇到的问题\n", "模板")
        );
    }

    public ObservableList<Snippet> getSnippets() {
        return snippets;
    }

    public ObservableList<Snippet> getSnippetsByCategory(String category) {
        if (category == null || category.isEmpty()) return snippets;
        ObservableList<Snippet> filtered = FXCollections.observableArrayList();
        for (Snippet s : snippets) {
            if (category.equals(s.getCategory())) filtered.add(s);
        }
        return filtered;
    }

    public ObservableList<String> getCategories() {
        ObservableList<String> categories = FXCollections.observableArrayList();
        categories.add("全部");
        for (Snippet s : snippets) {
            if (!categories.contains(s.getCategory())) categories.add(s.getCategory());
        }
        return categories;
    }

    public void addSnippet(Snippet snippet) {
        snippets.add(snippet);
    }

    public void removeSnippet(Snippet snippet) {
        snippets.remove(snippet);
    }

    /** A single snippet entry. */
    public static class Snippet {
        private final String name;
        private final String content;
        private final String category;

        public Snippet(String name, String content, String category) {
            this.name = name;
            this.content = content;
            this.category = category;
        }

        public String getName() { return name; }
        public String getContent() { return content; }
        public String getCategory() { return category; }

        @Override
        public String toString() { return name; }
    }
}
