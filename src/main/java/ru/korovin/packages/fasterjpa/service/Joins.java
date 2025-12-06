package ru.korovin.packages.fasterjpa.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public record Joins(Set<String> properties) {

    public static Joins of(String... fetchingProperties) {
        return new Joins(new HashSet<>(Arrays.asList(fetchingProperties)));
    }

    public static Joins of(Collection<String> fetchingProperties) {
        return new Joins(new HashSet<>(fetchingProperties));
    }

    public static Joins of(Joins... joins) {
        return new Joins(Arrays.stream(joins)
                .flatMap(join -> join.properties().stream())
                .collect(Collectors.toSet()));
    }

    public static Joins empty() {
        return new Joins(new HashSet<>());
    }

    public Joins with(Joins joins) {
        this.properties.addAll(joins.properties);
        return this;
    }


    public Joins withPropertyPrefix(String prefix) {
        return new Joins(properties.stream()
                .map(s -> String.format("%s.%s", prefix, s))
                .collect(Collectors.toSet()));
    }

    public static void main(String[] args) {
        String profileProperty = "profile";
        String departmentProperty = "structureDepartment";

        Joins rootJoins = Joins.of(profileProperty, departmentProperty);
        Joins profileJoins = Joins.of(profileProperty).withPropertyPrefix(profileProperty);
        Joins departmentJoins = Joins.of(departmentProperty).withPropertyPrefix(departmentProperty);


        Joins resultJoins = Joins.of(rootJoins, profileJoins, departmentJoins);
    }

}
