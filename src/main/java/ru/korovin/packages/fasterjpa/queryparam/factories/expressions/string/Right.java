package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.string;

public class Right {
    public static String sql(String property, String value){
        return String.format("right(%s,%s)", property, value);
    }
}
