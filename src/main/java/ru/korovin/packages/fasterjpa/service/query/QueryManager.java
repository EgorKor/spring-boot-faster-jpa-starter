package ru.korovin.packages.fasterjpa.service.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static ru.korovin.packages.fasterjpa.service.query.QueryManager.QuerySelection.resultType;
import static ru.korovin.packages.fasterjpa.service.query.QueryManager.QuerySelection.select;

@RequiredArgsConstructor
public class QueryManager {
    private final EntityManager persistenceContext;

    public <T> TypedQuery<T> query(QuerySelection<T> querySelection) {
        CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(querySelection.resultType());
        AtomicReference<Root<T>> root = new AtomicReference<>(null);


        return persistenceContext.createQuery(query);
    }


    public record From(Class<?> entityClass) implements QueryElement {

        @Override
        public void renderQuery(CriteriaBuilder cb, CriteriaQuery<?> query, AtomicReference<Root<?>> root) {
            root.set(query.from(entityClass));
        }

        public static From from(Class<?> entityClass){
            return new From(entityClass);
        }
    }

    public record Where(QueryCondition queryCondition) implements QueryElement {

        @Override
        public void renderQuery(CriteriaBuilder cb, CriteriaQuery<?> query, AtomicReference<Root<?>> root) {
            query.where();
        }
    }

    public record QueryCondition(Predicate predicate) {

    }

    public record QuerySelection<T>(Class<T> resultType,
                                    List<QueryElement> queryElements) {

        public static <T> QuerySelection<T> select(Class<T> resultType, List<QueryElement> queryElements) {
            return new QuerySelection<>(resultType, queryElements);
        }

        public static <T> Class<T> resultType(Class<T> resultType) {
            return resultType;
        }
    }


}
