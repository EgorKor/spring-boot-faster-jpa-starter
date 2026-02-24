package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.*;

public class FilterOperationIndexVisitor implements FilterConditionTreeNodeVisitor<Map<String, Set<FilterOperation>>> {
    @Override
    public Map<String, Set<FilterOperation>> visit(FilterCondition condition) {
        return Map.of(
                condition.property(),
                new LinkedHashSet<>(Set.of(condition.operation()))
        );
    }

    @Override
    public Map<String, Set<FilterOperation>> visit(FilterNotCondition condition) {
        return visitNode(condition.getNode());
    }

    @Override
    public Map<String, Set<FilterOperation>> visit(FilterOrCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public Map<String, Set<FilterOperation>> visit(FilterAndCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public Map<String, Set<FilterOperation>> visit(FilterEmptyCondition emptyCondition) {
        return Map.of();
    }

    private Map<String, Set<FilterOperation>> visitNodes(List<FilterConditionTreeNode> nodes){
        Map<String, Set<FilterOperation>> result = new HashMap<>();
        for(var node: nodes){
            Map<String, Set<FilterOperation>> nodeResult = visitNode(node);
            nodeResult.forEach((key, value) -> {
                if(result.containsKey(key)){
                    result.get(key).addAll(value);
                }else{
                    result.put(key, value);
                }
            });
        }
        return result;
    }
}
