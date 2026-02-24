package ru.korovin.packages.fasterjpa.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Перечисление доступных действий при массовом обновлении
 * записей в БД
 *
 * @author EgorKor
 * @since 2026
 */
@Getter
@AllArgsConstructor
public enum UpdateAction {
    UPDATE("Обновление", "set property = value"),
    SUM("Прибавить значение", "set property = property + value"),
    MULTIPLY("Умножить на значение", "set property = property * value"),
    DIVIDE("Разделить на значение", "set property = property / value"),
    ADD_DAYS("Прибавить дни", "set date_property = date_property + INTERVAL 'n days'"),
    TRUNCATE_TIME("Очистить время", "set timestamp_property = date_trunc('day', timestamp_property)"),
    CONCAT("Объединить строки", "set property = property || 'suffix'"),
    UPPER_CASE("Верхний регистр", "set property = UPPER(property)"),
    LOWER_CASE("Нижний регистр", "set property = LOWER(property)"),
    COPY("Копировать значение", "set target_property = source_property"),
    SET_NULL("Установить NULL", "set property = NULL");

    private final String verboseName;
    private final String description;
}
