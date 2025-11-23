package ru.korovin.packages.fasterjpa.template;

import ru.korovin.packages.fasterjpa.service.batching.BatchOperationStatus;
import ru.korovin.packages.fasterjpa.service.batching.BatchResultWithData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * @author EgorKor
 * @version 1.0.4
 * @since 2025
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchResultWithDataImpl<T> implements BatchResultWithData<T> {
    private String message;
    private T data;
    private String details;
    private BatchOperationStatus status;

    @Override
    public T getData() {
        return data;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDetails() {
        return details;
    }

    @Override
    public BatchOperationStatus getStatus() {
        return status;
    }


}
