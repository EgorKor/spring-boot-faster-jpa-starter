package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Lpad {
    public static String sql(String property, String value, String filler){
        return String.format("lpad(%s,%s,%s)", property, value, filler);
    }
}
