package ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast;

import ru.korovin.packages.fasterjpa.queryparam.Filter;

public interface ASTNode {
    <T> T accept(ASTVisitor<T> visitor);
}
