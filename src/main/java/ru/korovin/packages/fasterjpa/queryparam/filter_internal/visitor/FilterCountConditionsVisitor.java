package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.List;

public class FilterCountConditionsVisitor implements FilterConditionTreeNodeVisitor<Integer> {

    @Override
    public Integer visit(FilterCondition condition) {
        return 1;
    }

    @Override
    public Integer visit(FilterNotCondition condition) {
        return visitNode(condition);
    }

    @Override
    public Integer visit(FilterOrCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public Integer visit(FilterAndCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public Integer visit(FilterEmptyCondition emptyCondition) {
        return 0;
    }

    private Integer visitNodes(List<FilterConditionTreeNode> nodes) {
        return nodes.stream()
                .mapToInt(this::visitNode)
                .sum();
    }
}
