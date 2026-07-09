package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.UserRepository;
import ru.rowing.app.web.view.AdminViews.AthleteRow;
import ru.rowing.app.web.view.AdminViews.CoachRow;
import ru.rowing.app.web.view.AdminViews.CompetitionRow;
import ru.rowing.app.web.view.AdminViews.Option;
import ru.rowing.app.web.view.AdminViews.Overview;
import ru.rowing.app.web.view.AdminViews.PlanRow;
import ru.rowing.app.web.view.AdminViews.PlansView;
import ru.rowing.app.web.view.AdminViews.UserRow;
import ru.rowing.app.web.view.Labels;

import java.util.Comparator;
import java.util.List;

/** Чтение данных для админки (DTO готовятся в транзакции, open-in-view выключен). */
@Service
public class AdminViewService {

    private final UserRepository users;
    private final CoachRepository coaches;
    private final AthleteRepository athletes;
    private final CompetitionRepository competitions;
    private final CategoryRepository categories;
    private final EntryRepository entries;
    private final HeatRepository heats;
    private final ResultRepository results;
    private final PlanCatalog catalog;

    public AdminViewService(UserRepository users, CoachRepository coaches, AthleteRepository athletes,
                            CompetitionRepository competitions, CategoryRepository categories,
                            EntryRepository entries, HeatRepository heats, ResultRepository results,
                            PlanCatalog catalog) {
        this.users = users;
        this.coaches = coaches;
        this.athletes = athletes;
        this.competitions = competitions;
        this.categories = categories;
        this.entries = entries;
        this.heats = heats;
        this.results = results;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(users.count(), coaches.count(), athletes.count(), competitions.count(),
                categories.count(), entries.count(), heats.count(), results.count());
    }

    @Transactional(readOnly = true)
    public List<UserRow> users() {
        return users.findAllByOrderByLogin().stream()
                .map(u -> new UserRow(u.getId(), u.getLogin(), u.getRole().name(), u.getDisplayName(), u.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoachRow> coaches() {
        return coaches.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getFullName() == null ? "" : c.getFullName()))
                .map(c -> new CoachRow(c.getId(), c.getFullName(),
                        c.getUser() != null ? c.getUser().getLogin() : ""))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AthleteRow> athletes() {
        return athletes.findAll().stream()
                .sorted(Comparator.comparing(a -> a.getFullName() == null ? "" : a.getFullName()))
                .map(a -> new AthleteRow(a.getId(), a.getFullName(), a.getBirthYear(), a.getRank(),
                        a.getRegion(), a.getSportSchool(),
                        a.getCoach() != null ? a.getCoach().getId() : null,
                        a.getCoach() != null ? a.getCoach().getFullName() : ""))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitionRow> competitions() {
        return competitions.findAll().stream()
                .map(c -> new CompetitionRow(c.getId(), c.getName(), Labels.competitionStatus(c.getStatus())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Option> userOptions() {
        return users.findAllByOrderByLogin().stream()
                .map(u -> new Option(u.getId(), u.getLogin()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Option> coachOptions() {
        return coaches.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getFullName() == null ? "" : c.getFullName()))
                .map(c -> new Option(c.getId(), c.getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlansView plans() {
        List<PlanRow> rows = catalog.statuses().stream()
                .map(s -> new PlanRow(s.letter(), s.source(), s.valid(),
                        String.join("; ", s.violations()), s.min(), s.max()))
                .toList();
        List<String> cov = catalog.coverageViolations();
        String okMsg = "Диапазон " + ru.rowing.seeding.GridValidator.COVERAGE_MIN + ".."
                + ru.rowing.seeding.GridValidator.COVERAGE_MAX + " покрыт без дыр.";
        return new PlansView(rows, cov.isEmpty() ? okMsg : String.join("; ", cov));
    }
}
