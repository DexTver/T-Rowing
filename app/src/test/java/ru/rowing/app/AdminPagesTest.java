package ru.rowing.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPagesTest {

    @Autowired MockMvc mvc;

    @Test
    void anonymousIsRedirected() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "t", roles = "TRAINER")
    void trainerIsForbidden() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPagesRender() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Администрирование")));
        mvc.perform(get("/admin/users")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Пользователи")));
        mvc.perform(get("/admin/coaches")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Тренеры")));
        mvc.perform(get("/admin/athletes")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Спортсмены")));
        mvc.perform(get("/admin/plans")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Файлы сеток")));
    }
}
