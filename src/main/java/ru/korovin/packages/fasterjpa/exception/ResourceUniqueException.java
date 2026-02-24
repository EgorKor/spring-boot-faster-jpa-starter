package ru.korovin.packages.fasterjpa.exception;


/**
 * Исключение нарушения уникальности ресурса
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class ResourceUniqueException extends RuntimeException {
    public ResourceUniqueException(String message) {
        super(message);
    }
}
