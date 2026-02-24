package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class FilterNotCondition implements FilterConditionTreeNode {
    private FilterConditionTreeNode node;

    @Override
    public Predicate parsePredicate(Root<?> root,
                                    CriteriaQuery<?> criteriaQuery,
                                    CriteriaBuilder criteriaBuilder,
                                    Class<?> entityType) {
        return criteriaBuilder.not(
                node.parsePredicate(root, criteriaQuery, criteriaBuilder, entityType)
        );
    }

    @Override
    public FilterConditionTreeNode copy() {
        return new FilterNotCondition(node.copy());
    }
}
