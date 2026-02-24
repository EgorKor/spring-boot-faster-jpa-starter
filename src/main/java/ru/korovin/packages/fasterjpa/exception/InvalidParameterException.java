package ru.korovin.packages.fasterjpa.exception;

/**
 * Исключение невалидного параметра
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 *
 */
public class InvalidParameterException extends RuntimeException {

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
