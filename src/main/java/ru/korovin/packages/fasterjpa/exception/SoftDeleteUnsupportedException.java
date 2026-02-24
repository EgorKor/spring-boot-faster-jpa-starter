package ru.korovin.packages.fasterjpa.exception;

/**
 * Исключение отсутствия поддержки операции мягкого удаления
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class SoftDeleteUnsupportedException extends RuntimeException {
    public SoftDeleteUnsupportedException(String message) {
        super(message);
    }
}
