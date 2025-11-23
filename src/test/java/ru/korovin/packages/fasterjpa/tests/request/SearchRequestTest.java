package ru.korovin.packages.fasterjpa.tests.request;

import ru.korovin.packages.fasterjpa.testProject.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.Pagination;
import ru.korovin.packages.fasterjpa.queryparam.SearchRequest;
import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterCondition;
import ru.korovin.packages.fasterjpa.queryparam.sortingInternal.SortingUnit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static ru.korovin.packages.fasterjpa.queryparam.SearchRequest.*;
import static ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterOperation.*;

public class SearchRequestTest {


    @Test
    public void testPagination() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(PAGE_PARAM, "1");
        params.add(PAGE_SIZE_PARAM, "10");
        SearchRequest searchRequest = new SearchRequest(params);
        Pagination pagination = searchRequest.getPagination();
        assertNotNull(pagination);
        assertTrue(pagination.isPaged());
        assertEquals(10, pagination.getSize());
        assertEquals(1, pagination.getPage());
    }

    @Test
    public void testSorting() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SORT_PARAM, "id");
        params.add(SORT_PARAM, "name:desc");
        SearchRequest searchRequest = new SearchRequest(params);
        Sorting sorting = searchRequest.getSorting();
        assertIterableEquals(List.of(
                new SortingUnit("id", "asc"),
                new SortingUnit("name", "desc")), sorting.getSort());
    }

    @Test
    public void testFilter() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Egor");
        params.add("name", "like:Egor");
        params.add("name", "not_like:Egor");
        params.add("name", "is:null");
        params.add("name", "is_not:null");
        params.add("name", "in:Eg;or;ic");
        params.add("name", "is:true");
        params.add("name", "is_not:true");
        params.add("name", "is:false");
        params.add("name", "is_not:false");
        params.add("name.length()", "gt:10");
        params.add("name.length()", "ge:10");
        params.add("name.length()", "lt:10");
        params.add("name.length()", "le:10");
        params.add("name.length()", "not_equals:10");
        SearchRequest searchRequest = new SearchRequest(params);
        Filter filter = searchRequest.getFilter();
        assertIterableEquals(List.of(
                        new FilterCondition("name", EQUALS, "Egor"),
                        new FilterCondition("name", LIKE, "Egor"),
                        new FilterCondition("name", NOT_LIKE, "Egor"),
                        new FilterCondition("name", IS, "null"),
                        new FilterCondition("name", IS_NOT, "null"),
                        new FilterCondition("name", IN, List.of("Eg","or","ic")),
                        new FilterCondition("name", IS, "true"),
                        new FilterCondition("name", IS_NOT, "true"),
                        new FilterCondition("name", IS, "false"),
                        new FilterCondition("name", IS_NOT, "false"),
                        new FilterCondition("name.length()", GT, "10"),
                        new FilterCondition("name.length()", GTE, "10"),
                        new FilterCondition("name.length()", LS, "10"),
                        new FilterCondition("name.length()", LSE, "10"),
                        new FilterCondition("name.length()", NOT_EQUALS, "10")),
                filter.getConditions());
    }

    @Test
    public void testSortDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SORT_PARAM, "id");
        params.add(SORT_PARAM, "name:desc");
        assertThrows(InvalidParameterException.class, () -> {
            new SearchRequest(params, Filter.class, SortParams.class, Map.of());
        });
    }

    @Test
    public void testFilterDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Egor");
        params.add("value", "like:Egor");
        assertThrows(InvalidParameterException.class, () -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .params(params)
                    .filterClass(FilterParams.class)
                    .build();
        });
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class FilterParams extends Filter<User> {
        private String name;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SortParams extends Sorting {
        private String name;

    }

}
