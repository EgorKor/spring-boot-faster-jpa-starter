package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Position {
    public static String sql(String property, String value){
        return String.format("position(%s,%s)", property, value);
    }
}
