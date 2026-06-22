package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rowing.app.domain.Coach;

import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Long> {
    Optional<Coach> findByUserId(Long userId);
}
