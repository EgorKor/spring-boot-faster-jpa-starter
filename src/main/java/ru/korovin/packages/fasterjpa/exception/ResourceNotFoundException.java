package ru.korovin.packages.fasterjpa.exception;

/**
 * Исключение отсутствия ресурса
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
