package com.plugin.svg;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) cache for rendered SVG images.
 * Prevents re-decoding and re-rendering the same SVG payload.
 */
public class SvgImageCache {
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final long MAX_CACHE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB

    private final Map<String, CacheEntry> cache;
    private long currentSizeBytes = 0;

    public SvgImageCache() {
        // LinkedHashMap with access-order (LRU) eviction
        this.cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_ENTRIES || currentSizeBytes > MAX_CACHE_SIZE_BYTES;
            }
        };
    }

    /**
     * Get cached image by payload hash.
     * Updates LRU order on access.
     */
    public BufferedImage get(String payloadHash) {
        CacheEntry entry = cache.get(payloadHash);
        if (entry != null) {
            entry.accessCount++;
            return entry.image;
        }
        return null;
    }

    /**
     * Put image in cache.
     * Auto-evicts oldest entries if cache limits exceeded.
     */
    public void put(String payloadHash, BufferedImage image) {
        if (image == null) return;

        // Remove old entry if exists
        CacheEntry oldEntry = cache.remove(payloadHash);
        if (oldEntry != null) {
            currentSizeBytes -= oldEntry.estimatedSizeBytes;
        }

        // Add new entry
        long estimatedSize = estimateImageSize(image);
        CacheEntry newEntry = new CacheEntry(image, estimatedSize);
        cache.put(payloadHash, newEntry);
        currentSizeBytes += estimatedSize;

        // LinkedHashMap.removeEldestEntry() will be called if limits exceeded
    }

    /**
     * Clear all cached entries and reset size.
     */
    public void clear() {
        cache.clear();
        currentSizeBytes = 0;
    }

    /**
     * Get cache statistics for debugging.
     */
    public String getStats() {
        return String.format(
            "Cache: %d entries, ~%.1f MB / %.1f MB max",
            cache.size(),
            currentSizeBytes / (1024.0 * 1024),
            MAX_CACHE_SIZE_BYTES / (1024.0 * 1024)
        );
    }

    /**
     * Estimate memory size of BufferedImage.
     * RGB typically 4 bytes per pixel (ARGB).
     */
    private long estimateImageSize(BufferedImage image) {
        if (image == null) return 0;
        return (long) image.getWidth() * image.getHeight() * 4;
    }

    /**
     * Cache entry with image and metadata.
     */
    private static class CacheEntry {
        final BufferedImage image;
        final long estimatedSizeBytes;
        int accessCount = 0;

        CacheEntry(BufferedImage image, long sizeBytes) {
            this.image = image;
            this.estimatedSizeBytes = sizeBytes;
        }
    }
}
