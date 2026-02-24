package ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast;

public interface ASTVisitor<T> {
    T visit(FunctionCall node);
    T visit(FieldPath node);
    T visit(NumberLiteral node);
    T visit(StringLiteral node);;
}
