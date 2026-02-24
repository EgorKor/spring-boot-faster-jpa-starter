package ru.korovin.packages.fasterjpa.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * PageableResult - класс обёртка для результата запроса с учётом пагинации.
 * Пример оборачивания результата запроса в PageableResult:
 * <pre>
 *     {@code
 *     public PageableResult<List<User>> getAll(Filter filter, Pagination pagination){
 *         return PageableResult.of(userRepository.findAll(pagination.toJpaPageable(),
 *            filter.toJpaFilter()), userRepository.count(filter.toJpaFilter()), pagination.getPageSize());
 *     }
 *     }
 * </pre>
 *
 * @param <T> Тип записей в обертке
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
@AllArgsConstructor
@ToString
public class PageableResult<T> {
    /**
     * Данные полученные в результате запроса
     */
    private List<T> data;

    /**
     * Общее кол-ва записей без разбиения на страницы
     */
    private long count;

    /**
     * Общее кол-во страниц заданного размера
     */
    private long pageCount;

    /**
     * Размер страницы
     */
    private long pageSize;

    /**
     * Создает объект обертки на основе объекта {@link Page}
     * @param page результат запроса к БД
     * @return PageableResult - обертка над страницей результатом запроса к БД
     */
    public static <T> PageableResult<T> of(Page<T> page) {
        return of(page.stream().toList(), page.getTotalElements(), page.getTotalPages(), page.getSize());
    }

    /**
     * Создает объект обертки используя конструктор со всеми параметрами
     *
     * @param data результат запроса к БД
     * @param totalElements всего элементов удовлетворяющих запросу без пагинации
     * @param pageCount кол-во страниц
     * @param pageSize размер страницы
     * @return PageableResult - обертка над страницей результатом запроса к БД
     */
    public static <T> PageableResult<T> of(List<T> data, long totalElements, long pageCount, long pageSize) {
        return new PageableResult<>(data, totalElements, pageCount, pageSize);
    }

    /**
     * Создает объект обертки используя конструктор и метод расчета кол-ва страниц
     * исходя из кол-ва элементов и размера страницы
     *
     * @param data результат запроса к БД
     * @param totalElements всего элементов удовлетворяющих запросу без пагинации
     * @param pageSize размер страницы
     * @return PageableResult - обертка над страницей результатом запроса к БД
     */
    public static <T> PageableResult<T> of(List<T> data, long totalElements, long pageSize) {
        return new PageableResult<>(data, totalElements, countPages(totalElements, pageSize), pageSize);
    }

    /**
     * Рассчитывает кол-во страниц исходя из общего кол-ва
     * элементов и размера страницы
     *
     * @param totalElements всего элементов
     * @param pageSize размер страницы
     * @return кол-во страниц с учетом деления с остатком и округления вверх
     */
    public static long countPages(long totalElements, long pageSize) {
        return Math.ceilDiv(totalElements, pageSize);
    }

    /**
     * Преобразует все элементы данных обертки используя функцию маппер
     *
     * @param mapper функция преобразователь содержимого
     * @return PageableResult копия обертки с преобразованными элементами данными
     */
    public <R> PageableResult<R> map(Function<? super T, R> mapper) {
        return new PageableResult<>(data.stream().map(mapper).toList(), count, pageCount, pageSize);
    }
}
