package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class FilterEmptyCondition implements FilterConditionTreeNode{

    @Override
    public Predicate parsePredicate(Root<?> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder, Class<?> entityType) {
        return criteriaBuilder.equal(
                criteriaBuilder.literal(1),
                criteriaBuilder.literal(1)
        );
    }
}
