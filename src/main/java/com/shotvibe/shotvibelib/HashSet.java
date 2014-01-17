package com.shotvibe.shotvibelib;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class HashSet<E> implements Set<E> {
    public HashSet() {
        mSet = new java.util.HashSet<E>();
    }

    public HashSet(Collection<? extends E> c) {
        mSet = new java.util.HashSet<E>(c);
    }

    public HashSet(int initialCapacity) {
        mSet = new java.util.HashSet<E>(initialCapacity);
    }

    @Override
    public boolean add(E object) {
        return mSet.add(object);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return mSet.addAll(collection);
    }

    @Override
    public void clear() {
        mSet.clear();
    }

    @Override
    public boolean contains(Object object) {
        return mSet.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return mSet.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return mSet.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return mSet.iterator();
    }

    @Override
    public boolean remove(Object object) {
        return mSet.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return mSet.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return mSet.retainAll(collection);
    }

    @Override
    public int size() {
        return mSet.size();
    }

    @Override
    public Object[] toArray() {
        return mSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return mSet.toArray(array);
    }

    private java.util.HashSet<E> mSet;
}
