package ru.korovin.packages.fasterjpa.annotations;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Аннотация используемая для ограничения возможных
 * используемых операций в рамках фильтрации.
 * Пример использования в классе описывающем фильтр.
 *
 * <pre>
 *     {@code
 *     class EducationSpaceAggregateFilter extends Filter<EducationSpace> {
 *         @AllowedOperations(FilterOperation.EQUALS)
 *         Supplier<String> cgodCode = () -> educationSpace().consolidatedGroupOfDirections().code();
 *
 *         @AllowedOperations(FilterOperation.EQUALS)
 *         Supplier<String> directionId = () -> educationSpace().consolidatedGroupOfDirections().directions().id();
 *     }
 *     }
 * </pre>
 *
 * То есть фильтр принимает два параметра cgodCode и directionId, для которых
 * разрешены только операция EQUALS
 *
 * @author EgorKor
 * @since 2026
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedOperations {

    /**
     * Массив допустимых операций
     * */
    FilterOperation[] value() default {FilterOperation.EQUALS};
}
