package ru.korovin.packages.fasterjpa.testProject.repository;

import ru.korovin.packages.fasterjpa.testProject.model.StructureDepartment;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public interface StructureDepartmentRepository extends JpaRepository<StructureDepartment, Long>,
        JpaSpecificationExecutor<StructureDepartment> {
}
