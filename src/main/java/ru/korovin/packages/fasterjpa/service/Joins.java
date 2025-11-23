package ru.korovin.packages.fasterjpa.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public record Joins(Set<String> properties) {

    public static Joins of(String... fetchingProperties) {
        return new Joins(new HashSet<>(Arrays.asList(fetchingProperties)));
    }

    public static Joins of(Collection<String> fetchingProperties) {
        return new Joins(new HashSet<>(fetchingProperties));
    }

    public static Joins empty(){
        return new Joins(new HashSet<>());
    }

}
