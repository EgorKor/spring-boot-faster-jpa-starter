package ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition;


import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.*;
import ru.korovin.packages.fasterjpa.queryparam.utils.FieldTypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@Setter
@Getter
@EqualsAndHashCode
public final class FilterCondition implements FilterConditionTreeNode {
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(.*)\\.(length\\(\\)|size\\(\\)|isEmpty\\(\\)|isNotEmpty\\(\\))");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter();

    private static ThreadLocal<Root<?>> rootContext = new ThreadLocal<>();
    private static ThreadLocal<CriteriaBuilder> criteriaBuilderContext = new ThreadLocal<>();
    private static ThreadLocal<Class<?>> entityTypeContext = new ThreadLocal<>();

    private String property;
    private FilterOperation operation;
    private Object value;

    public FilterCondition(String property, FilterOperation operation, Object value) {
        this.property = property;
        this.operation = operation;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Filter('%s' %s %s)".formatted(property, operation.getOperation(), value);
    }

    @Override
    public Predicate parsePredicate(Root<?> root,
                                    CriteriaQuery<?> criteriaQuery,
                                    CriteriaBuilder cb,
                                    Class<?> entityType) {
        try {
            rootContext.set(root);
            criteriaBuilderContext.set(cb);
            Function function = null;
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(property);
            if (functionMatcher.matches()) {
                String functionStr = functionMatcher.group(2);
                function = Function.parseByOperation(functionStr);
                property = property.substring(0, property.lastIndexOf(functionStr) - 1);
            }

            Expression<?> selection = FieldExpressionCompiler.compileToCriteria(property, cb, root);
            Field reflectionField = FieldTypeUtils.getField(entityType, property);

            return switch (operation) {
                case EQUALS_IGNORE_CASE -> parseEqualIgnoreCasePredicate(cb, selection, value.toString());
                case IS -> parseIsPredicate(cb, selection, function, Is.parse(value.toString()));
                case IS_NOT -> cb.not(parseIsPredicate(cb, selection, function, Is.parse(value.toString())));
                case EQUALS -> parseEqualPredicate(cb, selection, reflectionField, value, function, property);
                case GT, LS, GTE, LSE ->
                        parseComparisonPredicate(cb, selection, operation, reflectionField, value, function, property);
                case NOT_EQUALS ->
                        cb.not(parseEqualPredicate(cb, selection, reflectionField, value, function, property));
                case CONTAINS -> parseContainsPredicate(cb, selection, value.toString());
                case NOT_CONTAINS -> cb.not(parseContainsPredicate(cb, selection, value.toString()));
                case LIKE -> parseLikePredicate(cb, selection, value.toString());
                case NOT_LIKE -> cb.not(parseLikePredicate(cb, selection, value.toString()));
                case IN -> parseInPredicate(cb, selection, reflectionField, (Collection<?>) value, function, property);
                case NOT_IN ->
                        cb.not(parseInPredicate(cb, selection, reflectionField, (Collection<?>) value, function, property));
            };
        } finally {
            rootContext.remove();
            criteriaBuilderContext.remove();
        }
    }

    @Override
    public FilterConditionTreeNode copy() {
        return new FilterCondition(property, operation, value);
    }

    private Predicate parseEqualIgnoreCasePredicate(CriteriaBuilder cb, Expression<?> selection, String value) {
        return cb.equal(cb.lower(getTypedExpression(selection, String.class)), value.toLowerCase());
    }

    private static Class<?> getFieldType(String field, Field reflectionField, Function function) {
        if (field.startsWith("concat")) {
            return String.class;
        }
        if (function != null) {
            if (function == Function.LENGTH || function == Function.SIZE) {
                return Long.class;
            }
        }
        return reflectionField != null ? reflectionField.getType() : null;
    }


    private static Expression<String> convertToString(CriteriaBuilder cb, Expression<?> expression) {
        if (expression.getJavaType() == String.class) {
            return (Expression<String>) expression;
        }
        // Для числовых и других типов преобразуем в строку
        return cb.toString((Expression<Character>) expression);
    }

    private Predicate parseInPredicate(CriteriaBuilder cb,
                                       Expression<?> selection,
                                       Field reflectionField,
                                       Collection<?> inValues,
                                       Function function,
                                       String field) {

        //Если есть функция size или length
        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            Class<?> elementType = getCollectionElementType(reflectionField);
            if (function != null) {
                Object[] values = inValues
                        .stream()
                        .map(v -> convertValue(v, elementType))
                        .toArray();
                return getFunctionPath(cb, selection, function).in(values);
            }

            if (!isRelation(reflectionField)) {

                Object[] values = inValues.stream()
                        .map(inValue -> convertValue(inValue, elementType))
                        .toArray();

                List<Predicate> predicates = new ArrayList<>();

                for (Object value : values) {

                    // Используем array_position - возвращает позицию элемента или 0 если не найден
                    Expression<Integer> position = cb.function(
                            "array_position",
                            Integer.class,
                            selection,
                            cb.literal(value)
                    );

                    predicates.add(cb.gt(position, 0));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            }

            List<Predicate> predicates = new ArrayList<>();
            for (Object inValue : inValues) {
                Object val = convertValue(inValue, elementType);
                predicates.add(cb.isMember(val, (Path<Collection>) selection));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        }
        // Для обычных полей
        Class<?> fieldType = defineValueType(selection, field, reflectionField, function);
        Object[] values = inValues.stream()
                .map(v -> convertValue(v, fieldType))
                .toArray();
        return selection.in(values);
    }

    public static Class<?> defineValueType(Expression<?> selection, String fieldName, Field reflectionField, Function function) {
        Class<?> result;
        if (reflectionField != null) {
            result = getFieldType(fieldName, reflectionField, function);
            if (result != null) {
                return result;
            }
        }
        return selection.getJavaType();
    }

    public static boolean isRelation(Field field) {
        return field.isAnnotationPresent(OneToOne.class) ||
                field.isAnnotationPresent(OneToMany.class) ||
                field.isAnnotationPresent(ManyToOne.class) ||
                field.isAnnotationPresent(ManyToMany.class);
    }

    public static Class<?> getCollectionElementType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return String.class;
    }

    @SneakyThrows
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == null || value.getClass().equals(targetType)) {
            return value;
        }

        if (value instanceof ValueExpression(String expression)) {
            Root<?> root = rootContext.get();
            CriteriaBuilder cb = criteriaBuilderContext.get();
            return FieldExpressionCompiler.compileToCriteria(expression, cb, root);
        }

        // Конвертация между числовыми типами
        if (Number.class.isAssignableFrom(targetType) && value instanceof Number number) {

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return number.intValue();
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return number.longValue();
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return number.doubleValue();
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return number.floatValue();
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return number.shortValue();
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return number.byteValue();
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(number.toString());
            } else if (targetType.equals(BigInteger.class)) {
                return BigInteger.valueOf(number.longValue());
            }
        }

        // Конвертация строк в числа
        if (Number.class.isAssignableFrom(targetType) && value instanceof String) {
            String stringValue = ((String) value).trim();

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return Short.parseShort(stringValue);
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return Byte.parseByte(stringValue);
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(stringValue);
            } else if (targetType.equals(BigInteger.class)) {
                return new BigInteger(stringValue);
            }
        }

        if (value instanceof Is is) {
            return switch (is) {
                case TRUE -> true;
                case FALSE -> false;
                case NULL -> null;
                default -> throw new InvalidParameterException("Некорректное значение для операции is: " + value);
            };
        }

        if (value.getClass() != String.class) {
            throw new InvalidParameterException("Невозможно преобразовать объект типа %s в тип %s"
                    .formatted(value.getClass().getSimpleName(), targetType.getSimpleName()));
        }
        String stringValue = value.toString();
        try {
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(stringValue);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(stringValue);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(stringValue);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(stringValue);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(stringValue);
            if (targetType == java.sql.Date.class)
                return java.sql.Date.valueOf(LocalDate.parse(stringValue, DATE_TIME_FORMATTER));
            if (targetType == LocalDate.class) return LocalDate.parse(stringValue, DATE_TIME_FORMATTER);
            if (targetType == LocalDateTime.class) {
                try {
                    return LocalDateTime.parse(stringValue, DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    return LocalDateTime.parse(stringValue);
                }
            }
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, stringValue);

            throw new InvalidParameterException(": " + targetType.getName());
        } catch (Exception e) {
            throw new InvalidParameterException(
                    String.format("Невозможно преобразовать '%s в %s: %s",
                            stringValue, targetType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseIsPredicate(CriteriaBuilder cb, Expression<?> selection, Function function, Is value) {
        return switch (value) {
            case TRUE -> cb.isTrue(getTypedExpression(getFunctionPath(cb, selection, function), Boolean.class));
            case FALSE -> cb.isFalse(getTypedExpression(getFunctionPath(cb, selection, function), Boolean.class));
            case NULL -> cb.isNull(selection);
            case NOT_NULL -> cb.isNotNull(selection);
        };
    }

    private Predicate parseEqualPredicate(CriteriaBuilder cb,
                                          Expression<?> selection,
                                          Field reflectionField,
                                          Object value,
                                          Function function,
                                          String field) {
        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {

            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> cb.equal(getFunctionPath(cb, selection, function),
                            convertValue(value, Long.class));
                    case IS_EMPTY, IS_NOT_EMPTY -> cb.equal(getFunctionPath(
                            cb, selection, function
                    ), convertValue(value, Boolean.class));
                };
            }
            Object convertedValue = convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Expression<Collection>) selection);
        }
        value = convertValue(value, getFieldType(field, reflectionField, function));
        return cb.equal(getFunctionPath(cb, selection, function), value);
    }

    private Expression<?> getFunctionPath(CriteriaBuilder cb, Expression<?> current, Function function) {
        if (function == null) {
            return current;
        }
        return switch (function) {
            case LENGTH -> cb.length(getTypedExpression(current, String.class));
            case SIZE -> cb.size(getTypedExpression(current, Collection.class));
            case IS_EMPTY -> cb.isEmpty(getTypedExpression(current, Collection.class));
            case IS_NOT_EMPTY -> cb.isNotEmpty(getTypedExpression(current, Collection.class));
        };

    }

    private Predicate parseComparisonPredicate(CriteriaBuilder cb,
                                               Expression<?> selection,
                                               FilterOperation operation,
                                               Field reflectionField,
                                               Object value,
                                               Function function,
                                               String field) {
        if (reflectionField != null && !Comparable.class.isAssignableFrom(reflectionField.getType())
                && function == null) {
            throw new InvalidParameterException("Аттрибут выборки " + selection + " не реализует интерфейс Comparable");
        }

        Expression<Comparable> comparablePath = (Expression<Comparable>) getFunctionPath(cb, selection, function);

        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE ->
                            getComparisonPredicate(cb, operation, comparablePath, (Long) convertValue(value, Long.class));
                    case IS_EMPTY, IS_NOT_EMPTY ->
                            throw new IllegalStateException("Невозможно применить операцию сравнения с функциями isEmpty()/isNotEmpty()");
                };
            }

            Comparable<?> convertedValue = (Comparable<?>) convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Path<Collection>) selection);
        }

        Class<?> type = getFieldType(field, reflectionField, function);
        Comparable<?> comparableValue = (Comparable<?>) convertValue(value, type);
        return getComparisonPredicate(cb, operation, comparablePath, comparableValue);
    }

    //endregion


    //region Utility Methods
    private static Predicate getComparisonPredicate(CriteriaBuilder cb,
                                                    FilterOperation operation,
                                                    Expression<Comparable> comparableSelection,
                                                    Comparable value) {
        return switch (operation) {
            case GT -> cb.greaterThan(comparableSelection, value);
            case LS -> cb.lessThan(comparableSelection, value);
            case GTE -> cb.greaterThanOrEqualTo(comparableSelection, value);
            case LSE -> cb.lessThanOrEqualTo(comparableSelection, value);
            default -> throw new InvalidParameterException("Некорректное операция сравнения: " + operation);
        };
    }

    private Predicate parseContainsPredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringSelection = cb.lower(getTypedExpression(selection, String.class));
        return cb.like(stringSelection, "%" + stringValue.toLowerCase() + "%");
    }

    private Predicate parseLikePredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringPath = getTypedExpression(selection, String.class);
        return cb.like(stringPath, stringValue);
    }

    public static <X> Expression<X> getTypedExpression(Expression<?> expression, Class<X> type) {
        return (Expression<X>) expression;
    }

}
