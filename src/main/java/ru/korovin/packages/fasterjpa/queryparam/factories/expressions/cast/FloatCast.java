package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.cast;

public class FloatCast {
    public static String sql(String property){
        return String.format("cast(%s, 'float')", property);
    }
}
