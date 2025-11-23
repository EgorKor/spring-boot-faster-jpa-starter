package ru.korovin.packages.fasterjpa.tests.params;

import ru.korovin.packages.fasterjpa.testProject.model.TestEntity;
import ru.korovin.packages.fasterjpa.testProject.model.TestNestedEntity;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.*;

import java.util.List;

import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.fb;
import static ru.korovin.packages.fasterjpa.queryparam.filterInternal.Is.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterTest {
    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void setup() {
        try {
            emf = Persistence.createEntityManagerFactory("test-pu");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterAll
    static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void init() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    @AfterEach
    void tearDown() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        if (em.isOpen()) {
            em.close();
        }
    }

    @Test
    void testFilterJPA1() {
        Filter<TestEntity> filter = new Filter<>(TestEntity.class);
        filter.setConditions(
                /*List.of(
                        "id:=:10", "name:like:some name", "isDeleted:is:true"
                )*/
                List.of(fb.equals("id", 10),
                        fb.contains("name", "some name"),
                        fb.is("isDeleted", TRUE))
        );
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestEntity> cq = cb.createQuery(TestEntity.class);
        Predicate predicate = filter.toPredicate(cq.from(TestEntity.class), cq, cb);
        System.out.println(predicate);
        assertEquals(predicate.getExpressions().size(), 3);
    }

    @Test
    void testFilterJPA2() {
        Filter<TestNestedEntity> filter = new Filter<>(TestNestedEntity.class);
        filter.setConditions(
                List.of(fb.equals("parent.id",10))
        );
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestNestedEntity> cq = cb.createQuery(TestNestedEntity.class);
        Predicate predicate = filter.toPredicate(cq.from(TestNestedEntity.class), cq, cb);
        System.out.println(predicate);

        assertEquals(predicate.getExpressions().size(), 1);
    }

}
