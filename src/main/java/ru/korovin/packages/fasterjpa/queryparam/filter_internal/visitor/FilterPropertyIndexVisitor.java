package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.*;

public class FilterPropertyIndexVisitor implements FilterConditionTreeNodeVisitor<Map<String, Set<FilterCondition>>> {
    @Override
    public Map<String, Set<FilterCondition>> visit(FilterCondition condition) {
        return Map.of(
                condition.property(),
                new LinkedHashSet<>(Set.of(condition))
        );
    }

    @Override
    public Map<String, Set<FilterCondition>> visit(FilterNotCondition condition) {
        return visitNode(condition.getNode());
    }

    @Override
    public Map<String, Set<FilterCondition>> visit(FilterOrCondition condition) {
        return visitNodeList(condition.getNodes());
    }

    @Override
    public Map<String, Set<FilterCondition>> visit(FilterAndCondition condition) {
        return visitNodeList(condition.getNodes());
    }

    @Override
    public Map<String, Set<FilterCondition>> visit(FilterEmptyCondition emptyCondition) {
        return Map.of();
    }

    private Map<String, Set<FilterCondition>> visitNodeList(List<FilterConditionTreeNode> nodes) {
        Map<String, Set<FilterCondition>> result = new HashMap<>();
        for (var node : nodes) {
            Map<String, Set<FilterCondition>> nodeResult = visitNode(node);
            nodeResult.forEach((key, value) -> {
                if (result.containsKey(key)) {
                    result.get(key).addAll(value);
                } else {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

}
