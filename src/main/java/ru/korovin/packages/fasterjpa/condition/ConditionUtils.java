package ru.korovin.packages.fasterjpa.condition;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Утилиты для работы с условным созданием
 * бинов в Spring Framework
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 * */
public class ConditionUtils {

    /**
     * Метод проверяющий присутствие бина в application context
     * помеченного указанной аннотацией
     *
     * @param context контекст вычисляемого условия
     * @param metadata метаданные
     * @param annotation проверяемая аннотация
     * @return true если есть хотя бы один бин помеченный указанной аннотацией
     * */
    public static boolean isBeanWithAnnotationPresents(ConditionContext context,
                                                       AnnotatedTypeMetadata metadata,
                                                       Class<? extends Annotation> annotation) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return false;
        }
        Map<String, Object> beansWithAnnotation =
                beanFactory.getBeansWithAnnotation(annotation);
        return !beansWithAnnotation.isEmpty();
    }
}
