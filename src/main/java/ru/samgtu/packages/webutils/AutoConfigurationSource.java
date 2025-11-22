package ru.samgtu.packages.webutils;

import ru.samgtu.packages.webutils.api.GenericApiControllerAdvice;
import ru.samgtu.packages.webutils.dto.DtoMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration
@RequiredArgsConstructor
public class AutoConfigurationSource {
    private final EntityManager entityManager;
    private final ApplicationContext applicationContext;


    @ConditionalOnMissingBean(GenericApiControllerAdvice.class)
    @Bean
    public GenericApiControllerAdvice genericApiControllerAdvice(){
        return new GenericApiControllerAdvice();
    }

    @ConditionalOnMissingBean(LocalValidatorFactoryBean.class)
    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @ConditionalOnMissingBean(Validator.class)
    @Bean
    public Validator validator(@Autowired LocalValidatorFactoryBean validatorFactoryBean) {
        return validatorFactoryBean.getValidator();
    }

    @Bean
    public DtoMapper dtoConverter() {
        return new DtoMapper();
    }



    @Role(value = BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

}
