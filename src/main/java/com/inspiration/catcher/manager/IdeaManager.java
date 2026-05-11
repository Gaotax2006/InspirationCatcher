package com.inspiration.catcher.manager;

import com.inspiration.catcher.dao.IdeaDao;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Project;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.service.AIService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IdeaManager {
    private static final Logger logger = LoggerFactory.getLogger(IdeaManager.class);

    private final IdeaDao ideaDao = new IdeaDao();
    private final AIService aiService = new AIService();
    private final ObservableList<Idea> ideaList = FXCollections.observableArrayList();
    private final ProjectManager projectManager;
    public IdeaManager(ProjectManager projectManager) {this.projectManager = projectManager;}
    // 加载所有灵感
    public void loadAllIdeas() {
        logger.info("加载所有灵感");
        Project currentProject = projectManager.getCurrentProject();
        ideaList.clear();
        if (currentProject == null) currentProject = projectManager.findDefaultProject();
        List<Idea> ideas = ideaDao.findByProjectId(currentProject.getId());
        ideaList.addAll(ideas);
        logger.info("成功加载 {} 条灵感", ideas.size());
    }
    public ObservableList<Idea> getIdeaList() {loadAllIdeas(); return ideaList;}
    public int getIdeaCount() {
        return ideaList.size();
    }
    public Idea saveIdea(Idea idea) {
        if (idea == null) return null;
        // 确保projectId不为空
        if (idea.getProjectId() == null) {
            Project currentProject = projectManager.getCurrentProject();
            idea.setProjectId(currentProject != null ? currentProject.getId() : ProjectManager.DEFAULT_PROJECT_ID);
        }
        // 确保标题不为空
        if (idea.getTitle() == null || idea.getTitle().trim().isEmpty()) {
            String generatedTitle = generateTitleFromContent(idea.getContent());
            idea.setTitle(generatedTitle);
        }
        // 确保内容不为空
        if (idea.getContent() == null || idea.getContent().trim().isEmpty()) return null;
        // 确保时间戳
        if (idea.getCreatedAt() == null) idea.setCreatedAt(LocalDateTime.now());
        idea.setUpdatedAt(LocalDateTime.now());
        // 确保重要性有默认值
        if (idea.getImportance() < 1 || idea.getImportance() > 5) idea.setImportance(3);
        // 如果是更新操作，需要获取旧标签以更新计数
        List<Tag> oldTags = null;
        if (idea.getId() != null) {
            Idea oldIdea = getIdeaById(idea.getId());
            if (oldIdea != null) oldTags = oldIdea.getTags();
        }
        // 保存灵感（这会自动更新数据库中的标签关联）
        Idea savedIdea = ideaDao.save(idea);
        if (savedIdea != null) {
            // 更新列表中的灵感
            updateIdeaInList(savedIdea);
            // 更新项目统计信息
            if (savedIdea.getProjectId() != null) {
                projectManager.updateProjectStatistics(savedIdea.getProjectId());
            }
            // 如果更新了标签，更新内存中的标签计数
            if (oldTags != null) {
                // 减少旧标签的计数
                updateTagUsageCountsInMemory(oldTags, -1);
                // 增加新标签的计数
                updateTagUsageCountsInMemory(savedIdea.getTags(), 1);
            } else {
                // 新增灵感，增加标签计数
                updateTagUsageCountsInMemory(savedIdea.getTags(), 1);
            }
        }

        return savedIdea;
    }
    // 生成标题（从内容中提取）
    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty())
            return "未命名灵感 " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        String trimmed = content.trim();
        // 取前50个字符作为标题
        if (trimmed.length() > 50) return trimmed.substring(0, 50) + "...";
        return trimmed;
    }
    // 更新列表中的灵感
    private void updateIdeaInList(Idea updatedIdea) {
        for (int i = 0; i < ideaList.size(); i++) {
            Idea existing = ideaList.get(i);
            if (existing.getId() != null && existing.getId().equals(updatedIdea.getId())) {
                ideaList.set(i, updatedIdea);
                logger.debug("列表中更新了灵感: ID={}", updatedIdea.getId());
                return;
            }
        }
        // 如果没找到，说明是新灵感，直接添加
        ideaList.add(updatedIdea);
    }
    // 删除灵感
    public void deleteIdea(Idea idea) {
        if (idea == null || idea.getId() == null) {logger.error("删除灵感失败：灵感或ID为空");return;}
        logger.info("删除灵感: {}", idea.getId());
        try {
            // 先获取这个灵感的所有标签
            List<Tag> tags = idea.getTags();
            logger.info("灵感 {} 关联了 {} 个标签", idea.getId(), tags.size());
            // 删除灵感（这会触发数据库的级联删除）
            boolean success = ideaDao.delete(idea.getId());
            if (success) {
                // 从列表中移除灵感
                ideaList.remove(idea);
                // 更新内存中标签的usageCount
                updateTagUsageCountsInMemory(tags, -1);
                logger.info("从列表中移除灵感，并更新了 {} 个标签的计数", tags.size());
            } else logger.error("数据库删除灵感失败: ID={}", idea.getId());
        } catch (Exception e) {logger.error("删除灵感失败: {}", idea.getId(), e);}
    }
    // 更新内存中标签的使用计数
    private void updateTagUsageCountsInMemory(List<Tag> tags, int delta) {
        if (tags == null || tags.isEmpty()) return;
        // 在内存列表中查找对应的标签并更新
        for (Tag tag : tags) for (Idea idea : ideaList) for (Tag ideaTag : idea.getTags())
            if (ideaTag.getId() != null && ideaTag.getId().equals(tag.getId())) {// 更新计数
                int currentCount = ideaTag.getUsageCount();
                ideaTag.setUsageCount(Math.max(0, currentCount + delta));
                break;
            }
    }
    public int cleanUnusedTags() {return cleanUnusedTags(false);}
    public int cleanUnusedTags(boolean dryRun) {
        logger.info("开始清理未使用标签，dryRun={}", dryRun);
        try {
            TagManager tagManager = new TagManager();
            if (dryRun) {
                // 只统计不删除
                List<Tag> unusedTags = tagManager.getUnusedTags();
                logger.info("检测到 {} 个未使用的标签", unusedTags.size());
                if (!unusedTags.isEmpty()) {
                    logger.info("未使用标签列表：");
                    for (Tag tag : unusedTags) logger.info("  - {} (ID: {})", tag.getName(), tag.getId());
                }
                return unusedTags.size();
            } else {
                // 实际删除
                int deletedCount = tagManager.cleanUnusedTags();
                if (deletedCount > 0) {
                    logger.info("成功清理 {} 个未使用的标签", deletedCount);
                    // 重新加载标签列表
                    tagManager.loadAllTags();
                }
                return deletedCount;
            }

        } catch (Exception e) {logger.error("清理未使用标签失败", e);return 0;}
    }
    // 统计方法
    public int getTotalCount() {return ideaDao.getTotalCount();}
    public int getWeeklyCount() {return ideaDao.getCountAfter(LocalDateTime.now().minusWeeks(1));}
    public String getTopTag() {
        return ideaDao.getMostUsedTag();
    }
    // 生成 AI建议
    public CompletableFuture<String> generateAISuggestions(Idea idea) {return aiService.generateSuggestions(idea);}
    // 根据 ID获取灵感
    public Idea getIdeaById(Integer id) {
        if (id == null || id <= 0) return null;
        // 先在列表中查找
        for (Idea idea : ideaList) if (idea.getId() != null && idea.getId().equals(id)) return idea;
        // 如果列表中没找到，从数据库加载
        Idea idea = ideaDao.findById(id);
        if (idea != null) ideaList.add(idea); // 添加到列表
        return idea;
    }
    // 根据项目ID获取灵感列表
    public List<Idea> getIdeasByProject(Integer projectId) {
        if (projectId == null) return new ArrayList<>();
        // 从数据库直接查询，避免从内存列表过滤
        return ideaDao.findByProjectId(projectId);
    }
}