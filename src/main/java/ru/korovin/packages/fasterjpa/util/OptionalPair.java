package ru.korovin.packages.fasterjpa.util;

import lombok.Getter;

import java.util.Optional;

@Getter
public class OptionalPair<T1, T2> {
    private final Optional<T1> first;
    private final Optional<T2> second;

    private OptionalPair(T1 first, T2 second) {
        this.first = Optional.ofNullable(first);
        this.second = Optional.ofNullable(second);
    }

    public static <T1, T2> OptionalPair<T1, T2> of(T1 first, T2 second) {
        return new OptionalPair<>(first, second);
    }

    public static <T1, T2> OptionalPair<T1, T2> ofFirst(T1 first) {
        return new OptionalPair<>(first, null);
    }

    public static <T1, T2> OptionalPair<T1, T2> ofSecond(T2 second) {
        return new OptionalPair<>(null, second);
    }

    public static <T1, T2> OptionalPair<T1, T2> empty() {
        return new OptionalPair<>(null, null);
    }
}
