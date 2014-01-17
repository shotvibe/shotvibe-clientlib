package com.shotvibe.shotvibelib;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class HashMap<K, V> implements Map<K, V> {
    public HashMap() {
        mMap = new java.util.HashMap<K, V>();
    }

    public HashMap(int capacity) {
        mMap = new java.util.HashMap<K, V>(capacity);
    }

    public HashMap(Map<? extends K, ? extends V> map) {
        mMap = new java.util.HashMap<K, V>(map);
    }

    @Override
    public void clear() {
        mMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return mMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mMap.containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return mMap.entrySet();
    }

    @Override
    public V get(Object key) {
        return mMap.get(key);
    }

    @Override
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return mMap.keySet();
    }

    @Override
    public V put(K key, V value) {
        return mMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        mMap.putAll(map);
    }

    @Override
    public V remove(Object key) {
        return mMap.remove(key);
    }

    @Override
    public int size() {
        return mMap.size();
    }

    @Override
    public Collection<V> values() {
        return mMap.values();
    }

    private java.util.HashMap<K, V> mMap;
}
