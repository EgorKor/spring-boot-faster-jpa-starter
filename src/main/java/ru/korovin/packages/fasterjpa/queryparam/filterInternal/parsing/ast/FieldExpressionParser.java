package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.ast;

import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterToken;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterTokenType;

import java.util.ArrayList;
import java.util.List;

public class FieldExpressionParser {
    private final List<FilterToken> tokens;
    private int current;

    public FieldExpressionParser(List<FilterToken> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    public ASTNode parse() {
        try {
            ASTNode expression = parseExpression();
            if (!isAtEnd()) {
                throw new ParseException("Unexpected tokens after expression", current());
            }
            return expression;
        } catch (ParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage());
        }
    }

    private ASTNode parseExpression() {
        return parseFunctionOrField();
    }

    private ASTNode parseFunctionOrField() {
        // Если это функция
        if (check(FilterTokenType.FUNCTION_NAME)) {
            return parseFunctionCall();
        }

        // Если это поле
        if (check(FilterTokenType.IDENTIFIER)) {
            return parseFieldPath();
        }

        // Если это литерал
        if (check(FilterTokenType.STRING_LITERAL)) {
            return parseStringLiteral();
        }

        if (check(FilterTokenType.NUMBER)) {
            return parseNumberLiteral();
        }

        // Если выражение в скобках
        if (match(FilterTokenType.PUNCTUATION, "(")) {
            ASTNode expression = parseExpression();
            consume(FilterTokenType.PUNCTUATION, ")");
            return expression;
        }

        throw new ParseException("Expected function, field or literal", current());
    }

    private FunctionCall parseFunctionCall() {
        String functionName = consume(FilterTokenType.FUNCTION_NAME).value();
        consume(FilterTokenType.PUNCTUATION, "(");

        List<ASTNode> arguments = parseArguments();

        consume(FilterTokenType.PUNCTUATION, ")");

        return new FunctionCall(functionName, arguments);
    }

    private FieldPath parseFieldPath() {
        String fieldPath = consume(FilterTokenType.IDENTIFIER).value();
        return new FieldPath(fieldPath);
    }

    private StringLiteral parseStringLiteral() {
        String value = consume(FilterTokenType.STRING_LITERAL).value();
        return new StringLiteral(value);
    }

    private NumberLiteral parseNumberLiteral() {
        String numberStr = consume(FilterTokenType.NUMBER).value();
        if(numberStr.contains(".")){
            return new NumberLiteral(Double.parseDouble(numberStr));
        }else{
            return new NumberLiteral(Integer.parseInt(numberStr));
        }
    }

    private List<ASTNode> parseArguments() {
        List<ASTNode> arguments = new ArrayList<>();

        if (!check(FilterTokenType.PUNCTUATION, ")")) {
            do {
                arguments.add(parseExpression());
            } while (match(FilterTokenType.PUNCTUATION, ","));
        }

        return arguments;
    }

    // Вспомогательные методы
    private boolean match(FilterTokenType type, String... values) {
        if (!check(type, values)) return false;
        advance();
        return true;
    }

    private boolean check(FilterTokenType type, String... values) {
        if (isAtEnd()) return false;
        if (current().type() != type) return false;

        if (values.length > 0) {
            String currentValue = current().value();
            for (String value : values) {
                if (value.equals(currentValue)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private FilterToken consume(FilterTokenType type, String message) {
        if (check(type)) return advance();
        throw new ParseException(message, current());
    }

    private FilterToken consume(FilterTokenType type, String... values) {
        if (check(type, values)) return advance();
        throw new ParseException("Expected " + String.join(" or ", values), current());
    }

    private FilterToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private FilterToken previous() {
        return tokens.get(current - 1);
    }

    private FilterToken current() {
        return tokens.get(current);
    }

    private boolean isAtEnd() {
        return current >= tokens.size();
    }

    private static class ParseException extends RuntimeException {
        public ParseException(String message, FilterToken token) {
            super(message + " at position " + token.position());
        }
    }
}
