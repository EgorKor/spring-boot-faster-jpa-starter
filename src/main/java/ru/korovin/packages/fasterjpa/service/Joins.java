package ru.korovin.packages.fasterjpa.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Класс представляющий набор свойств/связей которые
 * необходимо присоединить при запросе к БД.
 * При создании объекта необходимо указать множество
 * полей или цепочек полей начиная от корневой сущности запроса.
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public record Joins(Set<String> properties) {

    /**
     * Создает объект Joins на основе переданных свойств
     *
     * @param fetchingProperties набор присоединяемых свойств/связей
     * @return созданный объект Joins
     */
    public static Joins of(String... fetchingProperties) {
        return new Joins(new HashSet<>(Arrays.asList(fetchingProperties)));
    }


    /**
     * Создает объект Joins на основе переданной коллекции свойств
     *
     * @param fetchingProperties коллекция присоединяемых свойств/связей
     * @return созданный объект Joins
     */
    public static Joins of(Collection<String> fetchingProperties) {
        return new Joins(new HashSet<>(fetchingProperties));
    }


    /**
     * Создает объект Joins на основе существующего набора других объектов Joins
     *
     * @param joins набор других Joins
     * @return созданный объект Joins в котором есть все уникальные свойства из переданных объектов
     */
    public static Joins of(Joins... joins) {
        return new Joins(Arrays.stream(joins)
                .flatMap(join -> join.properties().stream())
                .collect(Collectors.toSet()));
    }


    /**
     * Создает объект Joins на основе существующей коллекции Joins
     *
     * @param joins коллекция других Joins
     * @return созданный объект Joins в котором есть все уникальные свойства из переданной коллекции объектов
     */
    public static Joins ofCollection(Collection<Joins> joins) {
        return new Joins(joins.stream()
                .flatMap(join -> join.properties().stream())
                .collect(Collectors.toSet()));
    }


    /**
     * Создает пустой объект Joins
     *
     * @return созданный пустой объект Joins
     */
    public static Joins empty() {
        return new Joins(new HashSet<>());
    }


    /**
     * Дополняет существующий объект свойствам/связями из другого объекта
     *
     * @return текущий объект дополненный новыми свойствам/связями
     */
    public Joins with(Joins joins) {
        this.properties.addAll(joins.properties);
        return this;
    }


    /**
     * Добавляет каждому свойству указанный префикс через точку
     *
     * @return текущий объект со свойствами с префиксами
     */
    public Joins withPropertyPrefix(String prefix) {
        return new Joins(properties.stream()
                .map(s -> String.format("%s.%s", prefix, s))
                .collect(Collectors.toSet()));
    }

}
