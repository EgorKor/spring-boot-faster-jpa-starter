package ru.korovin.packages.fasterjpa.exception;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class BusinessProcessException extends RuntimeException {
    private final Class<?> businessProcess;
    private final String message;


    public BusinessProcessException(@NonNull Class<?> businessProcess, @NonNull Throwable cause) {
        this.businessProcess = businessProcess;
        this.message = cause.getMessage();
    }

    public BusinessProcessException(@NonNull Class<?> businessProcess, @NonNull String message) {
        this.businessProcess = businessProcess;
        this.message = message;
    }

}
