package ru.korovin.packages.fasterjpa.tests.jpaCrud;

import ru.korovin.packages.fasterjpa.testProject.service.EducationProgramService;
import ru.korovin.packages.fasterjpa.testProject.service.impl.EducationProgramServiceImpl;
import ru.korovin.packages.fasterjpa.testProject.service.impl.OrderServiceImpl;
import ru.korovin.packages.fasterjpa.testProject.service.impl.UserServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.korovin.packages.fasterjpa.queryparam.factories.Filters;

@Import({OrderServiceImpl.class, UserServiceImpl.class, LocalValidatorFactoryBean.class, EducationProgramServiceImpl.class})
@ActiveProfiles("test")
@DataJpaTest
public class ManyToManyFilterTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private EducationProgramService educationProgramService;

    @Test
    public void test(){
        /*CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EducationProgram> q = cb.createQuery(EducationProgram.class);
        Root<EducationProgram> root = q.from(EducationProgram.class);

        q.where(cb.equal(root.get("profiles").get("name"), "234"));
        TypedQuery<EducationProgram> tq = em.createQuery(q);
        List<EducationProgram> list = tq.getResultList();*/

        educationProgramService.getList(
                Filters.equal("profiles.name","234")
        );


    }

}
