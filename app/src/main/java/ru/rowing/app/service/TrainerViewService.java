package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Entry;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.web.view.TrainerViews.CompetitionRow;
import ru.rowing.app.web.view.TrainerViews.CompetitionView;
import ru.rowing.app.web.view.TrainerViews.EntryRow;
import ru.rowing.app.web.view.TrainerViews.MyAthlete;
import ru.rowing.app.web.view.TrainerViews.OpenCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Чтение данных для страниц тренера (DTO готовятся в транзакции, open-in-view выключен). */
@Service
public class TrainerViewService {

    private final CompetitionRepository competitions;
    private final CategoryRepository categories;
    private final AthleteRepository athletes;
    private final EntryRepository entries;

    public TrainerViewService(CompetitionRepository competitions, CategoryRepository categories,
                              AthleteRepository athletes, EntryRepository entries) {
        this.competitions = competitions;
        this.categories = categories;
        this.athletes = athletes;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public List<CompetitionRow> listCompetitions() {
        return competitions.findAll().stream()
                .sorted(Comparator.comparing(Competition::getId).reversed())
                .map(c -> new CompetitionRow(c.getId(), c.getName(), hasOpenRegistration(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CompetitionView> competition(long coachId, long competitionId) {
        return competitions.findById(competitionId).map(c -> {
            List<OpenCategory> open = categories.findByCompetitionId(competitionId).stream()
                    .filter(cat -> cat.getStatus() == CategoryStatus.REGISTRATION_OPEN)
                    .map(cat -> new OpenCategory(cat.getId(), cat.getName(),
                            cat.getBirthYearFrom(), cat.getBirthYearTo()))
                    .toList();

            List<MyAthlete> mine = athletes.findByCoachIdOrderByFullName(coachId).stream()
                    .map(a -> new MyAthlete(a.getId(), a.getFullName(), a.getBirthYear()))
                    .toList();

            List<EntryRow> myEntries = entries
                    .findBySubmittedByCoach_IdAndCategory_Competition_IdOrderByCreatedAt(coachId, competitionId)
                    .stream()
                    .map(this::toEntryRow)
                    .toList();

            return new CompetitionView(c.getId(), c.getName(), !open.isEmpty(), open, mine, myEntries);
        });
    }

    private EntryRow toEntryRow(Entry e) {
        Athlete a = e.getAthlete();
        Category cat = e.getCategory();
        boolean ageWarning = !ageFits(a.getBirthYear(), cat);
        boolean canDelete = cat.getStatus() == CategoryStatus.REGISTRATION_OPEN;
        return new EntryRow(e.getId(), a.getFullName(), a.getBirthYear(), cat.getName(), ageWarning, canDelete);
    }

    /** Подходит ли год рождения в категорию (мягкая проверка, раздел 7.2). */
    public static boolean ageFits(int birthYear, Category cat) {
        if (cat.getBirthYearFrom() != null && birthYear < cat.getBirthYearFrom()) {
            return false;
        }
        return cat.getBirthYearTo() == null || birthYear <= cat.getBirthYearTo();
    }

    private boolean hasOpenRegistration(long competitionId) {
        return categories.findByCompetitionId(competitionId).stream()
                .anyMatch(c -> c.getStatus() == CategoryStatus.REGISTRATION_OPEN);
    }
}
