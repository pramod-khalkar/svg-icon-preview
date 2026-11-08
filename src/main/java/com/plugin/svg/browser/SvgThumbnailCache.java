package com.plugin.svg.browser;

import javax.swing.ImageIcon;
import java.util.LinkedHashMap;
import java.util.Map;

final class SvgThumbnailCache {
    private static final int DEFAULT_MAX_ENTRIES = 200;

    private final int maxEntries;
    private final Map<String, Entry> entries;
    private final java.util.Set<String> loading = new java.util.HashSet<>();

    SvgThumbnailCache() {
        this(DEFAULT_MAX_ENTRIES);
    }

    SvgThumbnailCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > SvgThumbnailCache.this.maxEntries;
            }
        };
    }

    synchronized ImageIcon getThumbnail(String key) {
        Entry entry = entries.get(key);
        return entry == null ? null : entry.thumbnail;
    }

    synchronized String getDimensions(String key) {
        Entry entry = entries.get(key);
        return entry == null ? null : entry.dimensions;
    }

    synchronized boolean beginLoading(String key) {
        return !entries.containsKey(key) && loading.add(key);
    }

    synchronized void complete(String key, ImageIcon thumbnail, String dimensions) {
        loading.remove(key);
        entries.put(key, new Entry(thumbnail, dimensions));
    }

    synchronized void clear() {
        entries.clear();
        loading.clear();
    }

    private static final class Entry {
        private final ImageIcon thumbnail;
        private final String dimensions;

        private Entry(ImageIcon thumbnail, String dimensions) {
            this.thumbnail = thumbnail;
            this.dimensions = dimensions;
        }
    }
}
