package ru.korovin.packages.fasterjpa.tests;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.korovin.packages.fasterjpa.service.query.QueryManager;
import ru.korovin.packages.fasterjpa.testProject.service.impl.UserServiceImpl;

@Import({UserServiceImpl.class, LocalValidatorFactoryBean.class})
@ActiveProfiles("test")
@DataJpaTest
public class QueryManagerTest {
    @Autowired
    private EntityManager persistenceContext;



}
