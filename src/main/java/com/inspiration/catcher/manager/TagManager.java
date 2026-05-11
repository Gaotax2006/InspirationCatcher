package com.inspiration.catcher.manager;

import com.inspiration.catcher.dao.TagDao;
import com.inspiration.catcher.model.Tag;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagManager {
    private static final Logger logger = LoggerFactory.getLogger(TagManager.class);
    private final TagDao tagDao = new TagDao();
    private final ObservableList<Tag> tagList = FXCollections.observableArrayList();
    // 加载所有标签
    public void loadAllTags() {
        logger.info("加载所有标签");
        tagList.clear();
        tagList.addAll(tagDao.findAll());
    }
    // 获取标签列表
    public ObservableList<Tag> getTagList() {
        return tagList;
    }
    // 根据名称查找或创建标签
    public Tag findOrCreateTag(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        String tagName = name.trim().toLowerCase(); // 转换为进行不区分大小写的比较
        // 首先在内存中查找
        for (Tag tag : tagList) {
            if (tag.getName().toLowerCase().equals(tagName)) {
                logger.debug("在内存中找到标签: {}", tag.getName());
                return tag;
            }
        }
        // 从数据库查找
        List<Tag> allTags = tagDao.findAll();
        for (Tag tag : allTags) {
            if (tag.getName().toLowerCase().equals(tagName)) {
                logger.debug("在数据库中找到标签: {}", tag.getName());
                // 添加到内存列表
                if (!tagList.contains(tag)) tagList.add(tag);
                return tag;
            }
        }

        // 创建新标签（保持原始大小写）
        Tag newTag = new Tag(name.trim()); // 使用原始输入的大小写
        logger.info("创建新标签: {}", newTag.getName());
        // 设置随机颜色
        String[] colors = {"#FF6B6B", "#4ECDC4", "#FFD166", "#06D6A0", "#118AB2"};
        newTag.setColor(colors[(int)(Math.random() * colors.length)]);
        Tag savedTag = tagDao.insert(newTag);

        if (savedTag != null && savedTag.getId() != null) {
            tagList.add(savedTag);
            logger.info("新标签保存到数据库成功: ID={}, Name={}", savedTag.getId(), savedTag.getName());
            return savedTag;
        }

        logger.warn("创建标签失败: {}", name);
        return newTag;
    }
    // 添加标签到灵感
    public boolean addTagToIdea(int ideaId, int tagId) {
        return tagDao.addTagToIdea(ideaId, tagId);
    }
    // 获取最常用的标签
    public List<Tag> getMostUsedTags(int limit) {
        return tagDao.getMostUsedTags(limit);
    }
    // 搜索标签
    public List<Tag> searchTags(String keyword) {
        return tagDao.searchTags(keyword);
    }
    // 获取标签数量
    public int getTagCount() {
        return tagList.size();
    }
    // 获取热门标签
    public List<Tag> getHotTags(int limit) {
        loadAllTags();
        List<Tag> hotTags = getMostUsedTags(limit);
        // 如果热门标签数量不够，补充一些普通标签
        if (hotTags.size() < limit) {
            List<Tag> allTags = tagDao.findAll();
            for (Tag tag : allTags) {
                if (hotTags.size() >= limit) break;
                boolean alreadyAdded = false;
                for (Tag hotTag : hotTags) {
                    if (hotTag.getId().equals(tag.getId())) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) hotTags.add(tag);
            }
        }
        return hotTags;
    }

    // 清理未使用的标签
    public int cleanUnusedTags() {
        logger.info("开始清理未使用的标签");

        try {
            // 获取未使用的标签
            List<Tag> unusedTags = tagDao.findUnusedTags();

            if (unusedTags.isEmpty()) {
                logger.info("没有未使用的标签需要清理");
                return 0;
            }

            int deletedCount = 0;
            List<Tag> toRemoveFromList = new ArrayList<>();

            logger.info("找到 {} 个未使用的标签", unusedTags.size());

            // 删除未使用的标签
            for (Tag tag : unusedTags) {
                try {
                    boolean deleted = tagDao.delete(tag.getId());
                    if (deleted) {
                        deletedCount++;
                        toRemoveFromList.add(tag);
                        logger.debug("删除未使用标签: ID={}, Name={}", tag.getId(), tag.getName());
                    }
                } catch (Exception e) {
                    logger.error("删除标签失败: ID={}, Name={}", tag.getId(), tag.getName(), e);
                }
            }

            // 从内存列表中移除已删除的标签
            tagList.removeAll(toRemoveFromList);

            logger.info("成功清理 {} 个未使用的标签", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("清理未使用标签失败", e);
            return 0;
        }
    }

    // 获取标签使用统计
    public Map<String, Integer> getTagUsageStats() {
        return tagDao.getAllTagUsageCounts();
    }

    // 获取未使用标签列表（用于显示）
    public List<Tag> getUnusedTags() {return tagDao.findUnusedTags();}
    // 批量清理标签（用户可选）
    public int cleanUnusedTagsWithConfirmation(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }
        int deletedCount = 0;
        List<Tag> toRemoveFromList = new ArrayList<>();
        for (Integer tagId : tagIds) {
            try {
                // 首先检查这个标签是否真的未使用
                Tag tag = findTagById(tagId);
                if (tag != null) {
                    boolean deleted = tagDao.delete(tagId);
                    if (deleted) {
                        deletedCount++;
                        toRemoveFromList.add(tag);
                        logger.info("删除标签: ID={}, Name={}", tag.getId(), tag.getName());
                    }
                }
            } catch (Exception e) {
                logger.error("删除标签失败: ID={}", tagId, e);
            }
        }
        // 从内存列表中移除
        tagList.removeAll(toRemoveFromList);
        return deletedCount;
    }

    // 根据ID查找标签
    private Tag findTagById(int tagId) {
        for (Tag tag : tagList) if (tag.getId() != null && tag.getId() == tagId) return tag;
        return tagDao.findById(tagId);
    }
}