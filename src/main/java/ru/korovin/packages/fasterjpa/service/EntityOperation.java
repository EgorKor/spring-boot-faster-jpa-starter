package ru.korovin.packages.fasterjpa.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Перечисление операций с сущностями
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
@AllArgsConstructor
public enum EntityOperation {
    CREATE("Создание"),
    UPDATE("Обновление"),
    DELETE("Удаление");

    private final String verboseName;
}
