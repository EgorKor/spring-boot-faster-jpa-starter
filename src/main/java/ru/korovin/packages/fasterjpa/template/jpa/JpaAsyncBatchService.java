package ru.korovin.packages.fasterjpa.template.jpa;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import ru.korovin.packages.fasterjpa.service.async.AsyncCrudBatchService;
import ru.korovin.packages.fasterjpa.service.batching.BatchResultWithData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class JpaAsyncBatchService<T, ID> extends JpaAsyncCrudService<T, ID>
        implements AsyncCrudBatchService<T, ID> {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final JpaBatchCrudService<T, ID> batchCrudService;


    public JpaAsyncBatchService(JpaRepository<T, ID> jpaRepository,
                                JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                                TransactionTemplate transactionTemplate,
                                EntityManager persistenceContext,
                                ThreadPoolTaskExecutor executor,
                                Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, persistenceContext, transactionTemplate, executor, validator);
        this.batchCrudService = new JpaBatchCrudService<>(jpaRepository,
                jpaSpecificationExecutor,
                persistenceContext,
                transactionTemplate,
                validator);
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(
                () -> batchCrudService.batchCreate(models, batchSize), executor);
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchDelete(ids, batchSize), executor);

    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchCreateAtomic(models, batchSize), executor);
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.runAsync(() -> batchCrudService.batchDeleteAtomic(ids, batchSize), executor);
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models) {
        return batchCreateAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids) {
        return batchDeleteAsync(ids, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models) {
        return batchCreateAtomicAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids) {
        return batchDeleteAtomicAsync(ids, DEFAULT_BATCH_SIZE);
    }
}
