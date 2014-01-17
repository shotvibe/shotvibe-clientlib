package com.shotvibe.shotvibelib;

import java.util.Collection;
import java.util.Iterator;

public class HashSet<E> {
    public HashSet() {
        mSet = new java.util.HashSet<E>();
    }

    public HashSet(Collection<? extends E> c) {
        mSet = new java.util.HashSet<E>(c);
    }

    public HashSet(int initialCapacity) {
        mSet = new java.util.HashSet<E>(initialCapacity);
    }

    public boolean add(E object) {
        return mSet.add(object);
    }

    public boolean addAll(Collection<? extends E> collection) {
        return mSet.addAll(collection);
    }

    public void clear() {
        mSet.clear();
    }

    public boolean contains(Object object) {
        return mSet.contains(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return mSet.containsAll(collection);
    }

    public boolean isEmpty() {
        return mSet.isEmpty();
    }

    /**
     * Must not be called directly. Only available for "for each" loop support.
     *
     * @return
     */
    public Iterator<E> iterator() {
        return mSet.iterator();
    }

    public boolean remove(Object object) {
        return mSet.remove(object);
    }

    public boolean removeAll(Collection<?> collection) {
        return mSet.removeAll(collection);
    }

    public boolean retainAll(Collection<?> collection) {
        return mSet.retainAll(collection);
    }

    public int size() {
        return mSet.size();
    }

    private java.util.HashSet<E> mSet;
}
