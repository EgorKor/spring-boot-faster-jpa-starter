package ru.korovin.packages.fasterjpa.service.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.concurrent.atomic.AtomicReference;

public interface QueryElement {
    void renderQuery(CriteriaBuilder cb, CriteriaQuery<?> query, AtomicReference<Root<?>> root);
}
