package ru.korovin.packages.fasterjpa.util;

import lombok.Getter;

@Getter
public class Pair<T1,T2> {
    private final T1 first;
    private final T2 second;

    private Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public static <T1, T2> Pair of(T1 first, T2 second) {
        return new Pair<>(first, second);
    }

    public static <T1, T2> Pair<T1, T2> ofFirst(T1 first) {
        return new Pair<>(first, null);
    }

    public static <T1, T2> Pair<T1, T2> ofSecond(T2 second) {
        return new Pair<>(null, second);
    }

    public static <T1, T2> Pair<T1, T2> empty() {
        return new Pair<>(null, null);
    }


}
