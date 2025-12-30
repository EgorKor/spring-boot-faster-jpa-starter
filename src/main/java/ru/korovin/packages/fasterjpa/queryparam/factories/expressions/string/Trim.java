package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Trim {
    public static String sql(String property) {
        return String.format("trim(%s)", property);
    }
}
