package ru.rowing.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Coach;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Role;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.UserRepository;
import ru.rowing.app.service.TrainerService;
import ru.rowing.app.service.TrainerViewService;
import ru.rowing.seeding.SeedingException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrainerFlowTest {

    @Autowired TrainerService trainer;
    @Autowired TrainerViewService trainerView;
    @Autowired UserRepository users;
    @Autowired CoachRepository coaches;
    @Autowired AthleteRepository athletes;
    @Autowired CompetitionRepository competitions;
    @Autowired CategoryRepository categories;

    private long competitionId;
    private long openCatId;
    private long closedCatId;

    @BeforeEach
    void seed() {
        User u = new User();
        u.setLogin("t1");
        u.setPasswordHash("x");
        u.setRole(Role.TRAINER);
        u.setDisplayName("Тренер Первый");
        users.save(u);

        Competition comp = new Competition();
        comp.setName("Кубок тренера");
        comp = competitions.save(comp);
        competitionId = comp.getId();

        openCatId = newCategory(comp, "Открытая", CategoryStatus.REGISTRATION_OPEN, 2009, 2010).getId();
        closedCatId = newCategory(comp, "Черновик", CategoryStatus.DRAFT, null, null).getId();
    }

    @Test
    void currentCoachIsAutoCreatedAndReused() {
        Coach c1 = trainer.currentCoach("t1");
        Coach c2 = trainer.currentCoach("t1");
        assertEquals(c1.getId(), c2.getId(), "повторный вызов не плодит тренеров");
        assertEquals("Тренер Первый", c1.getFullName());
    }

    @Test
    void submitsOwnAthleteToOpenCategory() {
        Athlete a = trainer.createAthlete("t1", "Юный Гребец", 2009, "1р", "Москва", "СШОР");
        TrainerService.CommitResult r = trainer.commitEntries("t1", competitionId,
                List.of(a.getId()), List.of(openCatId));
        assertEquals(1, r.added());
        assertTrue(r.messages().isEmpty());

        var view = trainerView.competition(trainer.currentCoach("t1").getId(), competitionId).orElseThrow();
        assertEquals(1, view.myEntries.size());
        assertFalse(view.myEntries.get(0).ageWarning, "2009 в диапазоне 2009–2010");
    }

    @Test
    void closedCategoryAndDuplicatesAreSkipped() {
        Athlete a = trainer.createAthlete("t1", "Гребец", 2009, null, null, null);
        // в черновик нельзя
        TrainerService.CommitResult closed = trainer.commitEntries("t1", competitionId,
                List.of(a.getId()), List.of(closedCatId));
        assertEquals(0, closed.added());
        assertTrue(closed.messages().stream().anyMatch(m -> m.contains("регистрация не открыта")));

        // дубль в открытую
        trainer.commitEntries("t1", competitionId, List.of(a.getId()), List.of(openCatId));
        TrainerService.CommitResult dup = trainer.commitEntries("t1", competitionId,
                List.of(a.getId()), List.of(openCatId));
        assertEquals(0, dup.added());
        assertTrue(dup.messages().stream().anyMatch(m -> m.contains("уже в заявке")));
    }

    @Test
    void cannotSubmitForeignAthlete() {
        Coach other = new Coach();
        other.setFullName("Чужой тренер");
        other = coaches.save(other);
        Athlete foreign = new Athlete();
        foreign.setFullName("Чужой спортсмен");
        foreign.setBirthYear(2009);
        foreign.setCoach(other);
        foreign = athletes.save(foreign);

        TrainerService.CommitResult r = trainer.commitEntries("t1", competitionId,
                List.of(foreign.getId()), List.of(openCatId));
        assertEquals(0, r.added());
        assertTrue(r.messages().stream().anyMatch(m -> m.contains("только своих")));
    }

    @Test
    void ageMismatchWarnsButIsAllowed() {
        Athlete old = trainer.createAthlete("t1", "Возрастной", 2005, null, null, null);
        TrainerService.CommitResult r = trainer.commitEntries("t1", competitionId,
                List.of(old.getId()), List.of(openCatId));
        assertEquals(1, r.added(), "возраст не блокирует подачу");

        var view = trainerView.competition(trainer.currentCoach("t1").getId(), competitionId).orElseThrow();
        assertTrue(view.myEntries.get(0).ageWarning, "2005 вне диапазона 2009–2010");
    }

    @Test
    void deleteOnlyOwnEntryWhileOpen() {
        Athlete a = trainer.createAthlete("t1", "Гребец", 2009, null, null, null);
        trainer.commitEntries("t1", competitionId, List.of(a.getId()), List.of(openCatId));
        var view = trainerView.competition(trainer.currentCoach("t1").getId(), competitionId).orElseThrow();
        long entryId = view.myEntries.get(0).id;

        // чужой тренер не может удалить
        users.save(makeTrainer("t2", "Второй"));
        assertThrows(SeedingException.class, () -> trainer.deleteEntry("t2", entryId));

        // владелец может
        trainer.deleteEntry("t1", entryId);
        var after = trainerView.competition(trainer.currentCoach("t1").getId(), competitionId).orElseThrow();
        assertTrue(after.myEntries.isEmpty());
    }

    private Category newCategory(Competition comp, String name, CategoryStatus status, Integer from, Integer to) {
        Category c = new Category();
        c.setCompetition(comp);
        c.setName(name);
        c.setBoatClass(BoatClass.K1);
        c.setGender(Gender.MALE);
        c.setDistanceM(500);
        c.setBirthYearFrom(from);
        c.setBirthYearTo(to);
        c.setStatus(status);
        return categories.save(c);
    }

    private User makeTrainer(String login, String name) {
        User u = new User();
        u.setLogin(login);
        u.setPasswordHash("x");
        u.setRole(Role.TRAINER);
        u.setDisplayName(name);
        return u;
    }
}
