package com.inspiration.catcher.service;

import com.inspiration.catcher.dao.IdeaDao;
import com.inspiration.catcher.model.Idea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class IdeaService {
    private static final Logger logger = LoggerFactory.getLogger(IdeaService.class);
    private final IdeaDao ideaDao = new IdeaDao();

    // 根据ID获取灵感
    public Idea getIdeaById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return ideaDao.findById(id);
    }

    // 修改 saveIdea 方法
    public Idea saveIdea(Idea idea) {
        logger.info("保存灵感: {}", idea.getTitle());
        // 确保更新时间
        idea.setUpdatedAt(LocalDateTime.now());
        Idea savedIdea;
        if (idea.getId() == null || idea.getId() == 0) {
            savedIdea = ideaDao.insert(idea);
            // 对于新插入的灵感，不需要刷新，因为insert方法已经返回了包含ID的对象
        } else {
            savedIdea = ideaDao.update(idea);
            // 对于更新的灵感，刷新数据确保最新
            refreshIdeaData(savedIdea);
        }

        return savedIdea;
    }

    // 修改 refreshIdeaData 方法，添加空值检查
    private void refreshIdeaData(Idea idea) {
        // 检查ID是否为null
        if (idea == null || idea.getId() == null) {
            logger.warn("尝试刷新数据但ID为null");
            return;
        }

        // 重新从数据库加载确保数据最新
        Idea refreshed = ideaDao.findById(idea.getId());
        if (refreshed != null) {
            idea.setTitle(refreshed.getTitle());
            idea.setContent(refreshed.getContent());
            idea.setUpdatedAt(refreshed.getUpdatedAt());
            idea.setTags(refreshed.getTags());
        }
    }

    // 删除灵感
    public boolean deleteIdea(Integer id) {
        if (id == null || id <= 0) {
            logger.error("尝试删除无效的灵感ID: {}", id);
            return false;
        }

        logger.info("删除灵感 ID: {}", id);
        return ideaDao.delete(id);
    }

    // 搜索灵感
    public List<Idea> searchIdeas(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ideaDao.findAll();
        }
        return ideaDao.searchByKeyword(keyword.trim());
    }

    // 获取统计信息
    public int getTotalCount() {
        return ideaDao.getTotalCount();
    }

    public int getWeeklyCount() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return ideaDao.getCountAfter(oneWeekAgo);
    }

    public String getTopTag() {
        return ideaDao.getMostUsedTag();
    }

    // 生成AI建议
    public String generateAISuggestions(Idea idea) {
        if (idea == null) {
            return "请先选择一个灵感";
        }

        logger.info("为灵感生成AI建议: {}", idea.getId());

        // TODO: 调用AI API
        // 这里只是一个示例
        return String.format("""
            🤖 AI扩展建议：
            
            📌 相关概念：
            1. %s
            2. %s
            3. %s
            
            💡 潜在应用：
            • 应用场景一：%s
            • 应用场景二：%s
            
            ❓ 深入问题：
            1. %s
            2. %s
            3. %s
            
            🔗 相关连接：
            • 考虑将这个想法与 [相关主题] 联系起来
            • 探索这个想法在 [另一个领域] 的应用
            """,
                "概念一",
                "概念二",
                "概念三",
                "具体应用一",
                "具体应用二",
                "为什么这个想法重要？",
                "这个想法有哪些限制？",
                "如何验证这个想法？"
        );
    }

    // 生成大纲
    public String generateOutline(List<Idea> ideas) {
        if (ideas == null || ideas.isEmpty()) {
            return "没有可用的灵感";
        }

        StringBuilder outline = new StringBuilder("# 灵感大纲\n\n");

        for (int i = 0; i < ideas.size(); i++) {
            Idea idea = ideas.get(i);
            outline.append("## ").append(i + 1).append(". ").append(idea.getTitle()).append("\n");
            outline.append(idea.getContent()).append("\n\n");

            if (!idea.getTags().isEmpty()) {
                outline.append("相关标签: ");
                idea.getTags().forEach(tag -> outline.append("#").append(tag.getName()).append(" "));
                outline.append("\n");
            }

            outline.append("\n---\n\n");
        }

        return outline.toString();
    }
}