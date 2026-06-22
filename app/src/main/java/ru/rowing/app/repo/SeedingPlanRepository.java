package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.SeedingPlanFile;

import java.util.Optional;

public interface SeedingPlanRepository extends JpaRepository<SeedingPlanFile, Long> {
    Optional<SeedingPlanFile> findByLetter(String letter);
}
