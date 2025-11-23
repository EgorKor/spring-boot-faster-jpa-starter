package ru.korovin.packages.fasterjpa.queryparam.filterInternal;

import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Function {
    LENGTH("length()"),
    SIZE("size()"),
    IS_EMPTY("isEmpty()"),
    IS_NOT_EMPTY("isNotEmpty()"),;


    private final String function;

    public static Function parseByOperation(String operation) {
        for (Function func : values()) {
            if (operation.equals(func.function)) {
                return func;
            }
        }
        throw new InvalidParameterException("Недопустимая функция: " + operation);
    }
}
