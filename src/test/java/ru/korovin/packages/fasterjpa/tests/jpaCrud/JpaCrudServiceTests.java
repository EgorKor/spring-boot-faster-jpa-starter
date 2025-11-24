package ru.korovin.packages.fasterjpa.tests.jpaCrud;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.exception.ResourceNotFoundException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.Pagination;
import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import ru.korovin.packages.fasterjpa.queryparam.factories.Filters;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.Is;
import ru.korovin.packages.fasterjpa.service.PageableResult;
import ru.korovin.packages.fasterjpa.testProject.model.Tag;
import ru.korovin.packages.fasterjpa.testProject.model.TestEntity;
import ru.korovin.packages.fasterjpa.testProject.params.UserFilter;
import ru.korovin.packages.fasterjpa.testProject.params.UserSort;
import ru.korovin.packages.fasterjpa.testProject.service.TestEntityService;
import ru.korovin.packages.fasterjpa.testProject.service.TestNestedEntityService;
import ru.korovin.packages.fasterjpa.testProject.service.impl.TestEntityCrudServiceImpl;
import ru.korovin.packages.fasterjpa.testProject.service.impl.TestNestedEntityServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.korovin.packages.fasterjpa.queryparam.Sorting.sb;
import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.*;

@Import({TestEntityCrudServiceImpl.class, TestNestedEntityServiceImpl.class, LocalValidatorFactoryBean.class})
@DataJpaTest
@ActiveProfiles("test")
public class JpaCrudServiceTests {
    @Autowired
    private EntityManager em;
    @Autowired
    private TestEntityService testEntityService;
    @Autowired
    private TestNestedEntityService testNestedEntityService;
    @Autowired
    private JpaRepository<TestEntity, Long> repo;

    @BeforeEach
    public void setup() {
        repo.deleteAll();
        repo.flush();
        repo.saveAll(
                List.of(
                        TestEntity.builder()
                                .id(1L)
                                .name("123456789")
                                .isDeleted(false)
                                .tags(List.of("tag1", "tag2"))
                                .nums(List.of(1, 2, 3))
                                .nullableProperty(10)
                                .flag(true)
                                .enumTags(List.of(Tag.TAG1, Tag.TAG2))
                                .build(),
                        TestEntity.builder()
                                .id(2L)
                                .name("1234")
                                .isDeleted(false)
                                .flag(false)
                                .nullableProperty(null)
                                .nums(List.of(1, 2))
                                .tags(List.of("tag2"))
                                .enumTags(List.of(Tag.TAG1))
                                .build()
                )
        );
        repo.flush();
    }

    @Test
    public void shouldThrowExceedLimitParametersCountExceptionForFilter() {
        UserFilter filter = fb.and(UserFilter.class,
                fb.equals("id", 1),
                fb.equals("orders_name", "name"));
        System.out.println(filter);
        filter.applyAllies();
        var exception = assertThrows(InvalidParameterException.class, filter::validateOperations);
        System.out.println(exception.getMessage());
    }

    @Test
    public void shouldThrowExceedLimitParametersCountExceptionForSorting() {
        UserSort userSort = sb.by(UserSort.class,
                sb.asc("id"),
                sb.desc("name"));
        Assertions.assertThrows(InvalidParameterException.class, userSort::toJpaSort);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonAllowedSortingParam() {
        UserSort userSort = sb.by(UserSort.class, sb.asc("ids"));
        Assertions.assertThrows(InvalidParameterException.class, userSort::toJpaSort);
    }


    @Test
    public void shouldCorrectMapIsTrue() {
        assertEquals(1, testEntityService
                .countByFilter(isTrue("flag")));
    }

    @Test
    public void shouldCorrectMapIsFalse() {
        assertEquals(1, testEntityService.countByFilter(isFalse("flag")));
    }

    @Test
    public void shouldCorrectMapIsNull() {
        assertEquals(1, testEntityService.countByFilter(isNull("nullableProperty")));
    }

    @Test
    public void shouldCorrectMapIsNotNull() {
        assertEquals(1, testEntityService.countByFilter(isNotNull("nullableProperty")));
    }


    @Test
    public void shouldCorrectMapInOperationWithList() {
        assertEquals(1, testEntityService.countByFilter(in("tags", "tag1")));
    }

    @Test
    public void shouldCorrectMapInOperationWithEnumList() {
        assertEquals(2, testEntityService.countByFilter(in("enumTags", "TAG1")));
    }

    @Test
    public void shouldCorrectMapInOperationWithIntegerList() {
        assertEquals(2, testEntityService.countByFilter(in("nums", 1, 2)));
    }

    @Test
    public void shouldThrowNotFound() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(10L);
        });
    }

    @Test
    public void shouldThrowNotFoundAndGetCorrectMessage() {
        try {
            testEntityService.getById(10L);
        } catch (ResourceNotFoundException e) {
            assertEquals("Сущность TestEntity с id = 10 не найдена.", e.getMessage());
        }
    }

    @Test
    public void shouldNotFoundAfterSoftDeleteById() {
        testEntityService.softDeleteById(2L);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(2L);
        });
    }

    @Test
    public void shouldNotFoundAfterSoftDeleteWithFilter() {
        Filter filter = equal("id", 1);
        testEntityService.deleteByFilter(filter);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(1L);
        });
    }

    @Test
    public void shouldFoundZeroRecordsAfterSoftDeleteAll() {
        testEntityService.softDeleteAll();
        assertEquals(0, testEntityService.countAll());
    }

    @Test
    public void shouldParseSizeFunctionForEquals() {
        assertEquals(
                1, testEntityService.countByFilter(
                        fb.and(
                                fb.equals("nums.size()", "2")
                        )
                )
        );
    }

    @Test
    public void shouldParseLengthFunctionForSubstring() {
        assertEquals(
                2, testEntityService.countByFilter(
                        Filters.greaterThan(
                                "substring(trim(upper(lower(name))),1).length()", 3
                        )
                )
        );
    }

    @Test
    public void shouldParseConcatTextBlock(){
        assertDoesNotThrow(() -> {
            testEntityService.countByFilter(
                    Filters.like("""
                            concat(
                                to_char(id, 'FM099'),
                                ' ',
                                cast(id, 'text')
                            )
                            ""","something")
            );
        });
    }

    @Test
    public void shouldParseCoalesce() {
        assertEquals(
                1, testEntityService.countByFilter(
                        Filters.equal("coalesce(id, nullableProperty)", 1)
                )
        );
    }

    @Test
    public void shouldParseMathFunctions(){
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("floor(nullableProperty)",1)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("ceiling(nullableProperty)",1)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("abs(nullableProperty)",1)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("round(nullableProperty,1)",1)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("mod(nullableProperty,1)",1)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("sqrt(nullableProperty)",1)));
    }

    @Test
    public void shouldParseStringFunctions(){
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("left(name,1)","123")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("right(name,1)","123")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("rpad(name,1,'*')","123")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("lpad(name,1,'*')","123")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("repeat(concat('abc ',name),2)","123")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("position(name,'substring')",123)));
        assertDoesNotThrow(() -> testEntityService.countByFilter(equal("instr(name,'substring')",123)));
    }

    @Test
    public void shouldParseDateFunctions(){
        assertDoesNotThrow(() -> testEntityService.countByFilter(isNotNull("current_timestamp()")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(isNotNull("year(current_timestamp())")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(isNotNull("month(current_timestamp())")));
        assertDoesNotThrow(() -> testEntityService.countByFilter(isNotNull("day(current_timestamp())")));
    }


    @Test
    public void shouldParseSizeFunctionForCompare() {
        assertEquals(testEntityService.countByFilter(greaterThanOrEqual("nums.size()", 2)), 2);
    }

    @Test
    public void shouldParseLengthFunctionForEquals() {
        assertEquals(testEntityService.countByFilter(equal("name.length()", 4)), 1);
        assertEquals(testEntityService.countByFilter(notEqual("name.length()", 4)), 1);
        assertEquals(testEntityService.countByFilter(in("name.length()", 3, 4)), 0);
    }

    @Test
    public void shouldParseLengthFunctionForCompare() {
        assertEquals(testEntityService.countByFilter(greaterThanOrEqual("name.length()", 5)), 1);
    }

    @Test
    public void shouldParseIsEmptyAndNotIsEmpty() {
        assertEquals(testEntityService.countByFilter(isTrue("tags.isEmpty()")), 0);
        assertEquals(testEntityService.countByFilter(isTrue("tags.isNotEmpty()")), 2);
    }

    @Test
    public void shouldParseReplace(){
        System.out.println(
                testEntityService.getList(Filters.isNotNull("replace(name,'1234','Egor')"))
        );
    }

    @Test
    public void shouldParseCast() {
        assertEquals(testEntityService.countByFilter(like("cast(id,'text')", "1")), 1);
        assertEquals(testEntityService.countByFilter(greaterThan("cast(name,'double')", 1234)), 1);
        assertEquals(testEntityService.countByFilter(equal("cast(name,'integer')", 1234)), 1);
    }

    @Test
    public void shouldGetById() {
        Assertions.assertNotNull(testEntityService.getById(1L));
    }

    @Test
    public void shouldGetAllWithFiltration() {
        Filter<TestEntity> filter = like("name", "123456789");

        Sorting sorting = new Sorting();
        Pagination pagination = new Pagination();
        PageableResult<TestEntity> results = testEntityService.getPage(
                filter,
                sorting,
                pagination
        );
        System.out.println(results);
        assertEquals(1, results.getData().size());
    }


}
