package ru.korovin.packages.fasterjpa.service.mapping;

@FunctionalInterface
public interface ProjectionRowMapper<P> {

    P mapRow(ProjectionMappingContext mapping);

}
