package ru.samgtu.packages.webutils.dto;

import ru.samgtu.packages.webutils.exception.EntityOperation;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public record EntityProcessingErrorDto(String entity,
                                       EntityOperation operation,
                                       String detailedMessage) {
}
