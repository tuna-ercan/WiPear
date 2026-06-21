package com.example.wipear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory store of items the user swiped left (marked for deletion).
 * Holds photos and/or PDFs, keyed by URI so the two never collide.
 * Shared between MainActivity and TrashActivity for the lifetime of the process.
 */
public final class TrashStore {

    private static final TrashStore INSTANCE = new TrashStore();

    private final Map<String, PhotoItem> marked = new LinkedHashMap<>();

    private TrashStore() {}

    public static TrashStore get() {
        return INSTANCE;
    }

    public void add(PhotoItem item) {
        marked.put(item.key(), item);
    }

    public void remove(String key) {
        marked.remove(key);
    }

    public boolean contains(String key) {
        return marked.containsKey(key);
    }

    public void clear() {
        marked.clear();
    }

    public int size() {
        return marked.size();
    }

    public List<PhotoItem> items() {
        return new ArrayList<>(marked.values());
    }
}
