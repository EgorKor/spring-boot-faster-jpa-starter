package ru.korovin.packages.fasterjpa.queryparam.factories;

import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterBuilder;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.Is;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterConditionTreeNode;

import java.util.Collection;

public class Filters {
    public final static FilterBuilder fb = new FilterBuilder();

    public static <T> Filter<T> of(FilterConditionTreeNode condition){
        return new Filter<>();
    }

    public static <T> Filter<T> equal(String field, Object value) {
        return fb.equals(field, value).toFilter();
    }

    public static <T> Filter<T> notEqual(String field, Object value) {
        return fb.notEquals(field, value).toFilter();
    }

    public static <T> Filter<T> contains(String field, String value) {
        return fb.contains(field, value).toFilter();
    }

    public static <T> Filter<T> notContains(String field, String value) {
        return fb.notContains(field, value).toFilter();
    }

    public static <T> Filter<T> like(String field, String value) {
        return fb.like(field, value).toFilter();
    }

    public static <T> Filter<T> notLike(String field, String value) {
        return fb.notLike(field, value).toFilter();
    }

    public static <T> Filter<T> empty() {
        return Filter.empty();
    }

    public static <T> Filter<T> isNull(String field) {
        return fb.is(field, Is.NULL).toFilter();
    }

    public static <T> Filter<T> isNotNull(String field) {
        return fb.is(field, Is.NOT_NULL).toFilter();
    }

    public static <T> Filter<T> isTrue(String field) {
        return fb.is(field, Is.TRUE).toFilter();
    }

    public static <T> Filter<T> isFalse(String field) {
        return fb.is(field, Is.FALSE).toFilter();
    }

    public static <T> Filter<T> in(String field, Object... values) {
        return fb.in(field, values).toFilter();
    }

    public static <T> Filter<T> inCollection(String field, Collection<?> values) {
        return fb.inCollection(field, values).toFilter();
    }

    public static <T> Filter<T> in(String field, Collection<Object> values) {
        return fb.in(field, values).toFilter();
    }

    public static <T> Filter<T> notIn(String field, Object... values) {
        return fb.notIn(field, values).toFilter();
    }

    public static <T> Filter<T> notInCollection(String field, Collection<?> values) {
        return fb.notInCollection(field, values).toFilter();
    }

    public static <T> Filter<T> greaterThan(String field, Comparable<?> value) {
        return fb.greater(field, value).toFilter();
    }

    public static <T> Filter<T> greaterThanOrEqual(String field, Comparable<?> value) {
        return fb.greaterOrEquals(field, value).toFilter();
    }

    public static <T> Filter<T> lessThan(String field, Comparable<?> value) {
        return fb.less(field, value).toFilter();
    }

    public static <T> Filter<T> lessThanOrEqual(String field, Comparable<?> value) {
        return fb.lessOrEquals(field, value).toFilter();
    }

    public static <T> Filter<T> equalsIgnoreCase(String field, String value) {
        return fb.equalsIgnoreCase(field, value).toFilter();
    }

}
