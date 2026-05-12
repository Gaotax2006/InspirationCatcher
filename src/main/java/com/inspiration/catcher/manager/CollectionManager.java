package com.inspiration.catcher.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.inspiration.catcher.model.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages collections (cross-project idea groupings) with JSON file persistence.
 */
public class CollectionManager {
    private static final Logger logger = LoggerFactory.getLogger(CollectionManager.class);
    private static final String COLLECTIONS_FILE = System.getProperty("user.home")
        + "/.inspiration-catcher/collections.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Collection> collections = new ArrayList<>();

    public CollectionManager() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    public List<Collection> getAll() { return collections; }

    public Collection getById(int id) {
        return collections.stream().filter(c -> c.getId() != null && c.getId() == id).findFirst().orElse(null);
    }

    public Collection getByName(String name) {
        return collections.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }

    public Collection create(String name, String description, String color) {
        Collection c = new Collection(name);
        c.setDescription(description != null ? description : "");
        c.setColor(color != null ? color : "#C4843C");
        c.setId(collections.size() + 1);
        collections.add(c);
        save();
        logger.info("Created collection: {}", name);
        return c;
    }

    public boolean delete(int id) {
        boolean removed = collections.removeIf(c -> c.getId() != null && c.getId() == id);
        if (removed) save();
        return removed;
    }

    public void addIdeaToCollection(int collectionId, int ideaId) {
        Collection c = getById(collectionId);
        if (c != null) {
            c.addIdeaId(ideaId);
            save();
        }
    }

    public void removeIdeaFromCollection(int collectionId, int ideaId) {
        Collection c = getById(collectionId);
        if (c != null) {
            c.removeIdeaId(ideaId);
            save();
        }
    }

    public List<Collection> getCollectionsForIdea(int ideaId) {
        return collections.stream().filter(c -> c.contains(ideaId)).toList();
    }

    /** Load collections from JSON file. */
    public void load() {
        File file = new File(COLLECTIONS_FILE);
        if (!file.exists()) {
            // Create parent directory
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            // Create default collections in memory only
            Collection c1 = new Collection("收藏");
            c1.setId(1);
            collections.add(c1);
            Collection c2 = new Collection("稍后阅读");
            c2.setId(2);
            collections.add(c2);
            trySave();
            logger.info("Created default collections");
            return;
        }
        try {
            List<Collection> loaded = mapper.readValue(file, new TypeReference<List<Collection>>() {});
            collections.clear();
            collections.addAll(loaded);
            logger.info("Loaded {} collections", collections.size());
        } catch (IOException e) {
            logger.warn("Failed to load collections, using defaults", e);
        }
    }

    /** Try to save, logging but not throwing on failure. */
    public void save() {
        trySave();
    }

    private boolean trySave() {
        try {
            File parent = new File(COLLECTIONS_FILE).getParentFile();
            if (parent != null) parent.mkdirs();
            mapper.writeValue(new File(COLLECTIONS_FILE), collections);
            logger.info("Saved {} collections", collections.size());
            return true;
        } catch (IOException e) {
            logger.warn("Failed to save collections (non-fatal): {}", e.getMessage());
            return false;
        }
    }
}
