package ru.rowing.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Result;

import java.util.Collection;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByHeatId(Long heatId);

    boolean existsByAthleteId(Long athleteId);

    /**
     * Поиск результатов по фильтрам публичной части (раздел 7.1, п.4):
     * по спортсмену, тренеру, категории, субъекту, спорт-школе. Пустая строка фильтра игнорируется.
     * Параметры передаются непустыми строками (а не null) — иначе PostgreSQL не может вывести тип
     * параметра в проверке «is null». Поэтому сравниваем с пустой строкой.
     */
    @Query("""
            select r from Result r
              join r.heat h
              join h.stage s
              join s.category c
              join c.competition comp
              join r.athlete a
              left join a.coach co
            where c.status in :visibleStatuses
              and (:athlete  = '' or lower(a.fullName)    like lower(concat('%', :athlete,  '%')))
              and (:coach    = '' or lower(co.fullName)   like lower(concat('%', :coach,    '%')))
              and (:category = '' or lower(c.name)        like lower(concat('%', :category, '%')))
              and (:region   = '' or lower(a.region)      like lower(concat('%', :region,   '%')))
              and (:school   = '' or lower(a.sportSchool) like lower(concat('%', :school,   '%')))
            order by comp.name, c.name, h.number, r.lane
            """)
    List<Result> search(@Param("athlete") String athlete,
                        @Param("coach") String coach,
                        @Param("category") String category,
                        @Param("region") String region,
                        @Param("school") String school,
                        @Param("visibleStatuses") Collection<CategoryStatus> visibleStatuses);
}
