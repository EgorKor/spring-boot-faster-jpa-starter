package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Replace {
    public static String sql(String property, String target, String replacement) {
        return String.format("replace(%s,%s,%s)", property, target, replacement);
    }
}
