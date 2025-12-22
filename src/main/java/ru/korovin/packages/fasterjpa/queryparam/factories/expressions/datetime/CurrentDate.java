package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class CurrentDate {
    public static String sql() {
        return "current_date()";
    }
}
