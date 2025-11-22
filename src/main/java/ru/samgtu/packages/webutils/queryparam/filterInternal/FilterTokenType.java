package ru.samgtu.packages.webutils.queryparam.filterInternal;

public enum FilterTokenType {
    // Ключевые слова и функции
    FUNCTION_NAME,      // concat, to_char, coalesce, lower и т.д.
    IDENTIFIER,         // activityKind.area.code, profActivityArea
    STRING_LITERAL,     // 'FM09', 'FM099'
    NUMBER,             // 123, 45.67
    PUNCTUATION,        // ( ) , .
    OPERATOR,           // = > < + - * /
    UNKNOWN
}
