package ru.korovin.packages.fasterjpa.queryparam;

import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterConditionsSearcher;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterValidator;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterCountConditionsVisitor;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterListConditionsVisitor;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterPropertyIndexVisitor;
import ru.korovin.packages.fasterjpa.service.Joins;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 *
 * @author EgorKor
 * @version 1.0.4
 * @since 2025
 */
//TODO: добавить поддержку операций работы с JSON
//TODO: добавить поддержку функций size() length() для SQL
//TODO: реализовать метод обновления по фильтру
@Slf4j
@Setter
@Getter
public class Filter<T> implements Specification<T> {
    /**
     * Класс сущности для которой происходит выборка
     */
    protected Class<?> entityType;

    /**
     * Флаг уникальности выбираемых значений
     */
    protected boolean isDistinct;

    /**
     * Список конфигураторов запроса, запускаются только при запуске запроса
     */
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();

    /**
     * Множество разрешенных свойств не подлежащих проверкам и валидации.
     * Необходим для дополнения клиентских запросов с серверной стороны без проверки
     * этого дополнения.
     */
    protected Set<String> propertiesWhiteList = new HashSet<>();

    /**
     * Множество подгружаемых (через join) связей/свойств
     */
    protected Set<String> fetchingProperties = new HashSet<>();

    protected Map<String, Fetch<?, ?>> fetches = new HashMap<>();

    /**
     * Условие фильтрации
     */
    protected FilterConditionTreeNode filterCondition;

    /**
     * Карта исходных условий фильтрации до применения альясов
     */
    protected Map<String, Set<FilterCondition>> conditionsWithNoMappedFields;

    /**
     * Создает пустой фильтр с попыткой автоматического
     * определения класса сущности
     */
    public Filter() {
        this.filterCondition = new FilterEmptyCondition();
        determineEntityType();
    }

    /**
     * Создает пустой фильтр с конкретным классом сущности
     *
     * @param entityType класс сущности
     */
    public Filter(Class<?> entityType) {
        this.filterCondition = new FilterEmptyCondition();
        this.entityType = entityType;
    }

    /**
     * Создает фильтр с конкретным условием
     *
     * @param filterCondition условие фильтрации
     */
    public Filter(@NonNull FilterConditionTreeNode filterCondition) {
        this.filterCondition = filterCondition;
        determineEntityType();
    }

    /**
     * Создает фильтр с конкретным условием и классом сущности
     *
     * @param filterCondition условие фильтрации
     * @param entityType      тип сущности
     */
    public Filter(@NonNull FilterConditionTreeNode filterCondition,
                  @NonNull Class<?> entityType) {
        this.filterCondition = filterCondition;
        this.entityType = entityType;
    }

    /**
     * Создает копию текущего фильтра, условия также полностью копируются
     *
     * @return копия текущего фильтра
     *
     */
    @SneakyThrows
    public <R extends Filter<?>> R copy() {
        R copiedFilter = (R) this.getClass().getDeclaredConstructor().newInstance();
        copiedFilter.setEntityType(entityType);
        copiedFilter.setPropertiesWhiteList(propertiesWhiteList);
        copiedFilter.setFilterCondition(filterCondition.copy());
        copiedFilter.setDistinct(isDistinct);
        return copiedFilter;
    }

    //region STATE_GET_METHODS

    /**
     * Проверка наличия условия в фильтре
     */
    public boolean isFiltered() {
        return filterCondition != null && !(filterCondition instanceof FilterEmptyCondition);
    }

    /**
     * Проверка безусловности фильтра
     *
     */
    public boolean isUnfiltered() {
        return filterCondition == null || filterCondition instanceof FilterEmptyCondition;
    }


    /**/
    public int getConditionsCount() {
        return filterCondition.visitWith(new FilterCountConditionsVisitor());
    }

    /**/
    public List<FilterCondition> getConditions() {
        return filterCondition.visitWith(new FilterListConditionsVisitor());
    }
    //endregion

    //region Criteria API Mapping
    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        query.distinct(isDistinct);
        return toPredicate(root, cb);
    }

    public Predicate toPredicate(Root<T> root,
                                 CriteriaBuilder cb) {
        //конфигурация запроса
        queryConfigurers.forEach(c -> c.accept(root));
        filterCondition.setFilter(this);
        return filterCondition.parsePredicate(root, null, cb, entityType);
    }

    //region QUERY_CONDITION_MODIFICATION_METHODS

    /**
     * Метод добавления условия НЕ к текущего условию
     * запроса
     *
     * @return текущий объект с измененным условием
     */
    public <R extends Filter<?>> R not() {
        this.filterCondition = new FilterNotCondition(
                this.filterCondition
        );
        return _this();
    }

    /**
     * Метод объединения условия текущего фильтра
     * и другого условия через И
     *
     * @param condition внешнее условие
     * @return текущий объект с измененным условием
     */
    public <R extends Filter<?>> R andCondition(FilterConditionTreeNode condition) {
        return andFilter(condition.toFilter());
    }

    /**
     * Метод объединения текущего фильтра и другого
     * фильтра через И
     *
     * @param externalFilter внешний фильтр
     * @return текущий объект с измененным условием
     */
    public <R extends Filter<?>> R andFilter(Filter<?> externalFilter) {
        this.initializeOriginalNamesMap();
        this.filterCondition = new FilterAndCondition(
                List.of(
                        filterCondition,
                        externalFilter.getFilterCondition()
                )
        );
        this.propertiesWhiteList.addAll(
                externalFilter.getFilterCondition()
                        .visitWith(new FilterListConditionsVisitor())
                        .stream()
                        .map(FilterCondition::property)
                        .toList()
        );
        if (externalFilter.conditionsWithNoMappedFields != null) {
            externalFilter.conditionsWithNoMappedFields.forEach(
                    (field, filters) -> {
                        if (this.conditionsWithNoMappedFields.containsKey(field)) {
                            this.conditionsWithNoMappedFields.get(field).addAll(filters);
                        } else {
                            this.conditionsWithNoMappedFields.put(field, filters);
                        }
                    }
            );
        }
        return _this();
    }

    @SneakyThrows
    public <R extends Filter<?>> R toDerivedFilter(Class<R> derivedClass) {
        R derivedFilter = (R) derivedClass.getDeclaredConstructor().newInstance();
        derivedFilter.setEntityType(this.getEntityType());
        derivedFilter.setFilterCondition(this.getFilterCondition());
        derivedFilter.setDistinct(this.isDistinct());
        derivedFilter.setPropertiesWhiteList(this.getPropertiesWhiteList());
        derivedFilter.setFetchingProperties(this.getFetchingProperties());
        derivedFilter.setQueryConfigurers(new ArrayList<>());
        this.queryConfigurers.forEach(c -> {
            derivedFilter.queryConfigurers.add(
                    s -> c.accept((Root<T>) s)
            );
        });
        derivedFilter.setConditionsWithNoMappedFields(this.getConditionsWithNoMappedFields());
        return derivedFilter;
    }

    /**
     * Метод объединения условия текущего фильтра
     * и другого условия через ИЛИ
     *
     * @param condition внешнее условие
     * @return текущий объект с измененным условием
     */
    public <R extends Filter<?>> R orCondition(FilterConditionTreeNode condition) {
        return orFilter(condition.toFilter());
    }

    /**
     * Метод объединения текущего фильтра
     * и внешнего фильтра через ИЛИ
     *
     * @param externalFilter внешний фильтр
     * @return текущий объект с измененным условием
     */
    public <R extends Filter<?>> R orFilter(Filter<?> externalFilter) {
        this.initializeOriginalNamesMap();
        this.filterCondition = new FilterOrCondition(
                List.of(
                        filterCondition,
                        externalFilter.getFilterCondition()
                )
        );
        this.propertiesWhiteList.addAll(
                externalFilter.getFilterCondition()
                        .visitWith(new FilterListConditionsVisitor())
                        .stream()
                        .map(FilterCondition::property)
                        .toList()
        );
        if (externalFilter.conditionsWithNoMappedFields != null) {
            externalFilter.conditionsWithNoMappedFields.forEach(
                    (field, filters) -> {
                        if (this.conditionsWithNoMappedFields.containsKey(field)) {
                            this.conditionsWithNoMappedFields.get(field).addAll(filters);
                        } else {
                            this.conditionsWithNoMappedFields.put(field, filters);
                        }
                    }
            );
        }
        return _this();
    }
    //endregion

    //region QUERY_CONFIGURATION

    /**
     * Метод конфигурации запроса к которому будет применен фильтр
     *
     * @param queryConfigurer конфигуратор запроса
     * @return текущий объект с добавленным конфигуратором
     */
    public <R extends Filter<?>> R configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    /**
     * Метод установки флага уникальности выборки
     * в запросе в котором будет применен фильтра
     *
     * @return текущий объект с измененным флагом уникальности выборки
     */
    public <R extends Filter<?>> R distinct() {
        this.isDistinct = true;
        return _this();
    }

    /**
     * Метод для добавления свойства/связи которую необходимо
     * подгрузить (через join)
     *
     * @return текущий объект с добавленным свойством для подгрузки
     */
    public <R extends Filter<?>> R withFetchJoin(String fetchingProperty) {
        this.fetchingProperties.add(fetchingProperty);
        queryConfigurers.add((root) -> {
            String[] attributes = fetchingProperty.split("\\.");
            FetchParent<?, ?> currentParent = root;
            String mappingAttribute = attributes[0];
            for (String attribute : attributes) {
                if (mappingAttribute.contains(".")) {
                    mappingAttribute += attribute;
                }
                currentParent = currentParent.fetch(attribute, JoinType.LEFT);
                fetches.put(mappingAttribute, (Fetch<?, ?>) currentParent);
                mappingAttribute += ".";
            }
        });
        return _this();
    }

    /**
     * Метод для добавления свойств/связей которую
     * необходимо подгрузить (через join)
     *
     * @return текущий объект с добавленными свойствами для подгрузки
     */
    public <R extends Filter<?>> R withFetchJoins(Joins joins) {
        this.fetchingProperties.addAll(joins.properties());
        joins.properties().forEach(this::withFetchJoin);
        return _this();
    }

    public Fetch<?, ?> getFetchAttribute(String attribute) {
        return fetches.get(attribute);
    }

    //endregion


    //region Utility Methods

    private void determineEntityType() {
        if (getClass() == Filter.class) {
            return;
        }
        try {
            Type superclass = getClass().getGenericSuperclass();
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            this.entityType = typeArgument.getClass();
        } catch (Exception e) {
            log.warn("Cannot determine entity type", e);
        }
    }

    private <SameType extends Filter<?>> SameType _this() {
        return (SameType) this;
    }

    public FilterValidator validator() {
        return new FilterValidator(this, this::initializeOriginalNamesMap);
    }

    public FilterConditionsSearcher searcher() {
        return new FilterConditionsSearcher(this, this::initializeOriginalNamesMap);
    }

    private void initializeOriginalNamesMap() {
        if (this.conditionsWithNoMappedFields == null) {
            this.conditionsWithNoMappedFields = filterCondition.visitWith(
                    new FilterPropertyIndexVisitor()
            );
        }
    }

    /**
     * Метод определения вызывает ли текущий
     * метод объект класса наследника
     *
     */
    public boolean isCalledByInheritor() {
        return this.getClass() == Filter.class;
    }

    //endregion


}
