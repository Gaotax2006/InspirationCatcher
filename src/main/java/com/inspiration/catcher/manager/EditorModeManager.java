// 创建新文件：controller/EditorModeManager.java
package com.inspiration.catcher.manager;

import com.inspiration.catcher.model.Idea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorModeManager {
    private static final Logger logger = LoggerFactory.getLogger(EditorModeManager.class);

    // 编辑模式枚举
    public enum EditorMode {
        NEW_IDEA,      // 新建模式
        EDIT_IDEA      // 编辑模式
    }

    private EditorMode currentMode = EditorMode.NEW_IDEA;
    private Idea currentIdea = null;

    // 切换到新建模式
    public void switchToNewMode() {
        logger.info("切换到新建模式");
        currentMode = EditorMode.NEW_IDEA;
        currentIdea = null;
    }

    // 切换到编辑模式
    public void switchToEditMode(Idea idea) {
        if (idea == null) {
            logger.error("切换到编辑模式失败：idea为null");
            return;
        }
        logger.info("切换到编辑模式：ID={}, Title={}", idea.getId(), idea.getTitle());
        currentMode = EditorMode.EDIT_IDEA;
        currentIdea = idea;
    }

    // 获取当前编辑的灵感（仅在编辑模式下有效）
    public Idea getCurrentIdea() {
        return currentIdea;
    }
    // 检查是否为新建模式
    public boolean isNewMode() {
        return currentMode == EditorMode.NEW_IDEA;
    }
    // 检查是否为编辑模式
    public boolean isEditMode() {return currentMode == EditorMode.EDIT_IDEA;}
    // 保存后处理（新建模式下使用）
    public void afterSave(Idea savedIdea) {
        if (savedIdea == null || savedIdea.getId() == null) {
            logger.error("保存后处理失败：savedIdea或ID为null");
            return;
        }
        // 从新建模式切换到编辑模式
        switchToEditMode(savedIdea);
        logger.info("已从新建模式切换到编辑模式，ID={}", savedIdea.getId());
    }

}