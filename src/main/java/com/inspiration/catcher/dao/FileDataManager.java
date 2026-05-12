package com.inspiration.catcher.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inspiration.catcher.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON file-based storage manager.
 * Each project is stored as a subdirectory under ./projects/.
 * Within each project: project.json, ideas.json, tags.json, mindmap.json
 *
 * This is the file-based storage replacing SQLite for project portability.
 */
public class FileDataManager {
    private static final Logger logger = LoggerFactory.getLogger(FileDataManager.class);
    private static final String PROJECTS_DIR = "projects";
    private static final String COLLECTIONS_FILE = "collections.json";

    private final ObjectMapper mapper;
    private String currentProjectPath;

    public FileDataManager() {
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        File dir = new File(PROJECTS_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    // === Project Management ===

    /** Create a new project directory with default files. */
    public boolean createProject(String projectName) {
        String safeName = sanitizeName(projectName);
        File projectDir = new File(PROJECTS_DIR + "/" + safeName);
        if (projectDir.exists()) {
            logger.warn("Project dir already exists: {}", safeName);
            return false;
        }
        projectDir.mkdirs();
        // Write project.json
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", projectName);
        meta.put("description", "");
        meta.put("color", "#C4843C");
        meta.put("createdAt", System.currentTimeMillis());
        meta.put("status", "ACTIVE");
        writeJson(new File(projectDir, "project.json"), meta);
        // Write empty arrays
        writeJson(new File(projectDir, "ideas.json"), new ArrayList<>());
        writeJson(new File(projectDir, "tags.json"), new ArrayList<>());
        writeJson(new File(projectDir, "mindmap_nodes.json"), new ArrayList<>());
        writeJson(new File(projectDir, "mindmap_connections.json"), new ArrayList<>());
        logger.info("Created project: {}", safeName);
        return true;
    }

    /** List all available projects. */
    public List<String> listProjects() {
        File dir = new File(PROJECTS_DIR);
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) return Collections.emptyList();
        return Arrays.stream(subdirs)
                .map(d -> {
                    File meta = new File(d, "project.json");
                    if (meta.exists()) {
                        Map<String, Object> data = readJson(meta, Map.class);
                        if (data != null) return (String) data.getOrDefault("name", d.getName());
                    }
                    return d.getName();
                })
                .collect(Collectors.toList());
    }

    /** Open a project by name. */
    public boolean openProject(String projectName) {
        String safeName = sanitizeName(projectName);
        String path = PROJECTS_DIR + "/" + safeName;
        File dir = new File(path);
        if (!dir.exists()) {
            logger.warn("Project not found: {}", path);
            return false;
        }
        currentProjectPath = path;
        logger.info("Opened project: {}", path);
        return true;
    }

    /** List all project directories. */
    public List<String> getProjectDirs() {
        File dir = new File(PROJECTS_DIR);
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) return Collections.emptyList();
        return Arrays.stream(subdirs).map(File::getName).collect(Collectors.toList());
    }

    public boolean isOpen() { return currentProjectPath != null; }
    public String getCurrentPath() { return currentProjectPath; }

    // === Idea CRUD ===

    public int saveIdea(Idea idea) {
        if (!isOpen()) return -1;
        File ideasFile = new File(currentProjectPath, "ideas.json");
        List<Map<String, Object>> ideas = readJsonList(ideasFile);

        if (idea.getId() != null && idea.getId() > 0) {
            // Update existing
            for (int i = 0; i < ideas.size(); i++) {
                Map<String, Object> m = ideas.get(i);
                if (m.get("id") instanceof Number n && n.intValue() == idea.getId()) {
                    ideas.set(i, ideaToMap(idea));
                    writeJson(ideasFile, ideas);
                    return idea.getId();
                }
            }
        }
        // New idea
        int newId = ideas.stream()
                .mapToInt(m -> m.get("id") instanceof Number n ? n.intValue() : 0)
                .max().orElse(0) + 1;
        idea.setId(newId);
        ideas.add(ideaToMap(idea));
        writeJson(ideasFile, ideas);
        return newId;
    }

    public List<Idea> loadAllIdeas() {
        if (!isOpen()) return Collections.emptyList();
        List<Map<String, Object>> data = readJsonList(new File(currentProjectPath, "ideas.json"));
        return data.stream().map(this::mapToIdea)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Idea loadIdeaById(int id) {
        List<Idea> ideas = loadAllIdeas();
        return ideas.stream().filter(i -> i.getId() != null && i.getId() == id).findFirst().orElse(null);
    }

    public boolean deleteIdea(int id) {
        if (!isOpen()) return false;
        File ideasFile = new File(currentProjectPath, "ideas.json");
        List<Map<String, Object>> ideas = readJsonList(ideasFile);
        boolean removed = ideas.removeIf(m -> m.get("id") instanceof Number n && n.intValue() == id);
        if (removed) writeJson(ideasFile, ideas);
        return removed;
    }

    // === Tag CRUD ===

    public int saveTag(Tag tag) {
        if (!isOpen()) return -1;
        File tagsFile = new File(currentProjectPath, "tags.json");
        List<Map<String, Object>> tags = readJsonList(tagsFile);

        if (tag.getId() != null && tag.getId() > 0) {
            for (int i = 0; i < tags.size(); i++) {
                Map<String, Object> m = tags.get(i);
                if (m.get("id") instanceof Number n && n.intValue() == tag.getId()) {
                    tags.set(i, tagToMap(tag));
                    writeJson(tagsFile, tags);
                    return tag.getId();
                }
            }
        }
        int newId = tags.stream()
                .mapToInt(m -> m.get("id") instanceof Number n ? n.intValue() : 0)
                .max().orElse(0) + 1;
        tag.setId(newId);
        tags.add(tagToMap(tag));
        writeJson(tagsFile, tags);
        return newId;
    }

    public List<Tag> loadAllTags() {
        if (!isOpen()) return Collections.emptyList();
        List<Map<String, Object>> data = readJsonList(new File(currentProjectPath, "tags.json"));
        return data.stream().map(this::mapToTag).collect(Collectors.toList());
    }

    // === Idea-Tag Linking (stored in ideas.json as tagIds array) ===

    public void linkIdeaToTag(int ideaId, int tagId) {
        if (!isOpen()) return;
        File ideasFile = new File(currentProjectPath, "ideas.json");
        List<Map<String, Object>> ideas = readJsonList(ideasFile);
        for (Map<String, Object> m : ideas) {
            if (m.get("id") instanceof Number n && n.intValue() == ideaId) {
                @SuppressWarnings("unchecked")
                List<Integer> tagIds = (List<Integer>) m.computeIfAbsent("tagIds", _ -> new ArrayList<Integer>());
                if (!tagIds.contains(tagId)) tagIds.add(tagId);
                writeJson(ideasFile, ideas);
                return;
            }
        }
    }

    public void unlinkIdeaFromTag(int ideaId, int tagId) {
        if (!isOpen()) return;
        File ideasFile = new File(currentProjectPath, "ideas.json");
        List<Map<String, Object>> ideas = readJsonList(ideasFile);
        for (Map<String, Object> m : ideas) {
            if (m.get("id") instanceof Number n && n.intValue() == ideaId) {
                @SuppressWarnings("unchecked")
                List<Integer> tagIds = (List<Integer>) m.get("tagIds");
                if (tagIds != null) tagIds.remove((Integer) tagId);
                writeJson(ideasFile, ideas);
                return;
            }
        }
    }

    /** Resolve tag IDs to Tag objects. */
    public List<Tag> getTagsForIdea(int ideaId) {
        Idea idea = loadIdeaById(ideaId);
        if (idea == null) return Collections.emptyList();
        // Tags are stored inline in the idea
        return idea.getTags();
    }

    // === Mind Map Nodes ===

    public int saveMindMapNode(MindMapNode node) {
        if (!isOpen()) return -1;
        File nodesFile = new File(currentProjectPath, "mindmap_nodes.json");
        List<Map<String, Object>> nodes = readJsonList(nodesFile);

        if (node.getId() != null && node.getId() > 0) {
            for (int i = 0; i < nodes.size(); i++) {
                Map<String, Object> m = nodes.get(i);
                if (m.get("id") instanceof Number n && n.intValue() == node.getId()) {
                    nodes.set(i, nodeToMap(node));
                    writeJson(nodesFile, nodes);
                    return node.getId();
                }
            }
        }
        int newId = nodes.stream()
                .mapToInt(m -> m.get("id") instanceof Number n ? n.intValue() : 0)
                .max().orElse(0) + 1;
        node.setId(newId);
        nodes.add(nodeToMap(node));
        writeJson(nodesFile, nodes);
        return newId;
    }

    public List<Map<String, Object>> loadAllMindMapNodes() {
        if (!isOpen()) return Collections.emptyList();
        return readJsonList(new File(currentProjectPath, "mindmap_nodes.json"));
    }

    // === Collections (cross-project) ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadCollections() {
        File f = new File(COLLECTIONS_FILE);
        if (!f.exists()) return new ArrayList<>();
        List<Map<String, Object>> data = readJson(f, List.class);
        return data != null ? data : new ArrayList<>();
    }

    public void saveCollections(List<Map<String, Object>> collections) {
        writeJson(new File(COLLECTIONS_FILE), collections);
    }

    // === Conversion Utilities ===

    private Map<String, Object> ideaToMap(Idea idea) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", idea.getId());
        m.put("projectId", idea.getProjectId());
        m.put("title", idea.getTitle());
        m.put("content", idea.getContent());
        m.put("type", idea.getType().name());
        m.put("mood", idea.getMood().name());
        m.put("importance", idea.getImportance());
        m.put("privacy", idea.getPrivacy().name());
        m.put("createdAt", idea.getCreatedAt() != null ? idea.getCreatedAt().toString() : null);
        m.put("updatedAt", idea.getUpdatedAt() != null ? idea.getUpdatedAt().toString() : null);
        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            m.put("tags", idea.getTags().stream().map(this::tagToMap).collect(Collectors.toList()));
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private Idea mapToIdea(Map<String, Object> m) {
        Idea idea = new Idea();
        if (m.get("id") instanceof Number n) idea.setId(n.intValue());
        if (m.get("projectId") instanceof Number n) idea.setProjectId(n.intValue());
        idea.setTitle((String) m.get("title"));
        idea.setContent((String) m.get("content"));
        try {
            String type = (String) m.get("type");
            if (type != null) idea.setType(Idea.IdeaType.valueOf(type));
            String mood = (String) m.get("mood");
            if (mood != null) idea.setMood(Idea.Mood.valueOf(mood));
            String privacy = (String) m.get("privacy");
            if (privacy != null) idea.setPrivacy(Idea.PrivacyLevel.valueOf(privacy));
        } catch (Exception e) { /* ignore */ }
        if (m.get("importance") instanceof Number n) idea.setImportance(n.intValue());
        try {
            String ca = (String) m.get("createdAt");
            if (ca != null) idea.setCreatedAt(java.time.LocalDateTime.parse(ca));
            String ua = (String) m.get("updatedAt");
            if (ua != null) idea.setUpdatedAt(java.time.LocalDateTime.parse(ua));
        } catch (Exception e) { /* ignore */ }
        // Tags
        Object tagsObj = m.get("tags");
        if (tagsObj instanceof List<?> tagList) {
            List<Tag> tags = new ArrayList<>();
            for (Object t : tagList) {
                if (t instanceof Map<?, ?> tm) {
                    tags.add(mapToTag((Map<String, Object>) tm));
                }
            }
            idea.setTags(tags);
        }
        return idea;
    }

    private Map<String, Object> tagToMap(Tag tag) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tag.getId());
        m.put("name", tag.getName());
        m.put("color", tag.getColor());
        m.put("description", tag.getDescription());
        m.put("usageCount", tag.getUsageCount());
        return m;
    }

    private Tag mapToTag(Map<String, Object> m) {
        Tag tag = new Tag();
        if (m.get("id") instanceof Number n) tag.setId(n.intValue());
        tag.setName((String) m.get("name"));
        tag.setColor((String) m.get("color"));
        tag.setDescription((String) m.get("description"));
        if (m.get("usageCount") instanceof Number n) tag.setUsageCount(n.intValue());
        return tag;
    }

    private Map<String, Object> nodeToMap(MindMapNode node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", node.getId());
        m.put("projectId", node.getProjectId());
        m.put("ideaId", node.getIdeaId());
        m.put("nodeType", node.getNodeType().name());
        m.put("text", node.getText());
        m.put("description", node.getDescription());
        m.put("x", node.getX());
        m.put("y", node.getY());
        m.put("width", node.getWidth());
        m.put("height", node.getHeight());
        m.put("color", node.getColor());
        m.put("shape", node.getShape().name());
        m.put("fontSize", node.getFontSize());
        m.put("fontWeight", node.getFontWeight().name());
        m.put("isRoot", node.isRoot());
        m.put("isExpanded", node.isExpanded());
        return m;
    }

    // === File I/O ===

    private void writeJson(File file, Object data) {
        try {
            mapper.writeValue(file, data);
        } catch (IOException e) {
            logger.error("Failed to write {}", file, e);
        }
    }

    private <T> T readJson(File file, Class<T> clazz) {
        if (!file.exists()) return null;
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            logger.error("Failed to read {}", file, e);
            return null;
        }
    }

    private List<Map<String, Object>> readJsonList(File file) {
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            logger.error("Failed to read list from {}", file, e);
            return new ArrayList<>();
        }
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff\\-_]", "_").trim();
    }
}
