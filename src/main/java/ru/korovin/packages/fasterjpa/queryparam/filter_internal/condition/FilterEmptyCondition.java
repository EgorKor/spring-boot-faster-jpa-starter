package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ru.korovin.packages.fasterjpa.queryparam.Filter;

public final class FilterEmptyCondition implements FilterConditionTreeNode {

    @Override
    public Predicate parsePredicate(Root<?> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder, Class<?> entityType) {
        return criteriaBuilder.equal(
                criteriaBuilder.literal(1),
                criteriaBuilder.literal(1)
        );
    }

    @Override
    public void setFilter(Filter<?> filter) {
        //do nothing
    }

    @Override
    public FilterConditionTreeNode copy() {
        return this;
    }
}
