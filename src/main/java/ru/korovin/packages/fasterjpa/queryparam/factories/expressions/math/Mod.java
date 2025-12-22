package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Mod {
    public static String sql(String property, String value){
        return String.format("mod(%s,%s)", property, value);
    }
}
