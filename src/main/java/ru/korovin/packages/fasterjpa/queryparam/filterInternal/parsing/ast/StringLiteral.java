package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

public class StringLiteral implements ASTNode {
    public final String value;

    public StringLiteral(String value) {
        // Убираем обрамляющие кавычки
        this.value = value.substring(1, value.length() - 1);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
