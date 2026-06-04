package com.example.wipear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory store of photos the user swiped left (marked for deletion).
 * Shared between MainActivity and TrashActivity for the lifetime of the process.
 */
public final class TrashStore {

    private static final TrashStore INSTANCE = new TrashStore();

    private final Map<Long, PhotoItem> marked = new LinkedHashMap<>();

    private TrashStore() {}

    public static TrashStore get() {
        return INSTANCE;
    }

    public void add(PhotoItem item) {
        marked.put(item.id, item);
    }

    public void remove(long id) {
        marked.remove(id);
    }

    public boolean contains(long id) {
        return marked.containsKey(id);
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
