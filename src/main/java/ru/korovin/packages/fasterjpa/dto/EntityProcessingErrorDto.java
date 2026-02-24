package ru.korovin.packages.fasterjpa.dto;

import ru.korovin.packages.fasterjpa.service.EntityOperation;

/**
 * DTO ошибки обработки сущности
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public record EntityProcessingErrorDto(String entity,
                                       EntityOperation operation,
                                       String detailedMessage) {
}
