package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Upper {
    public static String sql(String property) {
        return String.format("upper(%s)", property);
    }
}
