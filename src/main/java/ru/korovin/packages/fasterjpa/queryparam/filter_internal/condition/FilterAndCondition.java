package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import ru.korovin.packages.fasterjpa.queryparam.Filter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class FilterAndCondition implements FilterConditionTreeNode {
    private final List<FilterConditionTreeNode> nodes;

    public FilterAndCondition(List<FilterConditionTreeNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    @Override
    public Predicate parsePredicate(Root<?> root,
                                    CriteriaQuery<?> criteriaQuery,
                                    CriteriaBuilder criteriaBuilder,
                                    Class<?> entityType) {
        return criteriaBuilder.and(
                nodes.stream()
                        .map(n -> n.parsePredicate(root, criteriaQuery, criteriaBuilder, entityType))
                        .toArray(Predicate[]::new)
        );
    }

    @Override
    public void setFilter(Filter<?> filter) {
        this.nodes.forEach(node -> {
            node.setFilter(filter);
        });
    }

    @Override
    public FilterConditionTreeNode copy() {
        return new FilterAndCondition(
                nodes.stream()
                        .map(FilterConditionTreeNode::copy)
                        .toList()
        );
    }
}
