package ru.korovin.packages.fasterjpa.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс описывающий запрос на обновление записией
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateSpecification {
    /**
     * Глобальный stateless update builder
     */
    public static UpdateSpecificationBuilder ub = new UpdateSpecificationBuilder();

    /**
     * Обновления
     */
    private Map<String, UpdateUnit> updates = new HashMap<>();

    public static UpdateSpecification updateValue(String field, Object value) {
        return ub.update(ub.updateValue(field, value));
    }

    public static UpdateSpecification setNull(String field) {
        return ub.update(ub.setNull(field));
    }

    public static UpdateSpecification plus(String field, Byte value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Short value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Integer value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Long value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Float value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Double value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification minus(String field, Byte value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Short value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Integer value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Long value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Float value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Double value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification concat(String field, String value) {
        return ub.update(ub.concat(field, value));
    }

    public static UpdateSpecification truncateTime(String field) {
        return ub.update(ub.truncateTime(field));
    }

    public static UpdateSpecification addDays(String field, Integer days) {
        return ub.update(ub.addDays(field, days));
    }

    public static UpdateSpecification toUpperCase(String field) {
        return ub.update(ub.toUpperCase(field));
    }

    public static UpdateSpecification toLowerCase(String field) {
        return ub.update(ub.toLowerCase(field));
    }

    public static UpdateSpecification copyValue(String from, String to) {
        return ub.update(ub.copyValue(from, to));
    }


    public record UpdateUnit(String field, UpdateAction action, Object data) {
    }


    public static class UpdateSpecificationBuilder {

        public UpdateUnit setNull(String field) {
            return new UpdateUnit(field, UpdateAction.SET_NULL, "null");
        }

        public UpdateUnit updateValue(String field, Object value) {
            return new UpdateUnit(field, UpdateAction.UPDATE, value);
        }

        public UpdateUnit multiply(String field, Number value) {
            return new UpdateUnit(field, UpdateAction.MULTIPLY, value);
        }

        public UpdateUnit divide(String field, Number value) {
            return new UpdateUnit(field, UpdateAction.DIVIDE, value);
        }

        public UpdateUnit increment(String field) {
            return plus(field, 1);
        }

        public UpdateUnit decrement(String field) {
            return minus(field, 1);
        }

        public UpdateUnit copyValue(String fromField, String toField) {
            return new UpdateUnit(toField, UpdateAction.COPY, fromField);
        }

        public UpdateUnit plus(String field, Number value) {
            return new UpdateUnit(field, UpdateAction.SUM, value);
        }

        public UpdateUnit minus(String field, Float value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit minus(String field, Double value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit minus(String field, Byte value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit minus(String field, Short value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit minus(String field, Integer value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit minus(String field, Long value) {
            return new UpdateUnit(field, UpdateAction.SUM, -value);
        }

        public UpdateUnit addDays(String field, int days) {
            return new UpdateUnit(field, UpdateAction.ADD_DAYS, days);
        }

        public UpdateUnit truncateTime(String field) {
            return new UpdateUnit(field, UpdateAction.TRUNCATE_TIME, null);
        }

        public UpdateUnit concat(String field, String value) {
            return new UpdateUnit(field, UpdateAction.CONCAT, value);
        }

        public UpdateUnit toUpperCase(String field) {
            return new UpdateUnit(field, UpdateAction.UPPER_CASE, null);
        }

        public UpdateUnit toLowerCase(String field) {
            return new UpdateUnit(field, UpdateAction.LOWER_CASE, null);
        }

        public UpdateSpecification update(UpdateUnit... updates) {
            Map<String, UpdateUnit> updateMap = Arrays.stream(updates).collect(Collectors.toMap(
                    UpdateUnit::field, o -> o
            ));
            return new UpdateSpecification(updateMap);
        }
    }

}
