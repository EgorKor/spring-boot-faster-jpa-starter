package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Length {
    public static String sql(String property){
        return String.format("length(%s)", property);
    }
}
