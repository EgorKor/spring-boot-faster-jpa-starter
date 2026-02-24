package ru.korovin.packages.fasterjpa.service;

import jakarta.persistence.LockModeType;
import jakarta.persistence.NonUniqueResultException;
import ru.korovin.packages.fasterjpa.exception.EntityProcessingException;
import ru.korovin.packages.fasterjpa.exception.ResourceNotFoundException;
import ru.korovin.packages.fasterjpa.exception.SoftDeleteUnsupportedException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.Pagination;
import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import ru.korovin.packages.fasterjpa.service.mapping.ProjectionRowMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Интерфейс базового CRUD параметризованного сервиса. Предоставляет
 * удобный и широкий набор операций для работы с СУБД:<p>
 * 1. Выборка данных - с фильтрацией, пагинацией, сортировкой;<p>
 * 2. Обновление данных - полное (full) и частичное (patch);<p>
 * 3. Создание записи;<p>
 * 4. Физическое удаление;<p>
 * 5. Мягкое удаление;<p>
 * 6. Выборка отдельных атрибутов;<p>
 * 7. Подсчет кол-ва записей;<p>
 *
 * @param <T>  Тип сущности
 * @param <ID> Тип идентификатора
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public interface CrudService<T, ID> {
    /**
     * Запрос на получение страницы сущностей с учётом фильтрации, сортировки, пагинации
     *
     * @param sorting    параметр запроса сортировки
     * @param filter     параметр запроса фильтрации
     * @param pagination параметр запроса постраничного доступа
     * @return PageableResult - результат постраничного запроса к БД, содержащий данные
     * и параметры страниц
     */
    PageableResult<T> getPage(Filter<T> filter, Sorting sorting, Pagination pagination);

    /**
     * Запрос на получение страницы сущностей с учётом фильтрации и пагинации
     *
     * @param filter     параметр запроса фильтрации
     * @param pagination параметр запроса постраничного доступа
     * @return PageableResult - результат постраничного запроса к БД, содержащий данные
     * и параметры страниц
     */
    PageableResult<T> getPage(Filter<T> filter, Pagination pagination);

    /**
     * Запрос на получение полного списка сущностей
     *
     * @return List типа T - результат запроса к БД, содержащий данные
     */
    List<T> getList();

    /**
     * Запрос на получение полного списка сущностей с
     * присоединением указанных связей
     *
     * @return List типа T - результат запроса к БД, содержащий данные
     */
    List<T> getList(Joins joins);

    /**
     * Запрос на получение списка сущностей с учётом фильтрации
     *
     * @param filter параметр запроса фильтрации
     * @return List типа T - результат запроса к БД, содержащий данные
     */
    List<T> getList(Filter<T> filter);

    /**
     * Запрос на получение списка сущностей с учётом фильтрации и сортировки
     *
     * @param sorting параметр запроса сортировки
     * @param filter  параметр запроса фильтрации
     * @return List типа T - результат запроса к БД, содержащий данные
     */
    List<T> getList(Filter<T> filter, Sorting sorting);

    /**
     * Запрос на получение потока данных
     *
     * @return Stream типа T - поток данных сущностей из БД
     */
    Stream<T> getDataStream();

    /**
     * Запрос на получение потока данных с учётом фильтрации.
     * В потоке запрашиваются все данные, но выгрузка происходит
     * частично, в отличие от методов getAll - которые выгружают
     * сразу весь список. Рекомендуется использовать его при обработке
     * больших объемов данных.
     *
     * @param filter параметр запроса фильтрации
     * @return Stream типа T - поток данных сущностей из БД
     */
    Stream<T> getDataStream(Filter<T> filter);

    /**
     * Запрос на получение потока данных с учётом фильтрации
     * и сортировки
     *
     * @param sorting параметр запроса сортировки
     * @param filter  параметр запроса фильтрации
     * @return Stream типа T - поток данных сущностей из БД
     * @see #getDataStream(Filter)
     */
    Stream<T> getDataStream(Filter<T> filter, Sorting sorting);

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id идентификатор сущности
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getById(ID id) throws ResourceNotFoundException;

    /**
     * Запрос на получение записи по идентификатору
     *
     * @param id идентификатор сущности
     * @return Optional обертка над записью
     */
    Optional<T> findById(ID id);

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id                 идентификатор сущности
     * @param fetchingProperties присоединяемые свойства/связи при запросе
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getById(ID id, Joins fetchingProperties) throws ResourceNotFoundException;

    /**
     * Запрос на получение сущности по идентификатору. Оборачивает
     * результат в Optional для контроля обработки отсутствия значения на клиентской стороне.
     *
     * @param id                 идентификатор сущности
     * @param fetchingProperties присоединяемые свойства/связи при запросе
     * @return Optional обертка над записью
     */
    Optional<T> findById(ID id, Joins fetchingProperties);

    /**
     * Запрос на получение сущности по идентификатору с возможностью блокировки
     * записи на уровне базы данных
     *
     * @param id       идентификатор сущности
     * @param lockType параметр блокировки в БД
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getById(ID id, LockModeType lockType) throws ResourceNotFoundException;

    /**
     * Запрос на получение сущности по идентификатору и блокировкой записи
     *
     * @param id       идентификатор сущности
     * @param lockType тип блокировки
     * @return Optional обертка над записью
     */
    Optional<T> findById(ID id, LockModeType lockType);

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id                 идентификатор сущности
     * @param lockType           тип блокировки
     * @param fetchingProperties присоединяемые свойства/связи
     * @return запись из БД
     * @throws ResourceNotFoundException если запись не найдена
     */
    T getById(ID id, LockModeType lockType, Joins fetchingProperties) throws ResourceNotFoundException;

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id                 идентификатор сущности
     * @param lockType           тип блокировки
     * @param fetchingProperties присоединяемые свойства/связи
     * @return Optional обертка над записью
     */
    Optional<T> findById(ID id, LockModeType lockType, Joins fetchingProperties);

    /**
     * Запрос на получение одной сущности с применением условий из фильтра.
     * Находимая сущность по данному фильтру должна быть уникальна
     *
     * @param filter параметр запроса фильтрации
     * @return объект Т - удовлетворяющий условиям из фильтра
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности удовлетворяющий
     *                                   условиям из фильтра
     */
    T getByFilter(Filter<T> filter) throws ResourceNotFoundException, NonUniqueResultException;

    /**
     *
     */
    Optional<T> findByFilter(Filter<T> filter);

    /**
     * Запрос на получение одной сущности с применением условий из фильтра
     * с возможностью блокировки записи на уровне базы данных
     *
     * @param filter   параметр запроса фильтрации
     * @param lockType параметр блокировки в БД
     * @return объект Т - удовлетворяющий условиям из фильтра
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности удовлетворяющий
     *                                   условиям из фильтра
     */
    T getByFilterWithLock(Filter<T> filter, LockModeType lockType) throws ResourceNotFoundException;

    /**
     * Запрос на получение записи по фильтру с блокировкой
     *
     * @param filter фильтр выборки
     * @param lockType тип блокировки
     * @return Optional обертка над записью
     */
    Optional<T> findByFilterWithLock(Filter<T> filter, LockModeType lockType);

    /**
     * Создание (POST) сущности в БД
     *
     * @return объект сущности после сохранения в БД - модифицированный
     */
    T create(T model) throws EntityProcessingException;

    /**
     * Создание (POST) списка сущностей в БД
     *
     * @return список объектов сущностей после сохранение в БД - новый
     */
    List<T> createAll(List<T> models) throws EntityProcessingException;

    /**
     * Полное (PUT) обновление сущности на основе переданной модели, переписывает все поля оригинальной сущности
     *
     * @param model объект сущности
     * @return объект сущности после обновления в БД
     */
    T fullUpdate(T model) throws EntityProcessingException;

    /**
     * Частичное (PATCH) обновление сущности на основе переданной модели, обновляет только не null
     * поля, которые отличаются от оригинальных.
     *
     * @param id    идентификатор сущности
     * @param model объект сущности
     * @return объект сущности после обновления в БД
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с указанным id
     */
    T patchUpdate(ID id, T model) throws ResourceNotFoundException, EntityProcessingException;

    /**
     * Массовое обновление по условию
     *
     * @param specification спецификация обновления в которой указан список изменений
     * @param filter        фильтр записей, которые должны быть обновлены
     * @return int число записей которые были обновлены
     */
    int updateByFilter(UpdateSpecification specification, Filter<T> filter);

    /**
     * Физическое удаление сущности по ID
     *
     * @param id идентификатор сущности
     */
    void deleteById(ID id) throws ResourceNotFoundException, EntityProcessingException;

    /**
     * Физическое удаление всех сущностей
     *
     * @return количество удаленных записей
     */
    long deleteAll() throws EntityProcessingException;

    /**
     * Физическое удаление всех сущностей с учётом фильтрации
     *
     * @param filter параметр запроса фильтрации
     * @return количество удаленных записей
     */
    long deleteByFilter(Filter<T> filter) throws EntityProcessingException;

    /**
     * Кол-во сущностей с учётом фильтрации
     *
     * @param filter параметр запроса фильтрации
     * @return общее кол-во сущностей в БД удовлетворяющих условиям фильтра
     */
    long countByFilter(Filter<T> filter);

    /**
     * Кол-во сущностей
     *
     * @return общее кол-во сущностей в БД
     */
    long countAll();

    /**
     * Проверка существования сущности по ID
     *
     * @param id идентификатор сущности
     * @return true - если сущность существует с указанным id
     */
    boolean existsById(ID id);

    /**
     * Проверка существования сущности по условию из фильтра
     *
     * @param filter - параметр запроса с фильтрацией
     * @return true - если сущность существует удовлетворяющая условиям фильтра
     */
    boolean existsByFilter(Filter<T> filter);

    /**
     * Мягкое удаление по ID
     *
     * @param id идентификатор сущности
     * @throws ResourceNotFoundException      если сущности с указанным id не существует в БД
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void softDeleteById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Мягкое удаление всех сущностей
     *
     * @return количество обновлений
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    int softDeleteAll() throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Мягкое удаление всех по условию
     *
     * @param filter параметр запроса
     * @return количество обновлений
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    int softDeleteByFilter(Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление после мягкого удаления по ID
     *
     * @param id идентификатор сущности
     * @throws ResourceNotFoundException      если сущности с указанным id не существует в БД
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление всех записей
     *
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreAll() throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление всех записей с учётом условий фильтрации
     *
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreByFilter(Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Получение ссылки на объект БД без загрузки объекта
     * и проверки существования записи в БД.
     *
     * @param id идентификатор сущности
     * @return ссылка на сущность
     */
    T getReference(ID id);

    /**
     * Получение проекции через выборку определенных атрибутов
     *
     * @param <P>        тип проекции
     * @param attributes список наименований атрибутов выборки
     * @param filter     фильтр выборки
     * @param rowMapper  функция преобразователь результата выборки в проекцию
     * @return одна запись типа P
     */
    <P> P getAttributesProjection(List<String> attributes, Filter<T> filter, ProjectionRowMapper<P> rowMapper);

    /**
     * Получение проекции через выборку определенных атрибутов
     *
     * @param <P>        тип проекции
     * @param attributes список наименований атрибутов выборки
     * @param filter     фильтр выборки
     * @param rowMapper  функция преобразователь результата выборки в проекцию
     * @return список записей типа P
     */
    <P> List<P> getAttributesProjectionList(List<String> attributes, Filter<T> filter, ProjectionRowMapper<P> rowMapper);

    /**
     * Получение проекции через выборку определенных атрибутов
     *
     * @param <P>        тип проекции
     * @param attributes список наименований атрибутов выборки
     * @param filter     фильтр выборки
     * @param sorting    сортировка выборки
     * @param rowMapper  функция преобразователь результата выборки в проекцию
     * @return список записей типа P
     */
    <P> List<P> getAttributesProjectionList(List<String> attributes, Filter<T> filter, Sorting sorting, ProjectionRowMapper<P> rowMapper);

    /**
     * Очистка кэша первого уровня. Перед очисткой выполняйте
     * flushQueries чтобы не потерять изменения.
     */
    void clearL1Cache();

    /**
     * Отправка накопленных изменений в виде запросов
     * непосредственно в БД.
     */
    void flushQueries();
}
