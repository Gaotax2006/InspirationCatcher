package com.inspiration.catcher.manager;

import com.inspiration.catcher.dao.IdeaDao;
import com.inspiration.catcher.dao.MindMapNodeDao;
import com.inspiration.catcher.dao.ProjectDao;
import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Project;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectManager {
    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);

    private final ProjectDao projectDao = new ProjectDao();
    private final IdeaDao ideaDao = new IdeaDao();
    private final MindMapNodeDao mindMapNodeDao = new MindMapNodeDao();
    // 当前选中的项目
    private final ObjectProperty<Project> currentProject = new SimpleObjectProperty<>();
    // 项目列表
    private final ObservableList<Project> projectList = FXCollections.observableArrayList();
    // 默认项目ID
    public static final int DEFAULT_PROJECT_ID = 1;
    // 构造函数
    public ProjectManager() {
        // 先加载项目列表
        loadAllProjects();
        // 设置当前项目 - 修复空指针问题
        Project defaultProject = findDefaultProject();
        if (defaultProject != null) {
            setCurrentProject(defaultProject);
        } else {
            // 创建新的默认项目
            logger.warn("未找到默认项目，创建新项目");
            Project newProject = createProject("默认项目", "系统默认项目", "#4A90E2");
            if (newProject != null) {
                setCurrentProject(newProject);
            } else {
                logger.error("无法创建默认项目");
                // 创建空项目对象防止空指针
                Project fallback = new Project();
                fallback.setId(DEFAULT_PROJECT_ID);
                fallback.setName("临时项目");
                currentProject.set(fallback);
            }
        }
    }

    // 加载所有项目
    public void loadAllProjects() {
        logger.info("加载所有项目");
        projectList.clear();
        List<Project> projects = projectDao.findAll();
        projectList.addAll(projects);
        logger.info("成功加载 {} 个项目", projects.size());
    }

    // 获取默认项目
    public Project findDefaultProject() {
        Project defaultProject = projectDao.findById(DEFAULT_PROJECT_ID);
        if (defaultProject == null) {
            // 创建默认项目
            defaultProject = new Project();
            defaultProject.setId(DEFAULT_PROJECT_ID);
            defaultProject.setName("默认项目");
            defaultProject.setDescription("系统默认项目，存放未分类的灵感");
            defaultProject.setColor("#4A90E2");
            projectDao.createDefaultProject();
            defaultProject = projectDao.findById(DEFAULT_PROJECT_ID);
        }
        return defaultProject;
    }

    // 创建新项目
    public Project createProject(String name, String description, String color) {
        if (name == null || name.trim().isEmpty()) {logger.error("项目名称不能为空");return null;}

        Project project = new Project();
        project.setName(name.trim());
        project.setDescription(description != null ? description.trim() : "");
        project.setColor(color != null ? color : "#36B37E");
        Project savedProject = projectDao.save(project);
        if (savedProject != null) {
            projectList.add(savedProject);
            logger.info("创建新项目成功: ID={}, Name={}", savedProject.getId(), savedProject.getName());
        }

        return savedProject;
    }

    // 更新项目
    public Project updateProject(Project project) {
        if (project == null || project.getId() == null) {
            logger.error("项目无效，无法更新");
            return null;
        }
        Project updatedProject = projectDao.save(project);
        if (updatedProject != null) {
            // 更新列表中的项目
            int index = projectList.indexOf(project);
            if (index >= 0) projectList.set(index, updatedProject);
            // 如果更新的是当前项目，更新currentProject
            if (currentProject.get() != null && currentProject.get().getId().equals(updatedProject.getId()))
                currentProject.set(updatedProject);
            logger.info("更新项目成功: ID={}", updatedProject.getId());
        }

        return updatedProject;
    }

    // 删除项目（不能删除默认项目）
    public boolean deleteProject(Project project) {
        if (project == null || project.getId() == null || project.getId() == DEFAULT_PROJECT_ID) {
            logger.error("无法删除项目: 项目无效或是默认项目");
            return false;
        }
        // 检查项目是否有灵感
        List<Idea> projectIdeas = ideaDao.findByProjectId(project.getId());
        if (!projectIdeas.isEmpty()) {
            logger.warn("项目包含 {} 个灵感，无法直接删除", projectIdeas.size());
            return false; // 或者可以将灵感移动到默认项目再删除
        }

        boolean success = projectDao.delete(project.getId());
        if (success) {
            projectList.remove(project);
            // 如果删除的是当前项目，切换到默认项目
            if (currentProject.get() != null && currentProject.get().getId().equals(project.getId()))
                setCurrentProject(findDefaultProject());
            logger.info("删除项目成功: ID={}", project.getId());
        }
        return success;
    }
    // 切换到指定项目
    public boolean switchToProject(Project project) {
        if (project == null) {logger.error("切换项目失败：项目为null");return false;}
        // 如果已经是当前项目，直接返回
        Project current = getCurrentProject();
        if (current != null && current.getId() != null &&
                current.getId().equals(project.getId())) {
            logger.info("已经是当前项目，无需切换");
            return true;
        }
        // 验证项目存在
        Project targetProject = projectDao.findById(project.getId());
        if (targetProject == null) {
            logger.error("切换项目失败：项目不存在，ID={}", project.getId());
            return false;
        }
        setCurrentProject(targetProject);
        logger.info("切换到项目: ID={}, Name={}", targetProject.getId(), targetProject.getName());
        return true;
    }

    // 切换到项目ID
    public boolean switchToProject(int projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            logger.error("切换项目失败：项目不存在，ID={}", projectId);
            return false;
        }return switchToProject(project);
    }

    // 获取当前项目的灵感列表
    public List<Idea> getCurrentProjectIdeas() {
        return ideaDao.findByProjectId(currentProject.get() == null?currentProject.get().getId():DEFAULT_PROJECT_ID);
    }
    // 更新项目统计信息
    public void updateProjectStatistics(Integer projectId) {
        if (projectId == null) return;
        projectDao.updateProjectStats(projectId);
        // 更新本地列表中的项目
        for (int i = 0; i < projectList.size(); i++) {
            Project p = projectList.get(i);
            if (p.getId().equals(projectId)) {
                Project updated = projectDao.findById(projectId);
                if (updated != null) {
                    projectList.set(i, updated);
                    // 如果更新的是当前项目，更新currentProject
                    if (currentProject.get() != null && currentProject.get().getId().equals(projectId))
                        currentProject.set(updated);
                }
                break;
            }
        }
    }
    // 将灵感移动到另一个项目
    public boolean moveIdeaToProject(Idea idea, Project targetProject) {
        if (idea == null || targetProject == null || idea.getId() == null || targetProject.getId() == null) {
            logger.error("移动灵感失败：参数无效");
            return false;
        }
        if (idea.getProjectId() != null && idea.getProjectId().equals(targetProject.getId())) {
            logger.info("灵感已在目标项目中，无需移动");
            return true;
        }
        // 更新灵感的项目ID
        idea.setProjectId(targetProject.getId());
        Idea updatedIdea = ideaDao.save(idea);

        if (updatedIdea != null) {
            // 更新两个项目的统计信息
            if (idea.getProjectId() != null) updateProjectStatistics(idea.getProjectId());
            updateProjectStatistics(targetProject.getId());
            logger.info("将灵感移动到项目: IdeaID={}, TargetProjectID={}", idea.getId(), targetProject.getId());
            return true;
        }
        return false;
    }

    // Getters and Setters
    public ObservableList<Project> getProjectList() {
        return projectList;
    }
    public Project getCurrentProject() {
        return currentProject.get();
    }

    public ObjectProperty<Project> currentProjectProperty() {
        return currentProject;
    }
    public void setCurrentProject(Project project) {
        this.currentProject.set(project);
    }
    // 获取项目数量
    public int getProjectCount() {return projectList.size();}
    // 根据ID获取项目
    public Project getProjectById(Integer id) {return id == null ? null : projectDao.findById(id);}
}