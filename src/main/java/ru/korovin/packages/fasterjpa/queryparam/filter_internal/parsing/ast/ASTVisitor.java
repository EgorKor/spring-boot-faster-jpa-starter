package ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast;

import ru.korovin.packages.fasterjpa.queryparam.Filter;

public interface ASTVisitor<T> {
    T visit(FunctionCall node);
    T visit(FieldPath node);
    T visit(NumberLiteral node);
    T visit(StringLiteral node);;
}
