package ru.korovin.packages.fasterjpa.tests.params;

import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterToken;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterTokenType;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterTokenizer;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class FilterTokenizerTest {

    private final FilterTokenizer tokenizer = new FilterTokenizer();

    @Test
    @DisplayName("Должен корректно токенизировать простое выражение")
    void shouldTokenizeSimpleExpression() {
        // Given
        String expression = "activityKind.area.code";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);

        // Then
        assertThat(tokens).hasSize(1);
        assertToken(tokens.get(0), FilterTokenType.IDENTIFIER, "activityKind.area.code", 0);
    }

    @Test
    @DisplayName("Должен токенизировать функцию CONCAT с аргументами")
    void shouldTokenizeConcatFunction() {
        // Given
        String expression = "concat(to_char(activityKind.area.code,'FM09'),'.',to_char(activityKind.code,'FM099'))";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);

        // Then
        assertThat(tokens).hasSize(18);
        assertToken(tokens.get(0), FilterTokenType.FUNCTION_NAME, "concat", 0);
        assertToken(tokens.get(1), FilterTokenType.PUNCTUATION, "(", 6);
        assertToken(tokens.get(2), FilterTokenType.FUNCTION_NAME, "to_char", 7);
        assertToken(tokens.get(3), FilterTokenType.PUNCTUATION, "(", 14);
        assertToken(tokens.get(4), FilterTokenType.IDENTIFIER, "activityKind.area.code", 15);
        assertToken(tokens.get(5), FilterTokenType.PUNCTUATION, ",", 37);
        assertToken(tokens.get(6), FilterTokenType.STRING_LITERAL, "'FM09'", 38);
        assertToken(tokens.get(7), FilterTokenType.PUNCTUATION, ")", 44);
        assertToken(tokens.get(8), FilterTokenType.PUNCTUATION, ",", 45);
        assertToken(tokens.get(9), FilterTokenType.STRING_LITERAL, "'.'", 46);
        assertToken(tokens.get(10), FilterTokenType.PUNCTUATION, ",", 49);
        assertToken(tokens.get(11), FilterTokenType.FUNCTION_NAME, "to_char", 50);
        assertToken(tokens.get(12), FilterTokenType.PUNCTUATION, "(", 57);
        assertToken(tokens.get(13), FilterTokenType.IDENTIFIER, "activityKind.code", 58);
        assertToken(tokens.get(14), FilterTokenType.PUNCTUATION, ",", 75);
        assertToken(tokens.get(15), FilterTokenType.STRING_LITERAL, "'FM099'", 76);
        assertToken(tokens.get(16), FilterTokenType.PUNCTUATION, ")", 83);
        assertToken(tokens.get(17), FilterTokenType.PUNCTUATION, ")", 84);
    }

    @Test
    @DisplayName("Должен токенизировать функцию COALESCE")
    void shouldTokenizeCoalesceFunction() {
        // Given
        String expression = "coalesce(lower(activityKind.name), 'default')";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);

        // Then
        assertThat(tokens).hasSize(9);
        assertToken(tokens.get(0), FilterTokenType.FUNCTION_NAME, "coalesce", 0);
        assertToken(tokens.get(1), FilterTokenType.PUNCTUATION, "(", 8);
        assertToken(tokens.get(2), FilterTokenType.FUNCTION_NAME, "lower", 9);
        assertToken(tokens.get(3), FilterTokenType.PUNCTUATION, "(", 14);
        assertToken(tokens.get(4), FilterTokenType.IDENTIFIER, "activityKind.name", 15);
        assertToken(tokens.get(5), FilterTokenType.PUNCTUATION, ")", 32);
        assertToken(tokens.get(6), FilterTokenType.PUNCTUATION, ",", 33);
        assertToken(tokens.get(7), FilterTokenType.STRING_LITERAL, "'default'", 35);
        assertToken(tokens.get(8), FilterTokenType.PUNCTUATION, ")", 44);
    }

    @Test
    @DisplayName("Должен токенизировать функцию SUBSTRING")
    void shouldTokenizeSubstringFunction() {
        // Given
        String expression = "substring(activityKind.description,1,10)";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);

        // Then
        assertThat(tokens).isNotEmpty();
        assertToken(tokens.get(0), FilterTokenType.FUNCTION_NAME, "substring", 0);
        assertToken(tokens.get(1), FilterTokenType.PUNCTUATION, "(", 9);
        assertToken(tokens.get(2), FilterTokenType.IDENTIFIER, "activityKind.description", 10);
    }

    @ParameterizedTest
    @MethodSource("provideValidExpressions")
    @DisplayName("Должен валидировать корректные выражения")
    void shouldValidateCorrectExpressions(String expression, String description) {
        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertTrue(result.isValid(),
                String.format("Expression '%s' should be valid. Errors: %s",
                        description, result.errors()));
        assertEquals("Syntax is valid", result.message());
        assertThat(result.errors()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidExpressions")
    @DisplayName("Должен находить ошибки в некорректных выражениях")
    void shouldFindErrorsInInvalidExpressions(String expression, List<String> expectedErrors) {
        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertEquals("Syntax errors found", result.message());

        for (String expectedError : expectedErrors) {
            assertTrue(result.errors().stream()
                            .anyMatch(error -> error.contains(expectedError)),
                    String.format("Expected error containing '%s', but got: %s",
                            expectedError, result.errors()));
        }
    }

    @Test
    @DisplayName("Должен возвращать ошибку для пустого выражения")
    void shouldReturnErrorForEmptyExpression() {
        // When
        FilterValidationResult result = tokenizer.validate(List.of());

        // Then
        assertFalse(result.isValid());
        assertEquals("Empty expression", result.message());
        assertThat(result.errors()).contains("Expression is empty");
    }

    @Test
    @DisplayName("Должен находить несбалансированные скобки")
    void shouldFindUnbalancedParentheses() {
        // Given
        String expression = "concat(to_char(activityKind.area.code,'FM09')";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error -> error.contains("Unmatched opening parenthesis"));
    }

    @Test
    @DisplayName("Должен находить функцию без скобок")
    void shouldFindFunctionWithoutParentheses() {
        // Given
        String expression = "concat activityKind.code";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("must be followed by parentheses"));
    }

    @Test
    @DisplayName("Должен проверять количество аргументов в CONCAT")
    void shouldValidateConcatArgumentsCount() {
        // Given
        String expression = "concat(activityKind.code)";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("CONCAT function requires at least 2 arguments"));
    }

    @Test
    @DisplayName("Должен проверять количество аргументов в TO_CHAR")
    void shouldValidateToCharArgumentsCount() {
        // Given
        String expression = "to_char(activityKind.code)";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("TO_CHAR function requires exactly 2 arguments"));
    }

    @Test
    @DisplayName("Должен находить некорректные идентификаторы")
    void shouldFindInvalidIdentifiers() {
        // Given
        String expression = "activityKind..area.code";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid(), "Должен быть невалидным. Валидный: " + result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("Unknown token '..' at position 12"));
    }

    @Test
    @DisplayName("Должен находить неизвестные токены")
    void shouldFindUnknownTokens() {
        // Given
        String expression = "unknown_func(test)";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("Unsupported function"));
    }

    @Test
    @DisplayName("Должен находить последовательные идентификаторы")
    void shouldFindConsecutiveOperators() {
        // Given
        String expression = "activityKind.code";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Должен находить некорректно расположенные запятые")
    void shouldFindMisplacedCommas() {
        // Given
        String expression = "concat(,activityKind.code)";

        // When
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        FilterValidationResult result = tokenizer.validate(tokens);

        // Then
        assertFalse(result.isValid());
        assertThat(result.errors()).anyMatch(error ->
                error.contains("Misplaced comma"));
    }

    // Вспомогательные методы

    private static void assertToken(FilterToken token, FilterTokenType expectedType,
                                    String expectedValue, int expectedPosition) {
        assertEquals(expectedType, token.type(),
                String.format("Token type mismatch for '%s'", token.value()));
        assertEquals(expectedValue, token.value(),
                String.format("Token value mismatch for type %s", token.type()));
        assertEquals(expectedPosition, token.position(),
                String.format("Token position mismatch for '%s'", token.value()));
    }

    // Провайдеры данных для параметризованных тестов

    private static Stream<Arguments> provideValidExpressions() {
        return Stream.of(
                Arguments.of("activityKind.area.code", "Простой идентификатор"),
                Arguments.of("concat(to_char(activityKind.area.code,'FM09'),'.',to_char(activityKind.code,'FM099'))",
                        "Сложный CONCAT с TO_CHAR"),
                Arguments.of("coalesce(lower(activityKind.name), 'default')", "COALESCE с LOWER"),
                Arguments.of("substring(activityKind.description,1,10)", "SUBSTRING с FROM и FOR"),
                Arguments.of("upper(activityKind.name)", "Простая функция UPPER"),
                Arguments.of("trim(activityKind.code)", "Функция TRIM"),
                Arguments.of("activityKind.area.code", "Простое сравнение"),
                Arguments.of("length(activityKind.name)", "Функция LENGTH с оператором")
        );
    }

    private static Stream<Arguments> provideInvalidExpressions() {
        return Stream.of(
                Arguments.of("concat(to_char(activityKind.area.code,))",
                        List.of("CONCAT function requires at least 2 arguments at position 0")),
                Arguments.of("activityKind..area.code",
                        List.of("Unknown token '..' at position 12")),
                Arguments.of("concat(test",
                        List.of("Unmatched opening parenthesis")),
                Arguments.of("concat(activityKind.code)",
                        List.of("CONCAT function requires at least 2 arguments")),
                Arguments.of("to_char(activityKind.code)",
                        List.of("TO_CHAR function requires exactly 2 arguments")),
                Arguments.of("coalesce()",
                        List.of("COALESCE function requires at least 1 argument")),
                Arguments.of("activityKind.code =* 5",
                        List.of("Consecutive operators")),
                Arguments.of("concat(,activityKind.code)",
                        List.of("Misplaced comma"))
        );
    }
}
