package ru.korovin.packages.fasterjpa.testProject.service;

import ru.korovin.packages.fasterjpa.testProject.model.TestingEntityBatching;
import ru.korovin.packages.fasterjpa.service.CrudBatchService;

public interface BatchTestEntityService extends CrudBatchService<TestingEntityBatching, Long> {
}
