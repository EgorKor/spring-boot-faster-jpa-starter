package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Rpad {
    public static String sql(String property, String value, String filler){
        return String.format("rpad(%s,%s,%s)", property, value, filler);
    }
}
