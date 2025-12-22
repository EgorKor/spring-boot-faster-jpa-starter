package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Floor {
    public static String sql(String property) {
        return String.format("floor(%s)", property);
    }
}
