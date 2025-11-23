package ru.korovin.packages.fasterjpa.testProject.service.impl;

import ru.korovin.packages.fasterjpa.testProject.model.DirectionProfile;
import ru.korovin.packages.fasterjpa.testProject.service.DirectionProfileService;
import ru.korovin.packages.fasterjpa.template.jpa.JpaCrudService;
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

@Service
@Profile("test")
public class DirectionProfileServiceImpl extends JpaCrudService<DirectionProfile, Long>
        implements DirectionProfileService {

    public DirectionProfileServiceImpl(JpaRepository<DirectionProfile, Long> repository, JpaSpecificationExecutor<DirectionProfile> specificationExecutor, EntityManager persistenceContext, TransactionTemplate transactionTemplate, Validator validator) {
        super(repository, specificationExecutor, persistenceContext, transactionTemplate, validator);
    }
}
