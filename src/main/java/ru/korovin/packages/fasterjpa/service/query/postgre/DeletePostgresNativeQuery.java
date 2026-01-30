package ru.korovin.packages.fasterjpa.service.query.postgre;

import jakarta.persistence.EntityManager;
import lombok.NonNull;

public class DeletePostgresNativeQuery {
    private final EntityManager persistenceContext;
    private String tableName;
    private String condition;
    private boolean cascade;

    private final static String QUERY_TEMPLATE = """
            delete from ${TABLE_NAME}
            ${WHERE_CONDITION}
            ${CASCADE}
            """;

    private DeletePostgresNativeQuery(EntityManager persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    public DeletePostgresNativeQuery deleteFrom(String table) {
        this.tableName = table;
        return this;
    }

    public DeletePostgresNativeQuery where(String condition) {
        this.condition = condition;
        return this;
    }

    public DeletePostgresNativeQuery cascade() {
        this.cascade = true;
        return this;
    }

    private String buildQuery() {
        String whereCondition = condition != null ? String.format(
                "where %s",
                condition
        ) : "";
        String cascade = this.cascade ? "cascade" : "";
        if (tableName == null) {
            throw new IllegalStateException("Необходимо указать имя таблицы в запросе");
        }
        return QUERY_TEMPLATE
                .replace("${TABLE_NAME}", tableName)
                .replace("${WHERE_CONDITION}", whereCondition)
                .replace("${CASCADE}", cascade);
    }

    public void execute() {
        String query = buildQuery();
        persistenceContext.createNativeQuery(query).executeUpdate();
    }


    public static DeletePostgresNativeQuery createWithPersistenceContext(@NonNull EntityManager persistenceContext) {
        return new DeletePostgresNativeQuery(persistenceContext);
    }

}
