package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Lower {
    public static String sql(String property) {
        return String.format("lower(%s)", property);
    }
}
