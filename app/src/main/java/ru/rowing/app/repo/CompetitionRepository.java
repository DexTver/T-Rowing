package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Competition;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
}
