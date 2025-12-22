package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Concat {

    public static String sql(String... elements) {
        return String.format("concat(%s)", String.join(",", elements));
    }

}
