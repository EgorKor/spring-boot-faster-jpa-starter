package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.cast;

public class TextCast {
    public static String sql(String property){
        return String.format("cast(%s, 'text')", property);
    }
}
