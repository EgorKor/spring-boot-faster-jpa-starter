package ru.korovin.packages.fasterjpa.tests.params;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterCondition;
import ru.korovin.packages.fasterjpa.testProject.params.UserFilter;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.fb;

@ExtendWith(MockitoExtension.class)
public class FilterTest {
    @Mock
    private Root<TestEntity> root;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @Test
    void testDefaultConstructor() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
        assertFalse(filter.isFiltered());
    }

    @Test
    void testConstructorWithFilterList() {
        Filter<TestEntity> filter = new Filter<>(
                fb.and(
                        fb.contains("name", "John"),
                        fb.greater("age", 30)
                )
        );
        assertEquals(2, filter.getConditionsCount());
    }

    @Test
    void testIsFiltered() {
        Filter<TestEntity> filter = new Filter<>(
                fb.contains("name", "John"));
        assertTrue(filter.isFiltered());
    }

    @Test
    void testIsUnfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
    }

    @Test
    void testAnd() {
        Filter<TestEntity> filter1 = new Filter<>(fb.contains("name", "John"));
        Filter<TestEntity> filter2 = new Filter<>(fb.greater("age", 30));

        Filter<TestEntity> result = filter1.andFilter(filter2);
        assertEquals(2, result.getConditionsCount());
    }

    @Test
    void testToPredicate_unfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        Predicate result = filter.toPredicate(root, cb);
        assertNull(result);
    }

    @Test
    void testSoftDeleteFilter_booleanField() {
        Field field = getFiltersByFieldNameField(TestEntity.class, "active");
        Filter<TestEntity> filter = Filter.softDeleteFilter(field, true);

        assertEquals(1, filter.getConditionsCount());
    }

    @Test
    void testFilterIndex() {
        UserFilter userFilter = fb.and(
                fb.equals("orders_name", "something")
        ).toFilter(UserFilter.class);
        assertTrue(userFilter.containsFilterWithField("orders_name"));
        FilterCondition op1 = userFilter.findFirstFilterByName("orders_name").get();
        userFilter.applyAllies();
        FilterCondition op2 = userFilter.findFirstFilterByName("orders_name").get();
        assertSame(op1, op2);
    }

    @Test
    void testFilterParamCountConstraint() {
        UserFilter userFilter = fb.and(
                fb.equals("orders_name", "something"),
                fb.like("orders_name", "something")
        ).toFilter(UserFilter.class);
        var ex = assertThrows(InvalidParameterException.class, userFilter::validateFields);
        System.out.println(ex.getMessage());
    }

    @Test
    void testEmptyFilter() {
        Filter<TestEntity> filter = Filter.empty();
        assertTrue(filter.isUnfiltered());
    }

    @Test
    void testEmptyFilterWithType() {
        Filter<TestEntity> filter = Filter.empty(TestEntity.class);
        assertTrue(filter.isUnfiltered());
        assertEquals(TestEntity.class, filter.getEntityType());
    }

    @Test
    void testFilterBuilder() {
        Filter<TestEntity> filter = fb.and(
                fb.equals("name", "John"),
                fb.greater("age", "30")
        ).toFilter();

        assertEquals(2, filter.getConditionsCount());
    }

    private Field getFiltersByFieldNameField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static class TestEntity {
        String name;
        int age;
        boolean active;
        NestedEntity nested;
    }

    static class NestedEntity {
        String property;
    }
}
