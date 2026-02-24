package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.SneakyThrows;
import ru.korovin.packages.fasterjpa.queryparam.Filter;

import java.util.List;

public sealed interface FilterConditionTreeNode permits FilterAndCondition, FilterCondition, FilterEmptyCondition, FilterNotCondition, FilterOrCondition {
    Predicate parsePredicate(Root<?> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder, Class<?> entityType);

    default <T> T visitWith(FilterConditionTreeNodeVisitor<T> visitor) {
        return switch (this) {
            case FilterAndCondition andCondition -> visitor.visit(andCondition);
            case FilterOrCondition orCondition -> visitor.visit(orCondition);
            case FilterNotCondition notCondition -> visitor.visit(notCondition);
            case FilterCondition filterCondition -> visitor.visit(filterCondition);
            case FilterEmptyCondition filterEmptyCondition -> visitor.visit(filterEmptyCondition);
            case null -> null;
        };
    }

    default FilterConditionTreeNode and(FilterConditionTreeNode node){
        return new FilterAndCondition(List.of(this, node));
    }

    default FilterConditionTreeNode or(FilterConditionTreeNode node){
        return new FilterOrCondition(List.of(this, node));
    }

    default FilterConditionTreeNode not(){
        return new FilterNotCondition(this);
    }

    default <T> Filter<T> toFilter() {
        return new Filter<>(this);
    }

    @SneakyThrows
    default <T extends Filter<?>> T toFilter(Class<T> entityType) {
        T filter = entityType.getDeclaredConstructor().newInstance();
        filter.setEntityType(entityType);
        filter.setFilterCondition(this);
        return filter;
    }
}
