package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class Month {
    public static String sql(String property){
        return String.format("month(%s)", property);
    }
}
