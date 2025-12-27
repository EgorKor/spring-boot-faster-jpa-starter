package ru.korovin.packages.fasterjpa.service.mapping;

import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
public class ProjectionMappingContext {
    private Object[] data;
    private Map<String, Integer> paramIndexMapping;


    public String getString(String property){
        return getTypedPropertyValue(String.class, property);
    }

    public Long getLong(String property){
        return getTypedPropertyValue(Long.class, property);
    }

    public Integer getInt(String property){
        return getTypedPropertyValue(Integer.class, property);
    }

    public BigDecimal getBigDecimal(String property){
        return getTypedPropertyValue(BigDecimal.class, property);
    }

    public LocalDateTime getDateTime(String property){
        return getTypedPropertyValue(LocalDateTime.class, property);
    }

    public Instant getInstant(String property){
        return getTypedPropertyValue(Instant.class, property);
    }




    <T> T getTypedPropertyValue(Class<T> type, String param) {
        if (!paramIndexMapping.containsKey(param)) {
            return null;
        }
        Object dataElement = data[paramIndexMapping.get(param)];
        if (dataElement == null) {
            return null;
        }
        return type.cast(dataElement);
    }
}
