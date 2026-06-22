package ru.rowing.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Coach;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.CompetitionDay;
import ru.rowing.app.domain.CompetitionStatus;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.domain.Result;
import ru.rowing.app.domain.Stage;
import ru.rowing.app.domain.StageType;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.CompetitionDayRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.seeding.runtime.ResultStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Демонстрационные данные для проверки публичной части. Включается флагом {@code app.seed-demo=true}
 * (по умолчанию выключено). Создаёт одно «живое» соревнование с проведённым и непроведённым заездом.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final CompetitionRepository competitions;
    private final CompetitionDayRepository days;
    private final CategoryRepository categories;
    private final CoachRepository coaches;
    private final AthleteRepository athletes;
    private final StageRepository stages;
    private final HeatRepository heats;
    private final ResultRepository results;
    private final ru.rowing.app.repo.UserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public DemoDataSeeder(CompetitionRepository competitions, CompetitionDayRepository days,
                          CategoryRepository categories, CoachRepository coaches, AthleteRepository athletes,
                          StageRepository stages, HeatRepository heats, ResultRepository results,
                          ru.rowing.app.repo.UserRepository users,
                          org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.competitions = competitions;
        this.days = days;
        this.categories = categories;
        this.coaches = coaches;
        this.athletes = athletes;
        this.stages = stages;
        this.heats = heats;
        this.results = results;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (competitions.count() > 0) {
            return;
        }

        // Демо-тренер: учётка trainer/trainer, связанная с тренером-человеком.
        ru.rowing.app.domain.User trainerUser = new ru.rowing.app.domain.User();
        trainerUser.setLogin("trainer");
        trainerUser.setPasswordHash(encoder.encode("trainer"));
        trainerUser.setRole(ru.rowing.app.domain.Role.TRAINER);
        trainerUser.setDisplayName("Иванов Иван Иванович");
        trainerUser = users.save(trainerUser);

        Coach coach = new Coach();
        coach.setFullName("Иванов Иван Иванович");
        coach.setUser(trainerUser);
        coach = coaches.save(coach);

        Competition comp = new Competition();
        comp.setName("Открытый кубок города 2026");
        comp.setDescription("Демонстрационное соревнование.\nДистанции 200 и 500 м.");
        comp.setStartsAt(Instant.now());
        comp.setStatus(CompetitionStatus.LIVE);
        comp = competitions.save(comp);

        CompetitionDay day1 = new CompetitionDay();
        day1.setCompetition(comp);
        day1.setOrdinal(1);
        day1.setDayDate(LocalDate.now());
        day1.setStartTime(LocalTime.of(10, 0));
        day1 = days.save(day1);

        Category cat = new Category();
        cat.setCompetition(comp);
        cat.setCompetitionDay(day1);
        cat.setName("K1 мужчины 200 м");
        cat.setBoatClass(BoatClass.K1);
        cat.setGender(Gender.MALE);
        cat.setDistanceM(200);
        cat.setStatus(CategoryStatus.PRELIMS_RUNNING);
        cat.setPlan("A");
        cat.setActiveVariant("1");
        cat = categories.save(cat);

        // Категория с открытой регистрацией — чтобы демо-тренер мог подать заявку.
        Category openCat = new Category();
        openCat.setCompetition(comp);
        openCat.setCompetitionDay(day1);
        openCat.setName("K1 юноши 500 м (регистрация открыта)");
        openCat.setBoatClass(BoatClass.K1);
        openCat.setGender(Gender.MALE);
        openCat.setDistanceM(500);
        openCat.setBirthYearFrom(2009);
        openCat.setBirthYearTo(2010);
        openCat.setStatus(CategoryStatus.REGISTRATION_OPEN);
        categories.save(openCat);

        Stage prelim = new Stage();
        prelim.setCategory(cat);
        prelim.setType(StageType.PRELIM);
        prelim.setOrdinal(1);
        prelim = stages.save(prelim);

        // Заезд 1 — проведён (с временами).
        Heat heat1 = newHeat(prelim, 1, 1, Instant.now());
        String[] names1 = {"Петров П.П.", "Сидоров С.С.", "Кузнецов К.К.", "Смирнов С.А.", "Орлов О.О."};
        long[] times1 = {41230, 41880, 42010, 42550, 43900};
        for (int i = 0; i < names1.length; i++) {
            Athlete a = newAthlete(names1[i], 2008, "Москва", "СШОР №1", coach);
            saveResult(heat1, a, i + 1, ResultStatus.OK, times1[i]);
        }

        // Заезд 2 — ещё не проведён (NOT_STARTED).
        Heat heat2 = newHeat(prelim, 2, 2, Instant.now().plusSeconds(600));
        String[] names2 = {"Волков В.В.", "Зайцев З.З.", "Морозов М.М.", "Лебедев Л.Л."};
        for (int i = 0; i < names2.length; i++) {
            Athlete a = newAthlete(names2[i], 2008, "Санкт-Петербург", "СШОР №2", coach);
            saveResult(heat2, a, i + 1, ResultStatus.NOT_STARTED, null);
        }
    }

    private Heat newHeat(Stage stage, int number, int indexInStage, Instant start) {
        Heat h = new Heat();
        h.setStage(stage);
        h.setNumber(number);
        h.setIndexInStage(indexInStage);
        h.setScheduledStart(start);
        return heats.save(h);
    }

    private Athlete newAthlete(String name, int birthYear, String region, String school, Coach coach) {
        Athlete a = new Athlete();
        a.setFullName(name);
        a.setBirthYear(birthYear);
        a.setRegion(region);
        a.setSportSchool(school);
        a.setCoach(coach);
        return athletes.save(a);
    }

    private void saveResult(Heat heat, Athlete athlete, int lane, ResultStatus status, Long timeMs) {
        Result r = new Result();
        r.setHeat(heat);
        r.setAthlete(athlete);
        r.setLane(lane);
        r.setStatus(status);
        r.setTimeMs(timeMs);
        results.save(r);
    }
}
