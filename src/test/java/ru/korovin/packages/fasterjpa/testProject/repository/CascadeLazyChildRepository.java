package ru.korovin.packages.fasterjpa.testProject.repository;

import ru.korovin.packages.fasterjpa.testProject.model.CascadeLazyChildEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Profile("test")
@Repository
public interface CascadeLazyChildRepository extends JpaRepository<CascadeLazyChildEntity, Long> {
}
