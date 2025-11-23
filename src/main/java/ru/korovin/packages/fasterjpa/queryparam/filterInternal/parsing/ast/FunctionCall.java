package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

import java.util.List;

public class FunctionCall implements ASTNode {
    public final String functionName;
    public final List<ASTNode> arguments;

    public FunctionCall(String functionName, List<ASTNode> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
