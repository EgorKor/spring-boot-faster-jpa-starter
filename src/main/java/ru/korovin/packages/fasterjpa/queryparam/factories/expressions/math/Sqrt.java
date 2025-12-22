package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Sqrt {
    public static String sql(String property){
        return String.format("sqrt(%s)", property);
    }
}
