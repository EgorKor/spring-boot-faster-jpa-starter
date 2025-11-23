package ru.korovin.packages.fasterjpa.template.jpa;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import ru.korovin.packages.fasterjpa.annotations.SoftDeleteFlag;
import ru.korovin.packages.fasterjpa.exception.*;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.Pagination;
import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import ru.korovin.packages.fasterjpa.service.CrudService;
import ru.korovin.packages.fasterjpa.service.Joins;
import ru.korovin.packages.fasterjpa.service.PageableResult;
import ru.korovin.packages.fasterjpa.service.UpdateSpecification;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static ru.korovin.packages.fasterjpa.queryparam.Filter.softDeleteFilter;
import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.equal;
import static ru.korovin.packages.fasterjpa.service.UpdateSpecification.updateValue;


/**
 * Данный класс реализует интерфейс {@link CrudService}
 * используя стандарт JPA и Hibernate ORM в виде JPA провайдера.
 * Данная реализация предоставляет встроенное
 * определение поддержки мягкого удаления сущности посредством
 * использования аннотации @SoftDeleteFlag в классе сущности T,
 * оставленной над полем следующих типов данных:
 * <ul>
 *     <li>{@link Boolean}</li>
 *     <li>{@link Timestamp}</li>
 *     <li>{@link Instant}</li>
 *     <li>{@link LocalDateTime}</li>
 *     <li>{@link OffsetDateTime}</li>
 *     <li>{@link Date}</li>
 *
 * </ul>
 * <p>
 *
 * @author EgorKor
 * @version 1.0.4
 *
 * <pre>
 *     {@code
 * @Service
 * public class UserServiceImpl extends JpaCrudService<User, Long> implements UserService {
 *
 *     @Autowired
 *     public UserServiceImpl(JpaRepository<User, Long> jpaRepository, JpaSpecificationExecutor<User> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate) {
 *         super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
 *     }
 *
 * }}
 * </pre>
 * @see jakarta.persistence.PersistenceContext
 * @see SoftDeleteFlag
 * @see org.springframework.context.event.EventListener
 * @since 2025
 */
@Slf4j
public class JpaCrudService<T, ID> implements CrudService<T, ID> {

    private static final Set<Class<?>> SUPPORTED_SOFT_DELETE_TYPES = Set.of(
            Boolean.class, boolean.class,
            Timestamp.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
            Instant.class, OffsetDateTime.class, OffsetTime.class, Date.class
    );
    private static final Map<Class<?>, Supplier<Object>> SOFT_DELETE_FLAG_MAPPING
            = new HashMap<>(Map.of(
            boolean.class, () -> true,
            Boolean.class, () -> Boolean.TRUE,
            Timestamp.class, () -> Timestamp.from(Instant.now()),
            LocalDateTime.class, LocalDateTime::now,
            LocalDate.class, LocalDateTime::now,
            LocalTime.class, LocalDateTime::now,
            Instant.class, Instant::now,
            OffsetDateTime.class, OffsetDateTime::now,
            OffsetTime.class, OffsetTime::now,
            Date.class, () -> new Date(System.currentTimeMillis())
    ));
    private static final Map<Class<?>, Supplier<Object>> RESTORE_FLAG_MAPPING = Map.of(
            boolean.class, () -> false,
            Boolean.class, () -> Boolean.FALSE,
            Timestamp.class, () -> null,
            LocalDateTime.class, () -> null,
            LocalDate.class, () -> null,
            LocalTime.class, () -> null,
            Instant.class, () -> null,
            OffsetDateTime.class, () -> null,
            OffsetTime.class, () -> null,
            Date.class, () -> null
    );

    protected final EntityManager persistenceContext;
    protected final JpaRepository<T, ID> repository;
    protected final JpaSpecificationExecutor<T> specificationExecutor;
    protected final TransactionTemplate transactionTemplate;
    protected final Validator validator;
    protected final Class<T> entityType;
    protected boolean isSoftDeleteSupported;
    protected Field softDeleteField;
    protected Field idField;


    public JpaCrudService(JpaRepository<T, ID> repository,
                          JpaSpecificationExecutor<T> specificationExecutor,
                          EntityManager persistenceContext,
                          TransactionTemplate transactionTemplate,
                          Validator validator) {
        this.repository = repository;
        this.specificationExecutor = specificationExecutor;
        this.persistenceContext = persistenceContext;
        this.transactionTemplate = transactionTemplate;
        this.validator = validator;

        //initialize entity class definition
        Class<?> currentClass = getClass();
        try {
            while (currentClass.getSuperclass() != JpaCrudService.class
                    && currentClass.getSuperclass() != JpaBatchCrudService.class
                    && currentClass.getSuperclass() != JpaAsyncCrudService.class
                    && currentClass.getSuperclass() != JpaAsyncBatchService.class
                    && currentClass.getSuperclass() != Object.class) {
                currentClass = currentClass.getSuperclass();
            }
            if (currentClass.getSuperclass() == Object.class) {
                throw new IllegalStateException("Не удалось найти класс наследник JpaCrudService");
            }
            Type superclass = currentClass.getGenericSuperclass();
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            this.entityType = (Class<T>) typeArgument;
        } catch (Throwable e) {
            log.error("Не удалось определить тип сущности для класса {}", currentClass, e);
            throw e;
        }
        defineSoftDeleteSupport();
        defineIdField();
    }

    //method findById
    @Override
    public Optional<T> findById(@NonNull ID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<T> findById(@NonNull ID id,
                                @NonNull Joins joins) {
        Filter<T> baseIdFilter = equal(idField.getName(), id);
        Filter<T> resultIdFilter = getSoftDeleteSupportedFilter(baseIdFilter);
        resultIdFilter.setEntityType(entityType);
        joins.properties().forEach(resultIdFilter::withFetchJoin);
        return specificationExecutor.findOne(resultIdFilter);
    }

    @Override
    public Optional<T> findById(@NonNull ID id,
                                @NonNull LockModeType lockType) {
        return findById(id, lockType, Joins.empty());
    }

    @Override
    public Optional<T> findById(@NonNull ID id,
                                @NonNull LockModeType lockType,
                                @NonNull Joins fetchingProperties) {
        Filter<T> baseIdFilter = equal(idField.getName(), id);
        Filter<T> resultIdFilter = getSoftDeleteSupportedFilter(baseIdFilter);
        resultIdFilter.setEntityType(entityType);
        fetchingProperties.properties().forEach(resultIdFilter::withFetchJoin);
        return findByFilterWithLock(resultIdFilter, lockType);
    }

    @Override
    public T getById(@NonNull ID id) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException(getResourceNotFoundMessage(id));
        Filter<T> idFilter = equal(idField.getName(), id.toString());
        idFilter.setEntityType(entityType);
        return !isSoftDeleteSupported ?
                repository.findById(id)
                        .orElseThrow(exceptionSupplier) :
                specificationExecutor.findOne(getSoftDeleteSupportedFilter(idFilter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getById(@NonNull ID id,
                     @NonNull Joins joins) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException(getResourceNotFoundMessage(id));
        Filter<T> baseIdFilter = equal(idField.getName(), id);
        Filter<T> resultIdFilter = getSoftDeleteSupportedFilter(baseIdFilter);
        joins.properties().forEach(resultIdFilter::withFetchJoin);

        resultIdFilter.setEntityType(entityType);
        return specificationExecutor.findOne(resultIdFilter)
                .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getById(@NonNull ID id,
                     @NonNull LockModeType lockType) throws ResourceNotFoundException {
        return getById(id, lockType, Joins.empty());
    }

    @Override
    public T getById(@NonNull ID id,
                     @NonNull LockModeType lockType,
                     @NonNull Joins properties) throws ResourceNotFoundException {
        Filter<T> idFilter = equal(idField.getName(), id);
        idFilter.setEntityType(entityType);
        properties.properties().forEach(idFilter::withFetchJoin);
        return getByFilterWithLock(idFilter, lockType);
    }


    @Override
    public Optional<T> findByFilter(@NonNull Filter<T> filter) {
        filter.setEntityType(entityType);
        return specificationExecutor.findOne(getSoftDeleteSupportedFilter(filter));
    }

    @Override
    public Optional<T> findByFilterWithLock(@NonNull Filter<T> filter,
                                            @NonNull LockModeType lockType) {
        CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityType);
        Root<T> root = cq.from(entityType);
        filter.setEntityType(entityType);
        cq.select(root);
        cq.where(getSoftDeleteSupportedFilter(filter).toPredicate(root, cq, cb));
        TypedQuery<T> typedQuery = persistenceContext.createQuery(cq);
        typedQuery.setLockMode(lockType);
        return transactionTemplate.execute(status -> {
            try {
                return Optional.of(typedQuery.getSingleResult());
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public List<T> getList() {
        return getList(Filter.empty());
    }

    @Override
    public Stream<T> getDataStream() {
        return getDataStream(Filter.empty());
    }

    @Override
    public PageableResult<T> getPage(Filter<T> filter, Pagination pagination) {
        filter.setEntityType(entityType);
        return getPage(filter, Sorting.unsorted(), pagination);
    }

    @Override
    public List<T> getList(Filter<T> filter, Sorting sorting) {
        filter.setEntityType(entityType);
        return specificationExecutor.findAll(getSoftDeleteSupportedFilter(filter), sorting.toJpaSort());
    }

    @Override
    public List<T> getList(Filter<T> filter) {
        filter.setEntityType(entityType);
        return specificationExecutor.findAll(getSoftDeleteSupportedFilter(filter));
    }

    @Override
    public Stream<T> getDataStream(Filter<T> filter) {
        filter.setEntityType(entityType);
        return getDataStream(filter, Sorting.unsorted());
    }

    @Override
    public Stream<T> getDataStream(Filter<T> filter, Sorting sorting) {
        filter.setEntityType(entityType);
        CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = cb.createQuery(entityType);
        Root<T> root = criteriaQuery.from(entityType);
        criteriaQuery.select(root);
        criteriaQuery.where(getSoftDeleteSupportedFilter(filter)
                .toPredicate(root, cb));
        criteriaQuery.orderBy(sorting.toCriteriaOrderList(root, cb));
        TypedQuery<T> typedQuery = persistenceContext.createQuery(criteriaQuery);
        return typedQuery
                .setHint(HibernateHints.HINT_BATCH_FETCH_SIZE, "100")
                .setHint(HibernateHints.HINT_READ_ONLY, true)
                .getResultStream();
    }

    @Override
    public PageableResult<T> getPage(@NonNull Filter<T> filter,
                                     @NonNull Sorting sorting,
                                     @NonNull Pagination pagination) {
        filter.setEntityType(entityType);
        return PageableResult.of(specificationExecutor.findAll(getSoftDeleteSupportedFilter(filter),
                getSoftDeleteSupportedFilter(filter.copy()),
                pagination.toJpaPageable(sorting)));
    }

    @Override
    public T getByFilter(@NonNull Filter<T> filter) throws ResourceNotFoundException, NonUniqueResultException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException(getResourceNotFoundMessage(filter));
        filter.setEntityType(entityType);
        return !isSoftDeleteSupported ?
                specificationExecutor.findOne(filter)
                        .orElseThrow(exceptionSupplier) :
                specificationExecutor.findOne(getSoftDeleteSupportedFilter(filter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByFilterWithLock(@NonNull Filter<T> filter,
                                 @NonNull LockModeType lockType) throws ResourceNotFoundException {
        CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityType);
        Root<T> root = cq.from(entityType);
        filter.setEntityType(entityType);
        cq.select(root);
        cq.where(getSoftDeleteSupportedFilter(filter).toPredicate(root, cq, cb));
        TypedQuery<T> typedQuery = persistenceContext.createQuery(cq);
        typedQuery.setLockMode(lockType);
        return transactionTemplate.execute(status -> {
            try {
                return typedQuery.getSingleResult();
            } catch (NoResultException e) {
                throw new ResourceNotFoundException(getResourceNotFoundMessage(filter));
            }
        });
    }


    @Override
    public T create(@NonNull T model) throws EntityProcessingException {
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException("Ошибка валидации сущности " + entityType, violations);
        }
        return transactionTemplate.execute(status -> {
            try {
                persistenceContext.persist(model);
                return model;
            } catch (Exception e) {
                throw new EntityProcessingException("Ошибка сохранения сущности",
                        e, entityType, EntityOperation.CREATE);
            }
        });
    }

    @Override
    public List<T> createAll(List<T> models) throws EntityProcessingException {
        try {
            return transactionTemplate.execute(status -> {
                List<T> result = new ArrayList<>();
                models.forEach(model -> {
                    persistenceContext.persist(model);
                    result.add(model);
                });
                return result;
            });
        } catch (Exception e) {
            throw new EntityProcessingException("Ошибка создания списка сущностей", e, entityType, EntityOperation.CREATE);
        }
    }

    @SneakyThrows
    @Override
    public T fullUpdate(@NonNull T model) throws EntityProcessingException {
        ID id = (ID) idField.get(model);
        if (id == null) {
            throw new EntityProcessingException("Ошибка обновления сущности " + persistenceContext + ", id = null",
                    null,
                    entityType, EntityOperation.UPDATE);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException("Ошибка валидации сущности при обновлении " + entityType.getSimpleName(), violations);
        }
        return transactionTemplate.execute(status -> {
            try {
                persistenceContext.unwrap(Session.class).update(model);
                return model;
            } catch (Exception e) {
                throw new EntityProcessingException("Ошибка обновления сущности с id = " + id,
                        e, entityType, EntityOperation.CREATE);
            }
        });
    }

    @Override
    public T patchUpdate(@NonNull ID id,
                         @NonNull T model) throws EntityProcessingException {
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException("Ошибка валидации сущности " + getEntityTypeName(), violations);
        }
        T dbModel = getById(id);
        JpaEntityPropertyPatcher.patchIgnoreNulls(model, dbModel);
        return transactionTemplate.execute(status -> {
            try {
                return repository.save(dbModel);
            } catch (DataAccessException e) {
                throw new EntityProcessingException("Ошибка обновления сущности с id = " + id,
                        e, entityType, EntityOperation.CREATE);
            }
        });
    }

    @Override
    public void deleteById(@NonNull ID id) throws ResourceNotFoundException, EntityProcessingException {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                if (deleteByFilter(equal(idField.getName(), id)) != 1) {
                    throw new ResourceNotFoundException(getResourceNotFoundMessage(id));
                }
            } catch (DataAccessException e) {
                throw new EntityProcessingException("Ошибка удаления сущности с id : " + id, e, entityType, EntityOperation.DELETE);
            }
        });
    }

    @Override
    public long deleteAll() throws EntityProcessingException {
        try {
            return deleteByFilter(Filter.empty(entityType));
        } catch (Exception e) {
            throw new EntityProcessingException("Ошибка удаления всех сущностей " + getEntityTypeName(), e, entityType, EntityOperation.DELETE);
        }
    }

    @Override
    public long deleteByFilter(@NonNull Filter<T> filter) throws EntityProcessingException {
        try {
            filter.setEntityType(entityType);
            return specificationExecutor.delete(filter);
        } catch (Exception e) {
            throw new EntityProcessingException("Ошибка удаления сущностей по фильтру: " + filter, e, entityType, EntityOperation.DELETE);
        }
    }

    @Override
    public long countByFilter(@NonNull Filter<T> filter) {
        filter.setEntityType(entityType);
        return specificationExecutor.count(getSoftDeleteSupportedFilter(filter));
    }

    @Override
    public long countAll() {
        return !isSoftDeleteSupported ? repository.count() :
                countByFilter(getSoftDeleteSupportedFilter(Filter.empty()));
    }

    @Override
    public boolean existsById(@NonNull ID id) {
        return !isSoftDeleteSupported ? repository.existsById(id) :
                existsByFilter(equal(idField.getName(), id));
    }

    @Override
    public boolean existsByFilter(@NonNull Filter<T> filter) {
        filter.setEntityType(entityType);
        return specificationExecutor.exists(getSoftDeleteSupportedFilter(filter));
    }

    private void checkSoftDeleteAvailability() {
        if (!isSoftDeleteSupported) {
            throw new SoftDeleteUnsupportedException("Операция мягкого удаления не поддерживается");
        }
    }

    @SneakyThrows
    @Override
    public void softDeleteById(@NonNull ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        try {
            Object updateValue = SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get();
            transactionTemplate.executeWithoutResult(status -> {
                if (updateByFilter(
                        updateValue(softDeleteField.getName(), updateValue),
                        equal(idField.getName(), id)) != 1) {
                    throw new ResourceNotFoundException(getResourceNotFoundMessage(id));
                }
            });
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Ошибка мягкого удаления " + getEntityTypeName() + " по id: " + id,
                    e, entityType, EntityOperation.UPDATE);
        }
    }

    @Override
    public int softDeleteAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        return softDeleteByFilter(Filter.empty(entityType));
    }

    @Override
    public int updateByFilter(UpdateSpecification specification, Filter<T> filter) {
        filter.setEntityType(entityType);
        CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
        Root<T> root = update.from(entityType);

        for (Map.Entry<String, UpdateSpecification.UpdateUnit> entry :
                specification.getUpdates().entrySet()) {

            String field = entry.getKey();
            UpdateSpecification.UpdateUnit pair = entry.getValue();
            Path<Object> path = root.get(field);

            switch (pair.action()) {
                case UPDATE -> update.set(path, pair.data());
                case SUM -> {
                    if (pair.data() instanceof Number number) {
                        Object sumExpr = cb.sum(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, sumExpr);
                    }
                }
                case MULTIPLY -> {
                    if (pair.data() instanceof Number number) {
                        Object prodExpr = cb.prod(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, prodExpr);
                    }
                }
                case DIVIDE -> {
                    if (pair.data() instanceof Number number) {
                        Object quotExpr = cb.quot(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, quotExpr);
                    }
                }
                case ADD_DAYS -> {
                    if (pair.data() instanceof Integer days) {
                        if (path.getJavaType() == LocalDate.class) {
                            Object dateAddExpr = cb.function(
                                    "DATE_ADD",
                                    LocalDate.class,
                                    path,
                                    cb.literal(days)
                            );
                            update.set(path, dateAddExpr);
                        } else if (path.getJavaType() == LocalDateTime.class) {
                            Object dateTimeAddExpr = cb.function(
                                    "DATE_ADD",
                                    LocalDateTime.class,
                                    path,
                                    cb.literal(days)
                            );
                            update.set(path, dateTimeAddExpr);
                        }
                    }
                }
                case TRUNCATE_TIME -> {
                    if (path.getJavaType() == LocalDateTime.class) {
                        Object truncExpr = cb.function(
                                "TRUNC",
                                LocalDate.class,
                                path
                        );
                        update.set(path, truncExpr);
                    }
                }
                case CONCAT -> {
                    if (pair.data() instanceof String value) {
                        Object concatExpr = cb.concat(path.as(String.class), value);
                        update.set(path, concatExpr);
                    }
                }
                case UPPER_CASE -> {
                    Object upperExpr = cb.upper(path.as(String.class));
                    update.set(path, upperExpr);
                }
                case LOWER_CASE -> {
                    Object lowerExpr = cb.lower(path.as(String.class));
                    update.set(path, lowerExpr);
                }
                case COPY -> {
                    if (pair.data() instanceof String sourceField) {
                        Object sourcePath = root.get(sourceField);
                        update.set(path, sourcePath);
                    }
                }
            }
        }
        update.where(filter.toPredicate(root, cb));
        return persistenceContext.createQuery(update).executeUpdate();
    }

    @Override
    public int softDeleteByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        try {
            CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
            CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
            Root<T> root = update.from(entityType);
            update.set(root.get(softDeleteField.getName()),
                    SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get());
            if (filter.isFiltered()) {
                filter.setEntityType(entityType);
                update.where(filter.toPredicate(root, cb));
            }
            return transactionTemplate.execute(status -> persistenceContext.createQuery(update).executeUpdate());
        } catch (Exception e) {
            throw new EntityProcessingException(
                    "Неожиданная ошибка мягкого удаления сущности по фильтру: " + filter,
                    e,
                    entityType,
                    EntityOperation.UPDATE
            );
        }
    }

    @SneakyThrows
    @Override
    public void restoreById(@NonNull ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        Object updateValue = RESTORE_FLAG_MAPPING.get(softDeleteField.getType()).get();
        int updatedCount = updateByFilter(
                updateValue(softDeleteField.getName(), updateValue),
                equal(idField.getName(), id)
        );
        if (updatedCount != 1) {
            throw new ResourceNotFoundException(getResourceNotFoundMessage(id));
        }
    }

    @Override
    public void restoreAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        restoreByFilter(Filter.empty(entityType));
    }

    @Override
    public void restoreByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException, ResourceNotFoundException {
        checkSoftDeleteAvailability();
        try {
            CriteriaBuilder cb = persistenceContext.getCriteriaBuilder();
            CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
            Root<T> root = update.from(entityType);
            update.set(root.get(softDeleteField.getName()),
                    RESTORE_FLAG_MAPPING.get(softDeleteField.getType()).get());
            if (filter.isFiltered()) {
                filter.setEntityType(entityType);
                update.where(filter.toPredicate(root, cb));
            }
            transactionTemplate.executeWithoutResult(status ->
                    persistenceContext.createQuery(update).executeUpdate());
        } catch (Exception e) {
            throw new EntityProcessingException(
                    "Неожиданная ошибка восстановления по фильтру: " + filter,
                    e,
                    entityType,
                    EntityOperation.UPDATE
            );
        }
    }

    @Override
    public T getReference(@NonNull ID id) {
        return repository.getReferenceById(id);
    }

    private void defineSoftDeleteSupport() {
        if (this.entityType == null) {
            return;
        }
        List<Field> softDeleteFields = Arrays.stream(this.entityType.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(SoftDeleteFlag.class))
                .peek(field -> {
                    if (!SUPPORTED_SOFT_DELETE_TYPES.contains(field.getType())) {
                        throw new IllegalStateException(String.format(
                                "%s - поле '%s' имеет неподдерживаемый тип %s для флага мягкого удаления",
                                entityType.getName(),
                                field.getName(),
                                field.getType().getSimpleName()
                        ));
                    }
                })
                .toList();

        if (softDeleteFields.size() > 1) {
            throw new IllegalStateException(String.format(
                    "%s - поддерживается только один флаг мягкого удаления, обнаружено %d : %s",
                    entityType.getName(),
                    softDeleteFields.size(),
                    softDeleteFields
            ));
        }

        if (!softDeleteFields.isEmpty()) {
            this.isSoftDeleteSupported = true;
            this.softDeleteField = softDeleteFields.getFirst();
            this.softDeleteField.setAccessible(true);
        }
    }

    private void defineIdField() {
        this.idField = Arrays.stream(entityType.getDeclaredFields())
                .filter((f) -> f.isAnnotationPresent(Id.class))
                .findAny().orElseThrow(
                        () -> new IllegalStateException("Сущность " + getEntityTypeName() + " не имеет поля с аннотацией jakarta.persistence.@Id")
                );
        this.idField.setAccessible(true);
    }

    private Filter<T> getSoftDeleteSupportedFilter(@NonNull Filter<T> filter) {
        if (!isSoftDeleteSupported) {
            return filter;
        }
        boolean isDeleted = false;
        Filter<T> concantinatedFilter = filter._and(softDeleteFilter(softDeleteField, isDeleted));
        concantinatedFilter.setEntityType(entityType);
        return concantinatedFilter;
    }

    protected String getEntityTypeName() {
        return entityType == null ? "" : entityType.getSimpleName();
    }

    protected String getResourceNotFoundMessage(ID id) {
        return "Сущность "
                + getEntityTypeName()
                + " с id = "
                + id
                + " не найдена.";
    }

    protected String getResourceNotFoundMessage(Filter<T> filter) {
        return "Сущность "
                + getEntityTypeName()
                + " с условием: "
                + filter.getConditions()
                + " не найдена.";
    }

}
