package ru.korovin.packages.fasterjpa.exception;

public class ResourceUniqueException extends RuntimeException {
    public ResourceUniqueException(String message) {
        super(message);
    }
}
