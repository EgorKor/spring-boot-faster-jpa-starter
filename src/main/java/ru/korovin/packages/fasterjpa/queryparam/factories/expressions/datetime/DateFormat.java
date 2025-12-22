package ru.korovin.packages.fasterjpa.queryparam.factories.expressions.datetime;

public class DateFormat {
    public static String sql(String property, String format){
        return String.format("date_format(%s, %s)", property, format);
    }
}
