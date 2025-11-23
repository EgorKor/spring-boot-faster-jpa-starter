package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

public interface ASTNode {
    <T> T accept(ASTVisitor<T> visitor);
}
