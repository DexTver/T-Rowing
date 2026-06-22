package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Heat;

import java.util.Collection;
import java.util.List;

public interface HeatRepository extends JpaRepository<Heat, Long> {
    List<Heat> findByStageId(Long stageId);

    List<Heat> findByStageIdOrderByIndexInStage(Long stageId);

    /** Максимальный сквозной номер заезда в соревновании (для сквозной нумерации многодневного старта). */
    @Query("""
            select coalesce(max(h.number), 0) from Heat h
              join h.stage s
              join s.category c
            where c.competition.id = :competitionId
            """)
    int maxNumberInCompetition(@Param("competitionId") Long competitionId);

    /** Заезды соревнования, чьи категории уже показываемы (протокол сформирован), по сквозному номеру. */
    @Query("""
            select h from Heat h
              join h.stage s
              join s.category c
            where c.competition.id = :competitionId
              and c.status in :visibleStatuses
            order by h.number
            """)
    List<Heat> findVisibleByCompetition(@Param("competitionId") Long competitionId,
                                        @Param("visibleStatuses") Collection<CategoryStatus> visibleStatuses);
}
