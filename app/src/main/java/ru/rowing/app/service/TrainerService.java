package ru.rowing.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.CategoryStatus;
import ru.rowing.app.domain.Coach;
import ru.rowing.app.domain.Entry;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.UserRepository;
import ru.rowing.seeding.SeedingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Действия тренера (раздел 7.2 ТЗ): подача и правка собственной заявки в окне регистрации.
 * Тренер работает только со своими спортсменами (привязка через {@link Coach#getUser()}).
 */
@Service
@Transactional
public class TrainerService {

    /** Итог коммита заявки: сколько добавлено и сообщения о пропущенных. */
    public record CommitResult(int added, List<String> messages) {
    }

    private final UserRepository users;
    private final CoachRepository coaches;
    private final AthleteRepository athletes;
    private final CategoryRepository categories;
    private final EntryRepository entries;

    public TrainerService(UserRepository users, CoachRepository coaches, AthleteRepository athletes,
                          CategoryRepository categories, EntryRepository entries) {
        this.users = users;
        this.coaches = coaches;
        this.athletes = athletes;
        this.categories = categories;
        this.entries = entries;
    }

    /** Тренер-сущность для учётки; создаётся при первом обращении, если ещё не связана. */
    public Coach currentCoach(String login) {
        User user = users.findByLogin(login)
                .orElseThrow(() -> new SeedingException("Пользователь не найден: " + login));
        return coaches.findByUserId(user.getId()).orElseGet(() -> {
            Coach coach = new Coach();
            coach.setUser(user);
            coach.setFullName(user.getDisplayName() != null ? user.getDisplayName() : user.getLogin());
            return coaches.save(coach);
        });
    }

    /** Создаёт нового спортсмена, закреплённого за тренером (раздел 7.2, «добавить нового»). */
    public Athlete createAthlete(String login, String fullName, int birthYear,
                                 String rank, String region, String school) {
        Coach coach = currentCoach(login);
        Athlete a = new Athlete();
        a.setFullName(fullName);
        a.setBirthYear(birthYear);
        a.setRank(rank);
        a.setRegion(region);
        a.setSportSchool(school);
        a.setCoach(coach);
        return athletes.save(a);
    }

    /** Коммит промежуточной таблицы в заявки. Возрастное несоответствие не блокирует (раздел 7.2). */
    public CommitResult commitEntries(String login, long competitionId,
                                      List<Long> athleteIds, List<Long> categoryIds) {
        Coach coach = currentCoach(login);
        List<String> messages = new ArrayList<>();
        int added = 0;
        int n = Math.min(size(athleteIds), size(categoryIds));
        for (int i = 0; i < n; i++) {
            Long athleteId = athleteIds.get(i);
            Long categoryId = categoryIds.get(i);
            Athlete athlete = athletes.findById(athleteId).orElse(null);
            Category category = categories.findById(categoryId).orElse(null);
            if (athlete == null || category == null) {
                messages.add("Пропущена строка: спортсмен или категория не найдены.");
                continue;
            }
            if (category.getCompetition().getId() != competitionId) {
                messages.add(category.getName() + ": категория не из этого соревнования.");
                continue;
            }
            if (athlete.getCoach() == null || !athlete.getCoach().getId().equals(coach.getId())) {
                messages.add(athlete.getFullName() + ": можно подавать только своих спортсменов.");
                continue;
            }
            if (category.getStatus() != CategoryStatus.REGISTRATION_OPEN) {
                messages.add(category.getName() + ": регистрация не открыта.");
                continue;
            }
            if (entries.existsByCategoryIdAndAthleteId(categoryId, athleteId)) {
                messages.add(athlete.getFullName() + " уже в заявке на «" + category.getName() + "».");
                continue;
            }
            Entry e = new Entry();
            e.setCategory(category);
            e.setAthlete(athlete);
            e.setSubmittedByCoach(coach);
            entries.save(e);
            added++;
        }
        return new CommitResult(added, messages);
    }

    /** Удаление собственной заявки, пока регистрация в категории открыта (раздел 7.2, п.3). */
    public void deleteEntry(String login, long entryId) {
        Coach coach = currentCoach(login);
        Entry entry = entries.findById(entryId)
                .orElseThrow(() -> new SeedingException("Заявка не найдена: " + entryId));
        if (entry.getSubmittedByCoach() == null || !entry.getSubmittedByCoach().getId().equals(coach.getId())) {
            throw new SeedingException("Можно удалять только свои заявки.");
        }
        if (entry.getCategory().getStatus() != CategoryStatus.REGISTRATION_OPEN) {
            throw new SeedingException("Регистрация уже закрыта — заявку нельзя изменить.");
        }
        entries.delete(entry);
    }

    private static int size(List<Long> list) {
        return list == null ? 0 : list.size();
    }
}
