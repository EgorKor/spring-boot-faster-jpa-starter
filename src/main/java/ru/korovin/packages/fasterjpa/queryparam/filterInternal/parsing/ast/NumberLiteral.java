package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

public class NumberLiteral implements ASTNode {
    public final Number value;

    public NumberLiteral(Number value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
