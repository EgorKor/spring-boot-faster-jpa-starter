package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class Year {
    public static String sql(String property){
        return String.format("year(%s)", property);
    }
}
