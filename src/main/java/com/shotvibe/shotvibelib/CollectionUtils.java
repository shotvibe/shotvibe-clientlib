package com.shotvibe.shotvibelib;

import java.util.Collections;

public final class CollectionUtils {
    public interface Comparator<T> {
        int compare(T lhs, T rhs);
    }

    public static <T> void sortArrayList(ArrayList<T> list, Comparator<T> comparator) {
        final Comparator<T> finalComparator = comparator;
        Collections.sort(list, new java.util.Comparator<T>() {
            @Override
            public int compare(T lhs, T rhs) {
                return finalComparator.compare(lhs, rhs);
            }
        });
    }

    private CollectionUtils() {
        // Not Used
    }
}
