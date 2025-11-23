package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.visitor;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast.*;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CriteriaExpressionBuilder implements ASTVisitor<Expression<?>> {
    private final CriteriaBuilder cb;
    private final Root<?> root;

    // Паттерны из вашего текущего кода (предположительные)
    private static final Pattern TO_CHAR_FUNCTION_PATTERN =
            Pattern.compile("to_char\\(([^,]+),\\s*'([^']+)'\\)", Pattern.CASE_INSENSITIVE);

    public CriteriaExpressionBuilder(CriteriaBuilder cb, Root<?> root) {
        this.cb = cb;
        this.root = root;
    }

    @Override
    public Expression<?> visit(FunctionCall node) {
        List<Expression<?>> args = node.arguments.stream()
                .map(arg -> arg.accept(this))
                .collect(Collectors.toList());

        switch (node.functionName.toLowerCase()) {
            case "concat":
                return buildConcatExpression(args);

            case "to_char":
                // Сохраняем вашу логику для TO_CHAR
                if (args.size() != 2) {
                    throw new IllegalArgumentException("TO_CHAR requires exactly 2 arguments");
                }
                return cb.function(
                        "TO_CHAR",
                        String.class,
                        args.get(0),  // path expression
                        args.get(1)   // format literal
                );

            case "current_date":
                return cb.currentDate();

            case "current_timestamp":
                return cb.currentTimestamp();

            case "year":
                return cb.function("YEAR", Integer.class, args.getFirst());

            case "month":
                return cb.function("MONTH", Integer.class, args.getFirst());

            case "day":
                return cb.function("DAY", Integer.class, args.get(0));

            case "date_format":
                return cb.function("DATE_FORMAT", String.class, args.get(0), args.get(1));

            case "abs":
                return cb.abs((Expression<Number>) args.get(0));

            case "ceil":
            case "ceiling":
                return cb.ceiling((Expression<Number>) args.getFirst());

            case "floor":
                return cb.floor((Expression<Number>) args.getFirst());

            case "round":
                if (args.size() == 1) {
                    return cb.round((Expression<Number>) args.getFirst(), 0);
                } else {
                    return cb.round(getTypedExpression(args.getFirst(), Number.class),
                            ((NumberLiteral) node.arguments.get(1)).value.intValue());
                }

            case "mod":
                return cb.mod((Expression<Integer>) args.getFirst(),
                        ((NumberLiteral) node.arguments.get(1)).value.intValue());

            case "sqrt":
                return cb.sqrt(getTypedExpression(args.getFirst(), Number.class));

            case "left":
                return cb.substring((Expression<String>) args.getFirst(), 1,
                        convertToInteger(node.arguments.get(1)));

            case "right":
                String str = args.get(0).toString();
                int length = convertToInteger(node.arguments.get(1));
                // Реализация через substring(length - len, length)
                return cb.function("RIGHT", String.class, args.get(0), args.get(1));

            case "lpad":
                return cb.function("LPAD", String.class, args.get(0), args.get(1), args.get(2));

            case "rpad":
                return cb.function("RPAD", String.class, args.get(0), args.get(1), args.get(2));

            case "position":
            case "instr":
                return cb.locate(getTypedExpression(args.get(0), String.class),
                        getTypedExpression(args.get(1), String.class));

            case "repeat":
                return cb.function("REPEAT", String.class, args.get(0), args.get(1));

            case "cast":

                return buildCastExpression(args, node);

            case "nullif":
                return cb.nullif(args.get(0), args.get(1));

            case "coalesce":

                CriteriaBuilder.Coalesce<Object> coalesce = cb.coalesce();
                for (Expression<?> arg : args) {
                    coalesce = coalesce.value(arg);
                }
                return coalesce;

            case "lower":
                return cb.lower(getTypedExpression(args.getFirst(), String.class));

            case "upper":
                return cb.upper(getTypedExpression(args.getFirst(), String.class));

            case "trim":
                return cb.trim(getTypedExpression(args.getFirst(), String.class));

            case "substring":
                if (args.size() == 2) {
                    return cb.substring(
                            getTypedExpression(args.get(0), String.class),
                            getTypedExpression(args.get(1), Integer.class)
                    );
                } else {
                    return cb.substring(
                            getTypedExpression(args.get(0), String.class),
                            getTypedExpression(args.get(1), Integer.class),
                            getTypedExpression(args.get(2), Integer.class)
                    );
                }

            case "length":
                return cb.length(getTypedExpression(args.getFirst(), String.class));

            case "replace":
                return cb.function(
                        "REPLACE",
                        String.class,
                        args.get(0),
                        getTypedExpression(args.get(1), Number.class),
                        getTypedExpression(args.get(2), Number.class)
                );

            default:
                throw new UnsupportedOperationException("Unsupported function: " + node.functionName);
        }
    }

    private Expression<?> buildCastExpression(List<Expression<?>> args, FunctionCall node) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("Функция CAST требует 2 аргументов");
        }

        if (!(node.arguments.get(1) instanceof StringLiteral)) {
            throw new IllegalArgumentException("Второй аргумент функции CAST должен быть строковый литерал");
        }

        String targetType = ((StringLiteral) node.arguments.get(1)).value.toLowerCase();
        return switch (targetType) {
            case "string", "text" -> cb.toString(getTypedExpression(args.getFirst(), Character.class));
            case "integer", "int" -> cb.toInteger(getTypedExpression(args.getFirst(), Integer.class));
            case "long" -> cb.toLong(getTypedExpression(args.getFirst(), Long.class));
            case "double" -> cb.toDouble(getTypedExpression(args.getFirst(), Double.class));
            case "float" -> cb.toFloat(getTypedExpression(args.getFirst(), Float.class));
            default -> throw new IllegalArgumentException("Неизвестный тип для приведения");
        };
    }

    private int convertToInteger(ASTNode node) {
        return ((NumberLiteral) node).value.intValue();
    }

    public static <X> Expression<X> getTypedExpression(Expression<?> expression, Class<X> type) {
        return (Expression<X>) expression;
    }

    private Expression<String> buildConcatExpression(List<Expression<?>> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return cb.literal("");
        }

        Expression<String> result = null;

        for (Expression<?> expr : expressions) {
            if (result == null) {
                result = convertToString(cb, expr);
            } else {
                result = cb.concat(result, convertToString(cb, expr));
            }
        }

        return result;
    }

    /**
     * Конвертация выражения в строковое выражение (аналог вашего convertToString)
     */
    private Expression<String> convertToString(CriteriaBuilder cb, Expression<?> expr) {
        // Если выражение уже строковое, возвращаем как есть
        if (expr.getJavaType().equals(String.class)) {
            return (Expression<String>) expr;
        }

        // Для числовых и других типов используем TO_CHAR или CAST
        return cb.function("TO_CHAR", String.class, expr);
    }


    @Override
    public Expression<?> visit(FieldPath node) {
        return getNestedPath(root, node.path);
    }

    @Override
    public Expression<?> visit(StringLiteral node) {
        return cb.literal(node.value);
    }

    @Override
    public Expression<?> visit(NumberLiteral node) {
        return cb.literal(node.value);
    }

    /**
     * Ваш существующий метод для получения вложенных путей
     */
    private static <T> Expression<?> getNestedPath(Root<T> root, String fieldPath) {
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
