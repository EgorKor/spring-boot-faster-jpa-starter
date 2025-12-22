package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class CurrentTimestamp {
    public static String sql(){
        return "current_timestamp()";
    }
}
