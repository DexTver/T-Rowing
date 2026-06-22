package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Stage;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByCategoryIdOrderByOrdinal(Long categoryId);
}
