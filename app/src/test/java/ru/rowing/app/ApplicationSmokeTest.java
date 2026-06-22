package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionDay;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionDayRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @Autowired
    UserRepository users;
    @Autowired
    CompetitionRepository competitions;
    @Autowired
    CompetitionDayRepository days;
    @Autowired
    CategoryRepository categories;

    @Test
    void contextLoads() {
        // успешный запуск контекста подтверждает, что Spring Boot работает на текущем JDK
    }

    @Test
    void adminIsSeeded() {
        assertTrue(users.findByLogin("admin").isPresent(), "должен быть создан начальный администратор");
    }

    @Test
    void multiDayCompetitionWithCategoryPersists() {
        Competition comp = new Competition();
        comp.setName("Кубок весны 2026");
        comp = competitions.save(comp);

        CompetitionDay day2 = new CompetitionDay();
        day2.setCompetition(comp);
        day2.setOrdinal(2);
        day2.setDayDate(LocalDate.of(2026, 5, 11));
        day2.setStartTime(LocalTime.of(10, 0));
        day2 = days.save(day2);

        // 1000 м во второй день, с включённым финалом B
        Category c1000 = new Category();
        c1000.setCompetition(comp);
        c1000.setCompetitionDay(day2);
        c1000.setName("K1 мужчины 1000 м");
        c1000.setBoatClass(BoatClass.K1);
        c1000.setGender(Gender.MALE);
        c1000.setDistanceM(1000);
        c1000.setFinalBEnabled(true);
        categories.save(c1000);

        List<Category> day2cats = categories.findByCompetitionDayId(day2.getId());
        assertEquals(1, day2cats.size());
        assertEquals(Set.of("A", "B"), day2cats.getFirst().enabledFinals());
    }
}
