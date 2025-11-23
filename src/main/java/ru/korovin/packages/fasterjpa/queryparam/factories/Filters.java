package ru.korovin.packages.fasterjpa.queryparam.factories;

import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterBuilder;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.Is;

import java.util.Collection;

public class Filters {
    public final static FilterBuilder fb = new FilterBuilder();

    public static <T> Filter<T> equal(String field, Object value) {
        return fb.and(fb.equals(field, value));
    }

    public static <T> Filter<T> notEqual(String field, Object value) {
        return fb.and(fb.notEquals(field, value));
    }

    public static <T> Filter<T> contains(String field, String value) {
        return fb.and(fb.like(field, value));
    }

    public static <T> Filter<T> notContains(String field, String value) {
        return fb.and(fb.notLike(field, value));
    }

    public static <T> Filter<T> like(String field, String value) {
        return fb.and(fb.like(field, value));
    }

    public static <T> Filter<T> notLike(String field, String value) {
        return fb.and(fb.notLike(field, value));
    }


    public static <T> Filter<T> isNull(String field) {
        return fb.and(fb.is(field, Is.NULL));
    }

    public static <T> Filter<T> isNotNull(String field) {
        return fb.and(fb.is(field, Is.NOT_NULL));
    }

    public static <T> Filter<T> isTrue(String field) {
        return fb.and(fb.is(field, Is.TRUE));
    }

    public static <T> Filter<T> isFalse(String field) {
        return fb.and(fb.is(field, Is.FALSE));
    }

    public static <T> Filter<T> in(String field, Object... values) {
        return fb.and(fb.in(field, values));
    }

    public static <T> Filter<T> inCollection(String field, Collection<?> values) {
        return fb.and(fb.inCollection(field, values));
    }

    public static <T> Filter<T> in(String field, Collection<Object> values) {
        return fb.and(fb.in(field, values));
    }

    public static <T> Filter<T> notIn(String field, Object... values) {
        return fb.and(fb.notIn(field, values));
    }

    public static <T> Filter<T> notInCollection(String field, Collection<?> values) {
        return fb.and(fb.notInCollection(field, values));
    }

    public static <T> Filter<T> greaterThan(String field, Comparable<?> value) {
        return fb.and(fb.greater(field, value));
    }

    public static <T> Filter<T> greaterThanOrEqual(String field, Comparable<?> value) {
        return fb.and(fb.greaterOrEquals(field, value));
    }

    public static <T> Filter<T> lessThan(String field, Comparable<?> value) {
        return fb.and(fb.less(field, value));
    }

    public static <T> Filter<T> lessThanOrEqual(String field, Comparable<?> value) {
        return fb.and(fb.lessOrEquals(field, value));
    }

    public static <T> Filter<T> equalsIgnoreCase(String field, String value) {
        return fb.and(fb.equalsIgnoreCase(field, value));
    }

}
