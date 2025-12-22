package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Round {
    public static String sql(String property){
        return String.format("round(%s)", property);
    }

    public static String sql(String property, String n){
        return String.format("round(%s,%s)", property, n);
    }
}
