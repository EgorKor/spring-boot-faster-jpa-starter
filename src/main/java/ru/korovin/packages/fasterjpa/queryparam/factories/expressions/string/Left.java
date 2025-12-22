package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Left {
    public static String sql(String property, String value){
        return String.format("left(%s,%s)", property, value);
    }
}
