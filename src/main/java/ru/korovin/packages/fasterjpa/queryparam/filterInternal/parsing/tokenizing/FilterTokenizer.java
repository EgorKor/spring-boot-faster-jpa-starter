package ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing;

import ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterValidationResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterTokenizer {

    private static final Set<String> SUPPORTED_FUNCTIONS = new HashSet<>(Arrays.asList(
            "concat", "to_char", "coalesce", "lower", "upper", "trim",
            "substring", "cast", "length", "replace", "current_date",
            "current_timestamp", "year", "month", "day", "date_format",
            "abs", "ceil", "ceiling", "floor","round","mod","sqrt",
            "left","right","lpad","rpad","position","instr","repeat",
            "cast","nullif"
    ));

    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
            "(" +
                    "\\b(" + String.join("|", SUPPORTED_FUNCTIONS) + ")\\b|" + // поддерживаемые функции
                    "\\b(like|in|is|null|not|and|or)\\b|" + // ключевые слова
                    "'[^']*'|" + // строковые литералы
                    "\\d+(?:\\.\\d*)?|" + // числа
                    "\\$?\\w+(\\.\\w+)*|" + // корректные идентификаторы
                    "\\.{2,}|" + // множественные точки (ошибка)
                    "\\w*\\.\\.\\w*|" + // идентификаторы с двумя точками подряд
                    "[(),.=+\\-*/<>]" + // операторы и пунктуация
                    ")",
            Pattern.CASE_INSENSITIVE
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
        validateCommas(tokens, errors);

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
        Stack<String> functionStack = new Stack<>();
        Stack<Integer> parenStack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            FilterToken token = tokens.get(i);

            if (token.type() == FilterTokenType.FUNCTION_NAME &&
                    SUPPORTED_FUNCTIONS.contains(token.value().toLowerCase())) {
                functionStack.push(token.value().toLowerCase());

                // Проверяем, что после имени функции идет открывающая скобка
                if (i + 1 >= tokens.size() || !tokens.get(i + 1).value().equals("(")) {
                    errors.add("Function '" + token.value() + "' must be followed by parentheses at position " + token.position());
                }
            }

            if (token.value().equals("(")) {
                parenStack.push(i);
            } else if (token.value().equals(")")) {
                if (!parenStack.isEmpty()) {
                    int openParenIndex = parenStack.pop();
                    // Если после открывающей скобки была функция, проверяем ее аргументы
                    if (openParenIndex > 0 &&
                            tokens.get(openParenIndex - 1).type() == FilterTokenType.FUNCTION_NAME) {
                        String functionName = tokens.get(openParenIndex - 1).value().toLowerCase();
                        int argCount = countFunctionArguments(openParenIndex - 1, tokens);
                        validateFunctionArgumentCount(functionName, argCount, tokens.get(openParenIndex - 1), errors);
                    }
                }
            }
        }
    }

    private int countFunctionArguments(int functionIndex, List<FilterToken> tokens) {
        int argCount = 0;
        int parenDepth = 0;
        int startIndex = functionIndex + 2; // Пропускаем имя функции и открывающую скобку

        for (int i = startIndex; i < tokens.size(); i++) {
            FilterToken token = tokens.get(i);

            if (token.value().equals("(")) {
                parenDepth++;
            } else if (token.value().equals(")")) {
                if (parenDepth == 0) {
                    // Достигли закрывающей скобки функции
                    if (i > startIndex) {
                        FilterToken prevToken = tokens.get(i - 1);
                        if (!prevToken.value().equals("(")) {
                            argCount++;
                        }
                    }
                    break;
                }
                parenDepth--;
            }

            if (parenDepth == 0) {
                if (token.value().equals(",")) {
                    argCount++;
                }
            }
        }

        return Math.max(argCount, 0);
    }

    private void validateFunctionArgumentCount(String functionName, int argCount,
                                               FilterToken functionToken, List<String> errors) {
        switch (functionName.toLowerCase()) {
            case "concat":
                if (argCount < 2) {
                    errors.add("CONCAT function requires at least 2 arguments at position " + functionToken.position());
                }
                break;
            case "to_char":
                if (argCount != 2) {
                    errors.add("TO_CHAR function requires exactly 2 arguments at position " + functionToken.position());
                }
                break;
            case "substring":
                if (argCount < 2 || argCount > 3) {
                    errors.add("SUBSTRING function requires 2 or 3 arguments at position " + functionToken.position());
                }
                break;
            case "coalesce":
                if (argCount < 1) {
                    errors.add("COALESCE function requires at least 1 argument at position " + functionToken.position());
                }
                break;
            case "lower":
            case "upper":
            case "trim":
            case "length":
                if (argCount != 1) {
                    errors.add(functionName.toUpperCase() + " function requires exactly 1 argument at position " + functionToken.position());
                }
                break;
            case "replace":
                if (argCount != 3) {
                    errors.add("REPLACE function requires exactly 3 arguments at position " + functionToken.position());
                }
                break;
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
    // Подсчет аргументов функции (исправленная версия)
    private int countArguments(int functionIndex, List<FilterToken> tokens) {
        int argCount = 0;
        int parenDepth = 0;
        boolean inArgument = false;
        boolean foundOpeningParen = false;

        // Начинаем с токена после имени функции
        for (int i = functionIndex + 1; i < tokens.size(); i++) {
            FilterToken token = tokens.get(i);

            if (token.value().equals("(")) {
                parenDepth++;
                if (parenDepth == 1 && !foundOpeningParen) {
                    foundOpeningParen = true;
                    continue; // Пропускаем открывающую скобку самой функции
                }
            } else if (token.value().equals(")")) {
                parenDepth--;
                if (parenDepth < 0) {
                    // Закрывающая скобка функции
                    if (inArgument) {
                        argCount++;
                    }
                    break;
                }
            }

            // Мы на уровне аргументов функции (внутри ее скобок, но не внутри вложенных)
            if (parenDepth == 1) {
                if (token.value().equals(",")) {
                    argCount++;
                    inArgument = false;
                } else if (!token.value().equals("(") && !inArgument) {
                    inArgument = true;
                }
            } else if (parenDepth == 0 && foundOpeningParen) {
                // Вышли из скобок функции
                if (inArgument) {
                    argCount++;
                }
                break;
            }
        }

        // Обработка последнего аргумента
        if (inArgument) {
            argCount++;
        }

        return argCount;
    }

    private void validateCommas(List<FilterToken> tokens, List<String> errors) {
        for (int i = 0; i < tokens.size(); i++) {
            FilterToken current = tokens.get(i);

            if (!current.value().equals(",")) {
                continue;
            }

            FilterToken previous = i > 0 ? tokens.get(i - 1) : null;
            FilterToken next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

            // Запятая не может быть первым токеном
            if (i == 0) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может быть последним токеном
            if (i == tokens.size() - 1) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может идти сразу после открывающей скобки
            if (previous != null && previous.value().equals("(")) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может идти сразу после другой запятой
            if (previous != null && previous.value().equals(",")) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может идти после оператора
            if (previous != null && previous.type() == FilterTokenType.OPERATOR) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может идти перед закрывающей скобкой
            if (next != null && next.value().equals(")")) {
                errors.add("Misplaced comma at position " + current.position());
                continue;
            }

            // Запятая не может идти перед оператором
            if (next != null && next.type() == FilterTokenType.OPERATOR) {
                errors.add("Misplaced comma at position " + current.position());
            }
        }
    }

    // Общая валидация структуры
    private void validateSyntaxStructure(List<FilterToken> tokens, List<String> errors) {
        for (int i = 0; i < tokens.size(); i++) {
            FilterToken current = tokens.get(i);
            FilterToken previous = i > 0 ? tokens.get(i - 1) : null;
            FilterToken next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

            // Проверяем неизвестные токены
            if (current.type() == FilterTokenType.UNKNOWN) {
                errors.add("Unknown token '" + current.value() + "' at position " + current.position());
            }

            // Проверяем идентификаторы, за которыми идет скобка - это могут быть неподдерживаемые функции
            if (current.type() == FilterTokenType.IDENTIFIER &&
                    next != null && next.value().equals("(") &&
                    !SUPPORTED_FUNCTIONS.contains(current.value().toLowerCase())) {
                errors.add("Unsupported function '" + current.value() + "' at position " + current.position());
            }

            // Проверяем последовательности точек
            if (current.type() == FilterTokenType.PUNCTUATION && current.value().equals(".")) {
                if (previous == null || next == null) {
                    errors.add("Misplaced dot at position " + current.position());
                } else if (previous.type() != FilterTokenType.IDENTIFIER ||
                        next.type() != FilterTokenType.IDENTIFIER) {
                    errors.add("Misplaced dot at position " + current.position());
                }
            }

            if (current.type() == FilterTokenType.OPERATOR &&
                    previous != null && previous.type() == FilterTokenType.OPERATOR) {

                // Разрешаем только определенные комбинации операторов
                String combined = previous.value() + current.value();

                // Разрешенные комбинации: !=, <=, >=, ==
                boolean isAllowedCombination = combined.matches("!=|<=|>=|==");

                if (!isAllowedCombination) {
                    errors.add("Consecutive operators '" + previous.value() + "' and '" + current.value() + "' at position " + current.position());
                }
            }

            // Проверяем две точки подряд
            if (current.type() == FilterTokenType.PUNCTUATION && current.value().equals(".") &&
                    next != null && next.type() == FilterTokenType.PUNCTUATION && next.value().equals(".")) {
                errors.add("Consecutive dots at position " + current.position());
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
                        if (part.isEmpty()) {
                            errors.add("Invalid identifier with empty parts: " + identifier + " at position " + token.position());
                            break;
                        }
                        if (!part.matches("\\w+")) {
                            errors.add("Invalid identifier part '" + part + "' in '" + identifier + "' at position " + token.position());
                        }
                    }
                }
            }

            // Дополнительная проверка: последовательности точек между идентификаторами
            if (token.type() == FilterTokenType.PUNCTUATION && token.value().equals(".")) {
                // Эта проверка будет в validateSyntaxStructure
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
            if (!tokenValue.isEmpty()) {
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
        // 1. Сначала проверяем пунктуацию и операторы - они самые простые
        if (token.matches("[(),.]")) {
            return FilterTokenType.PUNCTUATION;
        }

        // 2. Операторы
        if (token.matches("[=+\\-*/<>]")) {
            return FilterTokenType.OPERATOR;
        }

        // 3. Строковые литералы
        if (token.startsWith("'") && token.endsWith("'")) {
            return FilterTokenType.STRING_LITERAL;
        }

        // 4. Числа
        if (token.matches("\\d+(?:\\.\\d*)?")) {
            return FilterTokenType.NUMBER;
        }

        // 5. Множественные точки подряд - это ошибка
        if (token.matches("\\.{2,}")) {
            return FilterTokenType.UNKNOWN;
        }

        // 6. Идентификаторы с двумя точками подряд - это ошибка
        if (token.matches(".*\\.\\..*")) {
            return FilterTokenType.UNKNOWN;
        }

        // 7. Поддерживаемые функции (точное совпадение)
        if (SUPPORTED_FUNCTIONS.contains(token.toLowerCase())) {
            return FilterTokenType.FUNCTION_NAME;
        }

        // 8. SQL ключевые слова
        if (token.matches("(?i)^(like|in|is|null|not|and|or)$")) {
            return FilterTokenType.KEYWORD;
        }

        // 9. Корректные идентификаторы (включая составные с точками)
        if (token.matches("\\$?\\w+(\\.\\w+)*")) {
            return FilterTokenType.IDENTIFIER;
        }

        // 10. Все остальное - неизвестно
        return FilterTokenType.UNKNOWN;
    }
}
