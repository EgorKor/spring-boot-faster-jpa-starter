package ru.korovin.packages.fasterjpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация используемая для ограничения кол-ва
 * используемых операций фильтрации в конкретном поле (если аннотация над полем) и
 * в фильтре в целом (если аннотация над классом).
 * Пример использования в классе описывающем фильтр.
 *
 * <pre>
 *     {@code
 *      @ParamCountLimit(1)
 *      class AdditionalProfessionalCompetenceFilter
 *             extends Filter<AdditionalProfessionalCompetence> {
 *
 *         @ParamCountLimit(1)
 *         @AllowedOperations(FilterOperation.CONTAINS)
 *         Supplier<String> search = () -> Concat.sql(
 *                 "'ДПК-'", competence().serial(), "' - '",
 *                 competence().name()
 *         );
 *
 *         @ParamCountLimit(1)
 *         Supplier<String> startYear = () -> competence().startYear()
 *      }
 *     }
 * </pre>
 *
 * То есть фильтр принимает два параметра search и startYear. Но фактически
 * каждый параметр нельзя указать более 1 одного раза, и в целом в фильтре
 * не может быть более одного указанного параметра одновременно.
 *
 * @author EgorKor
 * @since 2026
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamCountLimit {
    int UNLIMITED = -1;

    /**
     * Кол-во допустимых параметров
     * */
    int value() default UNLIMITED;
}
