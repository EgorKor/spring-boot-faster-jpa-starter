package io.github.egorkor.service;

import io.github.egorkor.model.TestingEntityBatching;
import ru.samgtu.packages.webutils.service.CrudBatchService;

public interface BatchTestEntityService extends CrudBatchService<TestingEntityBatching, Long> {
}
