package ru.korovin.packages.fasterjpa.testProject.service.impl;

import ru.korovin.packages.fasterjpa.testProject.model.TestNestedEntity;
import ru.korovin.packages.fasterjpa.testProject.service.TestNestedEntityService;
import ru.korovin.packages.fasterjpa.template.jpa.JpaCrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile("test")
public class TestNestedEntityServiceImpl extends JpaCrudService<TestNestedEntity, Long> implements TestNestedEntityService {

    public TestNestedEntityServiceImpl(JpaRepository<TestNestedEntity, Long> repository, JpaSpecificationExecutor<TestNestedEntity> specificationExecutor, EntityManager persistenceContext, TransactionTemplate transactionTemplate, Validator validator) {
        super(repository, specificationExecutor, persistenceContext, transactionTemplate, validator);
    }
}
