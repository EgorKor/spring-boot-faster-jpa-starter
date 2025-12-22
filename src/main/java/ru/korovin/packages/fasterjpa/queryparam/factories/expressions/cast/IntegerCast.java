package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.cast;

public class IntegerCast {
    public static String sql(String property){
        return String.format("cast(%s, 'integer')", property);
    }
}
