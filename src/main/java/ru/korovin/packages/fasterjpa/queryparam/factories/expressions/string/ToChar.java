package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class ToChar {
    public static String sql(String property, String format) {
        return String.format("to_char(%s,%s)", property, format);
    }
}
