package com.fast.cqrs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collection utility methods.
 */
public final class CollectionUtil {

    private CollectionUtil() {}

    /**
     * Checks if collection is null or empty.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if map is null or empty.
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns empty list if null.
     */
    public static <T> List<T> nullToEmpty(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    /**
     * Returns empty set if null.
     */
    public static <T> Set<T> nullToEmpty(Set<T> set) {
        return set != null ? set : Collections.emptySet();
    }

    /**
     * Returns empty map if null.
     */
    public static <K, V> Map<K, V> nullToEmpty(Map<K, V> map) {
        return map != null ? map : Collections.emptyMap();
    }

    /**
     * Creates mutable list from elements.
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        List<T> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Creates mutable set from elements.
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        Set<T> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * Creates mutable map from key-value pairs.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    /**
     * Creates mutable map from key-value pairs.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Creates mutable map from key-value pairs.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * Converts list to map by key extractor.
     */
    public static <T, K> Map<K, T> toMap(List<T> list, Function<T, K> keyMapper) {
        if (isEmpty(list)) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }

    /**
     * Gets first element or null.
     */
    public static <T> T first(List<T> list) {
        return isEmpty(list) ? null : list.get(0);
    }

    /**
     * Gets last element or null.
     */
    public static <T> T last(List<T> list) {
        return isEmpty(list) ? null : list.get(list.size() - 1);
    }

    /**
     * Partitions list into chunks.
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (isEmpty(list)) return Collections.emptyList();
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
