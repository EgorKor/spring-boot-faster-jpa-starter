package ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast;

public interface ASTNode {
    <T> T accept(ASTVisitor<T> visitor);
}
