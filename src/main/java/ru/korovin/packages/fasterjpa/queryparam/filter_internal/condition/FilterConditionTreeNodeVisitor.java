package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import java.util.List;

public interface FilterConditionTreeNodeVisitor<T> {
    T visit(FilterCondition condition);

    T visit(FilterNotCondition condition);

    T visit(FilterOrCondition condition);

    T visit(FilterAndCondition condition);

    T visit(FilterEmptyCondition emptyCondition);

    default T visitNode(FilterConditionTreeNode node) {
        return switch (node) {
            case FilterNotCondition notCondition -> visit(notCondition);
            case FilterAndCondition andCondition -> visit(andCondition);
            case FilterOrCondition orCondition -> visit(orCondition);
            case FilterCondition filterCondition -> visit(filterCondition);
            case FilterEmptyCondition emptyCondition -> visit(emptyCondition);
            case null -> throw new IllegalStateException();
        };
    }



}
