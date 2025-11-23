package ru.korovin.packages.fasterjpa.queryparam;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static ru.korovin.packages.fasterjpa.queryparam.factories.Paginations.*;

/**
 * Параметр запроса для пагинации запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam Pagination pagination)
 * }</pre>
 * Пример использования с JPA репозиториями сортировка и пагинация
 * <pre>
 * {@code
 * public List<Entity> query(SortParams sort, Pagination pagination){
 *     repository.findAll(pagination.toJpaPageable(sort));
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {
    public static final String MAX_CONSTRAINT_VIOLATION_MESSAGE = "Превышен максимальный размер страницы, текущий размер %d, максимальный размер %d";
    public static final String MIN_CONSTRAINT_VIOLATION_MESSAGE = "Нарушен минимальный размер страницы, текущий размер %d, максимальный размер %d";
    public static final String ILLEGAL_SIZE_CONSTRAINT_VALUE_MESSAGE = "Ограничение размера страницы не может быть меньше или равно нулю";
    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_PAGE_SIZE;

    //state checking methods
    public boolean isUnpaged() {
        return size == ALL_CONTENT_SIZE;
    }

    public boolean isPaged() {
        return size != ALL_CONTENT_SIZE;
    }

    //transform methods
    public Pageable toJpaPageable() {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged();
        }
        return PageRequest.of(page, size);
    }

    public Pageable toJpaPageable(Sort sort) {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged(sort);
        }
        return PageRequest.of(page, size, sort);
    }

    public Pageable toJpaPageable(Sorting sorting) {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged(sorting.toJpaSort());
        }
        return PageRequest.of(page, size, sorting.toJpaSort());
    }

    //default params method
    public Pagination withDefault(int page, int size) {
        if (isUnpaged()) {
            this.page = page;
            this.size = size;
        }
        return this;
    }

    //constraint methods
    public Pagination withMaxSizeConstraint(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(ILLEGAL_SIZE_CONSTRAINT_VALUE_MESSAGE);
        }
        if(this.size > maxSize){
            throw new IllegalStateException(String.format(MAX_CONSTRAINT_VIOLATION_MESSAGE, this.size, maxSize));
        }
        return this;
    }

    public Pagination withMinSizeConstraint(int minSize) {
        if (minSize <= 0) {
            throw new IllegalStateException(ILLEGAL_SIZE_CONSTRAINT_VALUE_MESSAGE);
        }
        if(this.size < minSize){
            throw new IllegalStateException(String.format(MAX_CONSTRAINT_VIOLATION_MESSAGE, this.size, minSize));
        }
        return this;
    }

}
