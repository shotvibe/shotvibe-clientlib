package com.shotvibe.shotvibelib;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ArrayList<E> implements java.lang.Iterable<E>, java.util.Collection<E>, java.util.List<E>, java.util.RandomAccess {
    private java.util.ArrayList<E> mElems;

    public ArrayList() {
        mElems = new java.util.ArrayList<E>();
    }

    public ArrayList(int initialCapacity) {
        mElems = new java.util.ArrayList<E>(initialCapacity);
    }

    public ArrayList(Collection<? extends E> c) {
        mElems = new java.util.ArrayList<E>(c);
    }

    @Override
    public void add(int location, E object) {
        mElems.add(location, object);
    }

    @Override
    public boolean add(E object) {
        return mElems.add(object);
    }

    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        return mElems.addAll(location, collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return mElems.addAll(collection);
    }

    @Override
    public void clear() {
        mElems.clear();
    }

    @Override
    public boolean contains(Object object) {
        return mElems.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return mElems.containsAll(collection);
    }

    @Override
    public E get(int location) {
        return mElems.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return mElems.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return mElems.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return mElems.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return mElems.lastIndexOf(object);
    }

    @Override
    public ListIterator<E> listIterator() {
        return mElems.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int location) {
        return mElems.listIterator(location);
    }

    @Override
    public E remove(int location) {
        return mElems.remove(location);
    }

    @Override
    public boolean remove(Object object) {
        return mElems.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return mElems.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return mElems.retainAll(collection);
    }

    @Override
    public E set(int location, E object) {
        return mElems.set(location, object);
    }

    @Override
    public int size() {
        return mElems.size();
    }

    @Override
    public List<E> subList(int start, int end) {
        return mElems.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        return mElems.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return mElems.toArray(array);
    }
}
