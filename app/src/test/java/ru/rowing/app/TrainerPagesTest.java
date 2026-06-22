package ru.rowing.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Role;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.CompetitionRepository;
import ru.rowing.app.repo.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrainerPagesTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CompetitionRepository competitions;

    private long competitionId;

    @BeforeEach
    void seed() {
        if (users.findByLogin("t1").isEmpty()) {
            User u = new User();
            u.setLogin("t1");
            u.setPasswordHash("x");
            u.setRole(Role.TRAINER);
            u.setDisplayName("Тренер Первый");
            users.save(u);
        }
        Competition c = new Competition();
        c.setName("Кубок для тренера");
        competitionId = competitions.save(c).getId();
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/trainer"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "t1", roles = "TRAINER")
    void trainerPagesRender() throws Exception {
        mvc.perform(get("/trainer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Подача заявок")));
        mvc.perform(get("/trainer/competitions/{id}", competitionId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Кубок для тренера")));
    }
}
