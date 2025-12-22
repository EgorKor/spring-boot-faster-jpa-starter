package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Repeat {
    public static String sql(String property, String n){
        return String.format("repeat(%s,%s)", property, n);
    }
}
