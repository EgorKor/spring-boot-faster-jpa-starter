package ru.korovin.packages.fasterjpa.testProject.repository;

import ru.korovin.packages.fasterjpa.testProject.model.TestNestedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestNestedEntityRepository extends JpaRepository<TestNestedEntity, Long>, JpaSpecificationExecutor<TestNestedEntity> {
}
