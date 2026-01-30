package ru.korovin.packages.fasterjpa.service.query.conditioning;

import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import ru.korovin.packages.fasterjpa.service.query.postgre.DeletePostgresNativeQuery;

import java.util.Arrays;
import java.util.Collection;

public class SqlOps {
    public static String and(String... conditions) {
        return "(" + String.join(" AND ", conditions) + ")";
    }

    public static String or(String... conditions) {
        return "(" + String.join(" OR ", conditions) + ")";
    }

    public static String not(String condition) {
        return "NOT (" + condition + ")";
    }

    public static <T> String notEquals(String leftExpression, T rightExpression) {
        return leftExpression + " != " + rightExpression;
    }

    public static <T> String equals(String leftExpression, T rightExpression) {
        return leftExpression + " = " + rightExpression.toString();
    }

    public static <T> String in(String leftExpression, T rightExpression) {
        return leftExpression + " IN " + "(" + rightExpression.toString() + ")";
    }

    public static String numbers(Collection<? extends Number> numbers) {
        return String.join(",", numbers.stream()
                .map(Object::toString)
                .toList());
    }

    public static String numbers(Number... numbers) {
        return String.join(",", Arrays.stream(numbers)
                .map(Object::toString)
                .toList());
    }

    public static String strings(Collection<String> strings) {
        return String.join(",", strings.stream()
                .map(SqlOps::strLiteral)
                .toList());
    }

    public static String strings(String... strings) {
        return String.join(",", Arrays.stream(strings)
                .map(SqlOps::strLiteral)
                .toList());
    }

    public static String strLiteral(String str) {
        return "'" + str + "'";
    }

    public static void main(String[] args) {
        EntityManager entityManager = new SessionDelegatorBaseImpl(null);
        DeletePostgresNativeQuery.createWithPersistenceContext(entityManager)
                .deleteFrom("user")
                .where(and(
                        equals("id", "10"),
                        or(
                                not(equals("id", "15")),
                                in("name", strings("Егор", "Дмитрий")),
                                in("name", numbers(10, 12))
                        )
                ))
                .cascade()
                .execute();

    }

}
