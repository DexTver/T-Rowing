package ru.rowing.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Coach;
import ru.rowing.app.domain.Role;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CoachRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.ResultRepository;
import ru.rowing.app.repo.UserRepository;
import ru.rowing.seeding.SeedingException;

/**
 * Администрирование (раздел 7.4 ТЗ): учётные записи, мастер-данные (тренеры, спортсмены),
 * удаление соревнований. Управление файлами сеток — в {@link PlanCatalog}.
 */
@Service
@Transactional
public class AdminService {

    private final UserRepository users;
    private final CoachRepository coaches;
    private final AthleteRepository athletes;
    private final CompetitionRepository competitions;
    private final EntryRepository entries;
    private final ResultRepository results;
    private final PasswordEncoder encoder;

    public AdminService(UserRepository users, CoachRepository coaches, AthleteRepository athletes,
                        CompetitionRepository competitions, EntryRepository entries, ResultRepository results,
                        PasswordEncoder encoder) {
        this.users = users;
        this.coaches = coaches;
        this.athletes = athletes;
        this.competitions = competitions;
        this.entries = entries;
        this.results = results;
        this.encoder = encoder;
    }

    // --- пользователи ---------------------------------------------------------

    public User createUser(String login, String rawPassword, Role role, String displayName) {
        if (login == null || login.isBlank()) {
            throw new SeedingException("Логин обязателен.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new SeedingException("Пароль обязателен.");
        }
        if (users.existsByLogin(login)) {
            throw new SeedingException("Логин «" + login + "» уже занят.");
        }
        User u = new User();
        u.setLogin(login.trim());
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        u.setDisplayName(displayName);
        u.setActive(true);
        return users.save(u);
    }

    public void updateUser(long id, Role role, String displayName, boolean active) {
        User u = user(id);
        boolean losingAdmin = u.getRole() == Role.ADMIN && (role != Role.ADMIN || !active);
        if (losingAdmin && users.countByRole(Role.ADMIN) <= 1) {
            throw new SeedingException("Нельзя снять или отключить последнего администратора.");
        }
        u.setRole(role);
        u.setDisplayName(displayName);
        u.setActive(active);
    }

    public void resetPassword(long id, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new SeedingException("Пароль обязателен.");
        }
        user(id).setPasswordHash(encoder.encode(rawPassword));
    }

    public void deleteUser(long id, String currentLogin) {
        User u = user(id);
        if (u.getLogin().equals(currentLogin)) {
            throw new SeedingException("Нельзя удалить собственную учётную запись.");
        }
        if (u.getRole() == Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new SeedingException("Нельзя удалить последнего администратора.");
        }
        // отвязать тренера-человека от удаляемой учётки, чтобы не нарушить FK
        coaches.findByUserId(id).ifPresent(c -> c.setUser(null));
        users.delete(u);
    }

    // --- тренеры --------------------------------------------------------------

    public Coach createCoach(String fullName, Long userId) {
        Coach c = new Coach();
        c.setFullName(fullName);
        c.setUser(userId != null ? user(userId) : null);
        return coaches.save(c);
    }

    public void updateCoach(long id, String fullName, Long userId) {
        Coach c = coaches.findById(id).orElseThrow(() -> notFound("Тренер", id));
        c.setFullName(fullName);
        c.setUser(userId != null ? user(userId) : null);
    }

    public void deleteCoach(long id) {
        Coach c = coaches.findById(id).orElseThrow(() -> notFound("Тренер", id));
        if (!athletes.findByCoachId(id).isEmpty()) {
            throw new SeedingException("У тренера есть спортсмены — сначала переназначьте их.");
        }
        coaches.delete(c);
    }

    // --- спортсмены -----------------------------------------------------------

    public void updateAthlete(long id, String fullName, int birthYear, String rank,
                              String region, String school, Long coachId) {
        Athlete a = athletes.findById(id).orElseThrow(() -> notFound("Спортсмен", id));
        a.setFullName(fullName);
        a.setBirthYear(birthYear);
        a.setRank(rank);
        a.setRegion(region);
        a.setSportSchool(school);
        a.setCoach(coachId != null ? coaches.findById(coachId).orElseThrow(() -> notFound("Тренер", coachId)) : null);
    }

    public void deleteAthlete(long id) {
        Athlete a = athletes.findById(id).orElseThrow(() -> notFound("Спортсмен", id));
        if (entries.existsByAthleteId(id) || results.existsByAthleteId(id)) {
            throw new SeedingException("Спортсмен участвует в заявках или результатах — удаление запрещено.");
        }
        athletes.delete(a);
    }

    // --- соревнования ---------------------------------------------------------

    public void deleteCompetition(long id) {
        competitions.deleteById(id);
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> notFound("Пользователь", id));
    }

    private static SeedingException notFound(String what, long id) {
        return new SeedingException(what + " не найдено: " + id);
    }
}
