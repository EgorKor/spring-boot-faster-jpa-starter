package ru.korovin.packages.fasterjpa.queryparam.filter_internal;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast.ASTNode;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.ast.FieldExpressionParser;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.tokenizing.FilterToken;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.tokenizing.FilterTokenizer;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.visitor.CriteriaExpressionBuilder;

import java.util.List;

public class FieldExpressionCompiler {
    public static Expression<?> compileToCriteria(
            String fieldExpression,
            Filter<?> filter,
            CriteriaBuilder cb,
            Root<?> root) {

        // Если это простое поле (не функция)
        if (!fieldExpression.toLowerCase().startsWith("concat") &&
                !fieldExpression.toLowerCase().contains("(")) {
            return fieldExpression.contains(".")
                    ? getNestedPath(root, fieldExpression)
                    : root.get(fieldExpression);
        }

        // Для сложных выражений используем парсер
        FilterTokenizer tokenizer = new FilterTokenizer();
        List<FilterToken> tokens = tokenizer.tokenize(fieldExpression);

        FilterValidationResult validation = tokenizer.validate(tokens);
        List<String> errors = validation.errors();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid expression: " +
                    String.join(", ", errors));
        }
        FieldExpressionParser parser = new FieldExpressionParser(tokens);
        ASTNode ast = parser.parse();

        CriteriaExpressionBuilder builder = new CriteriaExpressionBuilder(cb, root, filter);
        return ast.accept(builder);
    }

    // Ваш существующий метод
    public static <T> Expression<?> getNestedPath(Root<T> root, String fieldPath) {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath);
        }

        String[] parts = fieldPath.split("\\.");
        Path<?> path = root.get(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }

        return path;
    }
}
