package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.ArrayList;
import java.util.List;

public class FilterListConditionsVisitor implements FilterConditionTreeNodeVisitor<List<FilterCondition>> {

    @Override
    public List<FilterCondition> visit(FilterCondition condition) {
        return new ArrayList<>(
                List.of(condition)
        );
    }

    @Override
    public List<FilterCondition> visit(FilterNotCondition condition) {
        return new ArrayList<>(visitNode(condition));
    }

    @Override
    public List<FilterCondition> visit(FilterOrCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public List<FilterCondition> visit(FilterAndCondition condition) {
        return visitNodes(condition.getNodes());
    }

    @Override
    public List<FilterCondition> visit(FilterEmptyCondition emptyCondition) {
        return new ArrayList<>();
    }

    public List<FilterCondition> visitNodes(List<FilterConditionTreeNode> nodes){
        List<FilterCondition> result = new ArrayList<>();
        nodes.forEach(node -> {
            result.addAll(visitNode(node));
        });
        return result;
    }


}
