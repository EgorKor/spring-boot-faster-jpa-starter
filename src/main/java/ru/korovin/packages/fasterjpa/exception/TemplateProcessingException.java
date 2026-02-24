package ru.korovin.packages.fasterjpa.exception;

/**
 * Исключение обработки шаблона
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class TemplateProcessingException extends RuntimeException {
    public TemplateProcessingException(String message) {
        super(message);
    }

    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
