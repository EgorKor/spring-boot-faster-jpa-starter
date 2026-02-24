package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import lombok.AllArgsConstructor;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
public class FilterIterationVisitor implements FilterConditionTreeNodeVisitor<Void> {
    private Consumer<FilterCondition> action;

    @Override
    public Void visit(FilterCondition condition) {
        action.accept(condition);
        return null;
    }

    @Override
    public Void visit(FilterNotCondition condition) {
        visitNode(condition);
        return null;
    }

    @Override
    public Void visit(FilterOrCondition condition) {
        visitNodes(condition.getNodes());
        return null;
    }

    @Override
    public Void visit(FilterAndCondition condition) {
        visitNodes(condition.getNodes());
        return null;
    }

    @Override
    public Void visit(FilterEmptyCondition emptyCondition) {
        return null;
    }

    private void visitNodes(List<FilterConditionTreeNode> nodes){
        nodes.forEach(this::visitNode);
    }
}
