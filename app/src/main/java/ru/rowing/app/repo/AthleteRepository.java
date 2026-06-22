package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Athlete;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findByCoachId(Long coachId);

    List<Athlete> findByCoachIdOrderByFullName(Long coachId);
}
