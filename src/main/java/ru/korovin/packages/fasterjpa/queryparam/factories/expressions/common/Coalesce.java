package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.common;

public class Coalesce {
    public static String sql(String... args) {
        return String.format(
                "coalesce(%s)",
                String.join(",", args)
        );
    }
}
