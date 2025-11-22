package ru.samgtu.packages.webutils.queryparam.filterInternal;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterTokenizer {

    private static final Set<String> SUPPORTED_FUNCTIONS = new HashSet<>(Arrays.asList(
            "concat", "to_char", "coalesce", "lower", "upper", "trim",
            "substring", "cast", "length", "replace", "ltrim", "rtrim"
    ));

    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
            "(" +
                    "\\b(" + String.join("|", SUPPORTED_FUNCTIONS) + ")\\b|" +
                    "\\$?\\w+(?:\\.+[^(),.=+\\-*/<>]+)*|" + // Идентификаторы (включая с точками)
                    "'[^']*'|" +
                    "\\d+(?:\\.\\d*)?|" +
                    "[(),.=+\\-*/<>]|" +
                    ")", Pattern.CASE_INSENSITIVE
    );

    public FilterValidationResult validate(List<FilterToken> tokens) {
        List<String> errors = new ArrayList<>();

        if (tokens.isEmpty()) {
            return new FilterValidationResult(false, "Empty expression",
                    List.of("Expression is empty"), tokens);
        }

        // Проверяем базовый синтаксис
        validateParentheses(tokens, errors);
        validateFunctionCalls(tokens, errors);
        validateSyntaxStructure(tokens, errors);
        validateIdentifiers(tokens, errors);

        boolean isValid = errors.isEmpty();
        String message = isValid ? "Syntax is valid" : "Syntax errors found";

        return new FilterValidationResult(isValid, message, errors, tokens);
    }

    // Валидация скобок
    private void validateParentheses(List<FilterToken> tokens, List<String> errors) {
        Stack<FilterToken> stack = new Stack<>();

        for (FilterToken token : tokens) {
            if (token.value().equals("(")) {
                stack.push(token);
            } else if (token.value().equals(")")) {
                if (stack.isEmpty()) {
                    errors.add("Unmatched closing parenthesis at position " + token.position());
                } else {
                    stack.pop();
                }
            }
        }

        if (!stack.isEmpty()) {
            errors.add("Unmatched opening parenthesis at position " + stack.peek().position());
        }
    }

    // Валидация вызовов функций
    private void validateFunctionCalls(List<FilterToken> tokens, List<String> errors) {
        for (int i = 0; i < tokens.size(); i++) {
            FilterToken token = tokens.get(i);

            if (token.type() == FilterTokenType.FUNCTION_NAME && SUPPORTED_FUNCTIONS.contains(token.value().toLowerCase())) {
                // Проверяем, что после имени функции идет открывающая скобка
                if (i + 1 >= tokens.size() || !tokens.get(i + 1).value().equals("(")) {
                    errors.add("Function '" + token.value() + "' must be followed by parentheses at position " + token.position());
                }

                // Проверяем специфичные правила для функций
                validateSpecificFunction(token, i, tokens, errors);
            }
        }
    }

    // Валидация специфичных функций
    private void validateSpecificFunction(FilterToken functionToken,
                                          int index,
                                          List<FilterToken> tokens,
                                          List<String> errors) {
        String functionName = functionToken.value().toLowerCase();

        switch (functionName) {
            case "concat":
                validateConcatFunction(index, tokens, errors);
                break;
            case "to_char":
                validateToCharFunction(index, tokens, errors);
                break;
            case "substring":
                validateSubstringFunction(index, tokens, errors);
                break;
            case "coalesce":
                validateCoalesceFunction(index, tokens, errors);
                break;
        }
    }

    private void validateConcatFunction(int functionIndex, List<FilterToken> tokens, List<String> errors) {
        // CONCAT должен иметь хотя бы 2 аргумента
        int argCount = countArguments(functionIndex, tokens);
        if (argCount < 2) {
            errors.add("CONCAT function requires at least 2 arguments at position " + tokens.get(functionIndex).position());
        }
    }

    private void validateToCharFunction(int functionIndex, List<FilterToken> tokens, List<String> errors) {
        // TO_CHAR должен иметь 2 аргумента: значение и формат
        int argCount = countArguments(functionIndex, tokens);
        if (argCount != 2) {
            errors.add("TO_CHAR function requires exactly 2 arguments at position " + tokens.get(functionIndex).position());
        }
    }

    private void validateSubstringFunction(int functionIndex, List<FilterToken> tokens, List<String> errors) {
        // SUBSTRING должен иметь специфичный синтаксис
        boolean hasFromKeyword = false;
        for (int i = functionIndex; i < tokens.size(); i++) {
            if (tokens.get(i).value().equalsIgnoreCase("from")) {
                hasFromKeyword = true;
                break;
            }
            if (tokens.get(i).value().equals(")")) {
                break;
            }
        }

        if (!hasFromKeyword) {
            errors.add("SUBSTRING function requires 'FROM' keyword at position " + tokens.get(functionIndex).position());
        }
    }

    private void validateCoalesceFunction(int functionIndex, List<FilterToken> tokens, List<String> errors) {
        // COALESCE должен иметь хотя бы 1 аргумент
        int argCount = countArguments(functionIndex, tokens);
        if (argCount < 1) {
            errors.add("COALESCE function requires at least 1 argument at position " + tokens.get(functionIndex).position());
        }
    }

    // Подсчет аргументов функции
    private int countArguments(int functionIndex, List<FilterToken> tokens) {
        int argCount = 0;
        int parenDepth = 0;
        boolean inArgument = false;

        for (int i = functionIndex + 1; i < tokens.size(); i++) {
            FilterToken token = tokens.get(i);

            if (token.value().equals("(")) {
                parenDepth++;
            } else if (token.value().equals(")")) {
                parenDepth--;
                if (parenDepth < 0) break;
            }

            if (parenDepth == 1) {
                if (token.value().equals(",") && inArgument) {
                    argCount++;
                    inArgument = false;
                } else if (!token.value().equals("(") && !token.value().equals(",") && !inArgument) {
                    argCount++;
                    inArgument = true;
                }
            }

            if (parenDepth < 0) break;
        }

        return argCount;
    }

    // Общая валидация структуры
    private void validateSyntaxStructure(List<FilterToken> tokens, List<String> errors) {
        for (int i = 0; i < tokens.size(); i++) {
            FilterToken current = tokens.get(i);

            // Проверяем неизвестные токены
            if (current.type() == FilterTokenType.UNKNOWN) {
                errors.add("Unknown token '" + current.value() + "' at position " + current.position());
            }

            // Проверяем последовательности операторов
            if (i > 0) {
                FilterToken previous = tokens.get(i - 1);

                // Два оператора подряд
                if (current.type() == FilterTokenType.OPERATOR && previous.type() == FilterTokenType.OPERATOR) {
                    errors.add("Consecutive operators '" + previous.value() + "' and '" + current.value() + "' at position " + current.position());
                }

                // Запятая в начале или неправильном месте
                if (current.value().equals(",") &&
                        (i == 0 || previous.value().equals("(") || previous.value().equals(","))) {
                    errors.add("Misplaced comma at position " + current.position());
                }
            }
        }
    }

    // Валидация идентификаторов
    private void validateIdentifiers(List<FilterToken> tokens, List<String> errors) {
        for (FilterToken token : tokens) {
            if (token.type() == FilterTokenType.IDENTIFIER) {
                String identifier = token.value();

                // Проверяем составные идентификаторы (с точками)
                if (identifier.contains(".")) {
                    String[] parts = identifier.split("\\.");
                    for (String part : parts) {
                        if(part.isEmpty()){
                            errors.add("Invalid identifier commas: " + identifier);
                            continue;
                        }

                        if (!part.matches("\\w+")) {
                            errors.add("Invalid identifier part '"
                                    + part + "' in '"
                                    + identifier + "' at position "
                                    + token.position());
                        }
                    }
                }
            }
        }
    }


    public List<FilterToken> tokenize(String sqlExpression) {
        List<FilterToken> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERNS.matcher(sqlExpression);

        int lastPosition = 0;

        while (matcher.find()) {
            // Проверяем, нет ли пропущенных символов
            if (matcher.start() > lastPosition) {
                String skipped = sqlExpression.substring(lastPosition, matcher.start()).trim();
                if (!skipped.isEmpty()) {
                    tokens.add(new FilterToken(FilterTokenType.UNKNOWN, skipped, lastPosition));
                }
            }

            String tokenValue = matcher.group();
            if(!tokenValue.isEmpty()) {
                FilterTokenType type = determineFilterTokenType(tokenValue);
                tokens.add(new FilterToken(type, tokenValue, matcher.start()));
            }
            lastPosition = matcher.end();
        }

        // Проверяем хвост строки
        if (lastPosition < sqlExpression.length()) {
            String remaining = sqlExpression.substring(lastPosition).trim();
            if (!remaining.isEmpty()) {
                tokens.add(new FilterToken(FilterTokenType.UNKNOWN, remaining, lastPosition));
            }
        }

        return tokens;
    }

    private FilterTokenType determineFilterTokenType(String token) {
        // Функции
        if (token.matches("(?i)^(concat|to_char|coalesce|lower|upper|trim|substring|cast)$")) {
            return FilterTokenType.FUNCTION_NAME;
        }

        // SQL ключевые слова
        if (token.matches("(?i)^(like|in|is|null|not|and|or)$")) {
            return FilterTokenType.FUNCTION_NAME; // или можно создать отдельный тип
        }

        // Строковые литералы
        if (token.startsWith("'") && token.endsWith("'")) {
            return FilterTokenType.STRING_LITERAL;
        }

        // Числа
        if (token.matches("\\d+(?:\\.\\d*)?")) {
            return FilterTokenType.NUMBER;
        }

        // Пунктуация
        if (token.matches("[(),.]")) {
            return FilterTokenType.PUNCTUATION;
        }

        // Операторы
        if (token.matches("[=+\\-*/<>]")) {
            return FilterTokenType.OPERATOR;
        }

        // Идентификаторы (включая составные с точками)
        if (token.matches("\\$?\\w+(?:\\.\\w+)*")) {
            return FilterTokenType.IDENTIFIER;
        }

        return FilterTokenType.UNKNOWN;
    }
}
