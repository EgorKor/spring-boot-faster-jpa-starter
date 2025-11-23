package ru.korovin.packages.fasterjpa.testProject.service.impl;

import ru.korovin.packages.fasterjpa.testProject.model.StructureDepartment;
import ru.korovin.packages.fasterjpa.testProject.service.StructureDepartmentService;
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

@Profile("test")
@Service
public class StructureDepartmentServiceImpl
        extends JpaCrudService<StructureDepartment, Long>
        implements StructureDepartmentService {

    public StructureDepartmentServiceImpl(JpaRepository<StructureDepartment, Long> repository, JpaSpecificationExecutor<StructureDepartment> specificationExecutor, EntityManager persistenceContext, TransactionTemplate transactionTemplate, Validator validator) {
        super(repository, specificationExecutor, persistenceContext, transactionTemplate, validator);
    }
}
