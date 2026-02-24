package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class FilterOrCondition implements FilterConditionTreeNode {
    private final List<FilterConditionTreeNode> nodes;

    public FilterOrCondition(List<FilterConditionTreeNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    @Override
    public Predicate parsePredicate(Root<?> root,
                                    CriteriaQuery<?> criteriaQuery,
                                    CriteriaBuilder criteriaBuilder,
                                    Class<?> entityType) {
        return criteriaBuilder.or(
                nodes.stream()
                        .map(n -> n.parsePredicate(root, criteriaQuery, criteriaBuilder, entityType))
                        .toArray(Predicate[]::new)
        );
    }
}
