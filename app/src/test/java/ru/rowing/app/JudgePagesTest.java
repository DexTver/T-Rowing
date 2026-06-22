package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Category;
import ru.rowing.app.domain.Competition;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.domain.Heat;
import ru.rowing.app.repo.HeatRepository;
import ru.rowing.app.repo.StageRepository;
import ru.rowing.app.service.JudgeService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JudgePagesTest {

    @Autowired MockMvc mvc;
    @Autowired JudgeService judge;
    @Autowired StageRepository stages;
    @Autowired HeatRepository heats;

    @Test
    void anonymousIsRedirectedFromJudgeArea() throws Exception {
        mvc.perform(get("/judge"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allJudgeTemplatesRender() throws Exception {
        mvc.perform(get("/judge")).andExpect(status().isOk());
        mvc.perform(get("/judge/competitions/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Создать соревнование")));

        // создание соревнования через POST с CSRF -> редирект на страницу управления
        mvc.perform(post("/judge/competitions").param("name", "Кубок").with(csrf()))
                .andExpect(status().is3xxRedirection());

        // подготовим категорию с протоколом для страниц категории/заезда/печати
        Competition comp = judge.createCompetition("Тест", null);
        Category cat = judge.addCategory(comp.getId(), null, "K1 муж 200", BoatClass.K1, Gender.MALE,
                200, null, null, false, false, null, null);
        judge.openRegistration(cat.getId());
        for (int i = 1; i <= 12; i++) {
            judge.addParticipant(cat.getId(), "Спортсмен " + i, 2008, "Регион", "Школа");
        }
        judge.closeRegistrationAndFormProtocol(cat.getId());
        Heat heat = heats.findByStageIdOrderByIndexInStage(
                stages.findByCategoryIdOrderByOrdinal(cat.getId()).get(0).getId()).get(0);

        mvc.perform(get("/judge/competitions/{id}", comp.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Категории")));
        mvc.perform(get("/judge/categories/{id}", cat.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Заявки")));
        mvc.perform(get("/judge/heats/{id}", heat.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Быстрый ввод")));
        mvc.perform(get("/judge/competitions/{id}/print", comp.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Протоколы")));
    }
}
