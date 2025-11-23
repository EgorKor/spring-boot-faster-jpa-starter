package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

public class FieldPath implements ASTNode {
    public final String path; // "activityKind.area.code"

    public FieldPath(String path) {
        this.path = path;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
