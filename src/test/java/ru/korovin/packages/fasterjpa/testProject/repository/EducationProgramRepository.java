package ru.korovin.packages.fasterjpa.testProject.repository;

import ru.korovin.packages.fasterjpa.testProject.model.EducationProgram;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public interface EducationProgramRepository extends JpaRepository<EducationProgram, Long>, JpaSpecificationExecutor<EducationProgram> {
}
