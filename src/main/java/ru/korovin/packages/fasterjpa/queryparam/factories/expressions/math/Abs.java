package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Abs {
    public static String sql(String property){
        return String.format("abs(%s)",property);
    }
}
