package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.math;

public class Ceil {
    public static String sql(String property){
        return String.format("ceil(%s)", property);
    }
}
