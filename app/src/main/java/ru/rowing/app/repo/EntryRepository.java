package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Entry;

import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndAthleteId(Long categoryId, Long athleteId);

    boolean existsByAthleteId(Long athleteId);

    /** Заявки, поданные тренером в рамках соревнования (тренер видит только свои). */
    List<Entry> findBySubmittedByCoach_IdAndCategory_Competition_IdOrderByCreatedAt(Long coachId, Long competitionId);
}
