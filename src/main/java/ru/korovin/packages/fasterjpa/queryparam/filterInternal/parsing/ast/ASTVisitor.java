package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

public interface ASTVisitor<T> {
    T visit(FunctionCall node);
    T visit(FieldPath node);
    T visit(NumberLiteral node);
    T visit(StringLiteral node);;
}
