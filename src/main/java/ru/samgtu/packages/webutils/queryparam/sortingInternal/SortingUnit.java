package ru.samgtu.packages.webutils.queryparam.sortingInternal;

import ru.samgtu.packages.webutils.exception.InvalidParameterException;
import lombok.NonNull;

public record SortingUnit(String field, String order) {
    public SortingUnit(@NonNull String field,
                       @NonNull String order) {
        order = order.toLowerCase();
        if (!field.matches("[a-zA-Z.]+")) {
            throw new InvalidParameterException("Недопустимое имя параметра сортировки: " + field);
        }
        if (!order.equals("desc") && !order.equals("asc")) {
            throw new InvalidParameterException("Недопустимое значение сортировки: '" +
                    order +
                    "' .Допустимые значения ['asc','desc'] ");
        }
        this.field = field;
        this.order = order;
    }

    public static SortingUnit asc(String field){
        return new SortingUnit(field, "asc");
    }

    public static SortingUnit desc(String field){
        return new SortingUnit(field, "desc");
    }
}