package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class Day {
    public static String sql(String property){
        return String.format("day(%s)", property);
    }
}
