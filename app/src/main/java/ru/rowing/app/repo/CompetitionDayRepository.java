package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.CompetitionDay;

import java.util.List;

public interface CompetitionDayRepository extends JpaRepository<CompetitionDay, Long> {
    List<CompetitionDay> findByCompetitionIdOrderByOrdinal(Long competitionId);
}
