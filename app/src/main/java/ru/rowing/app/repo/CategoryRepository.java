package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCompetitionId(Long competitionId);

    List<Category> findByCompetitionDayId(Long competitionDayId);
}
