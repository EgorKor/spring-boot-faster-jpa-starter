package ru.korovin.packages.fasterjpa.dto;

import ru.korovin.packages.fasterjpa.exception.EntityOperation;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public record EntityProcessingErrorDto(String entity,
                                       EntityOperation operation,
                                       String detailedMessage) {
}
