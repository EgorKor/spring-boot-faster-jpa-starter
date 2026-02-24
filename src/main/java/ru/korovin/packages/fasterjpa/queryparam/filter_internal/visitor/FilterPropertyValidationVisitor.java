package ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor;

import lombok.AllArgsConstructor;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class FilterPropertyValidationVisitor implements FilterConditionTreeNodeVisitor<Void> {
    private Map<String, Set<FilterCondition>> conditionsWithNoMappedFields;
    private ParamCountLimit commonLimit;


    @Override
    public Void visit(FilterCondition condition) {
        return null;
    }

    @Override
    public Void visit(FilterNotCondition condition) {
        return null;
    }

    @Override
    public Void visit(FilterOrCondition condition) {
        return null;
    }

    @Override
    public Void visit(FilterAndCondition condition) {
        return null;
    }

    @Override
    public Void visit(FilterEmptyCondition emptyCondition) {
        return null;
    }

    public record FilterPropertyValidationResult(List<String> errors){
        boolean hasErrors(){
            return !errors.isEmpty();
        }
    }
}
