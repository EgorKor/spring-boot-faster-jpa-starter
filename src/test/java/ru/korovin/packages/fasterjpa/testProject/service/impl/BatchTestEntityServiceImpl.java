package ru.korovin.packages.fasterjpa.testProject.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.korovin.packages.fasterjpa.testProject.model.TestingEntityBatching;
import ru.korovin.packages.fasterjpa.testProject.service.BatchTestEntityService;
import ru.korovin.packages.fasterjpa.template.jpa.JpaBatchCrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Profile("test")
@Service
public class BatchTestEntityServiceImpl
        extends JpaBatchCrudService<TestingEntityBatching, Long>
        implements BatchTestEntityService {


    public BatchTestEntityServiceImpl(JpaRepository<TestingEntityBatching, Long> jpaRepository, JpaSpecificationExecutor<TestingEntityBatching> jpaSpecificationExecutor, EntityManager persistenceContext, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, persistenceContext, transactionTemplate, validator);
    }
}
