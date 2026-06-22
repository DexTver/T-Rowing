package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Athlete;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Entry;
import ru.rowing.app.domain.Role;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.AthleteRepository;
import ru.rowing.app.repo.CategoryRepository;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.EntryRepository;
import ru.rowing.app.repo.UserRepository;
import ru.rowing.app.service.AdminService;
import ru.rowing.app.service.PlanCatalog;
import ru.rowing.seeding.SeedingException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminFlowTest {

    @Autowired AdminService admin;
    @Autowired PlanCatalog catalog;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired CompetitionRepository competitions;
    @Autowired CategoryRepository categories;
    @Autowired AthleteRepository athletes;
    @Autowired EntryRepository entries;

    @Test
    void createsUserWithEncodedPassword() {
        User u = admin.createUser("judge1", "secret", Role.JUDGE, "Судья");
        assertEquals(Role.JUDGE, u.getRole());
        assertTrue(encoder.matches("secret", users.findById(u.getId()).orElseThrow().getPasswordHash()));
        assertThrows(SeedingException.class, () -> admin.createUser("judge1", "x", Role.JUDGE, "dup"));
    }

    @Test
    void cannotRemoveLastAdmin() {
        long adminId = users.findByLogin("admin").orElseThrow().getId();
        assertThrows(SeedingException.class, () -> admin.deleteUser(adminId, "someoneelse"));
        assertThrows(SeedingException.class, () -> admin.updateUser(adminId, Role.JUDGE, "x", true));
    }

    @Test
    void cannotDeleteSelf() {
        User j = admin.createUser("self", "pw", Role.JUDGE, null);
        assertThrows(SeedingException.class, () -> admin.deleteUser(j.getId(), "self"));
    }

    @Test
    void athleteInUseCannotBeDeleted() {
        Competition comp = new Competition();
        comp.setName("C");
        comp = competitions.save(comp);
        Category cat = new Category();
        cat.setCompetition(comp);
        cat.setName("cat");
        cat.setBoatClass(ru.rowing.app.domain.BoatClass.K1);
        cat.setGender(ru.rowing.app.domain.Gender.MALE);
        cat.setDistanceM(500);
        cat = categories.save(cat);

        Athlete a = new Athlete();
        a.setFullName("Гребец");
        a.setBirthYear(2009);
        a = athletes.save(a);
        Entry e = new Entry();
        e.setCategory(cat);
        e.setAthlete(a);
        entries.save(e);

        long aid = a.getId();
        assertThrows(SeedingException.class, () -> admin.deleteAthlete(aid));

        // без ссылок — удаляется
        Athlete free = new Athlete();
        free.setFullName("Свободный");
        free.setBirthYear(2009);
        free = athletes.save(free);
        long freeId = free.getId();
        admin.deleteAthlete(freeId);
        assertTrue(athletes.findById(freeId).isEmpty());
    }

    @Test
    void planUploadValidatesBeforeApplying() throws IOException {
        // валидный план A (из ресурса) принимается
        String validA = resource("/seeding/plan_A.json");
        catalog.replace("A", validA);
        assertEquals("A", catalog.planByLetter("A").plan());

        // несовпадение буквы — отказ
        assertThrows(SeedingException.class, () -> catalog.replace("B", validA));

        // невалидная раскладка (не заполнены дорожки) — отказ
        String broken = """
                { "plan":"A","participants":{"min":10,"max":18},"lanes":9,
                  "prelims":{"count":2,"sizing":"even_desc"},
                  "stages":[{"stage":"semifinal","heats_count":1,"variants":{"1":[
                    {"target":{"heat":1,"lane":1},"source":{"type":"place","place":4,"from":{"stage":"prelim","heat":1}}}
                  ]}}] }
                """;
        assertThrows(SeedingException.class, () -> catalog.replace("A", broken));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = AdminFlowTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
