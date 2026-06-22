package ru.rowing.app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.rowing.app.domain.Role;
import ru.rowing.app.domain.User;
import ru.rowing.app.repo.UserRepository;

/**
 * Создаёт начального администратора, если в БД ещё нет ни одного пользователя.
 * Логин/пароль — из переменных окружения {@code APP_ADMIN_LOGIN}/{@code APP_ADMIN_PASSWORD}
 * (по умолчанию admin/admin). В проде пароль обязательно сменить через админку.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminLogin;
    private final String adminPassword;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${app.admin.login:admin}") String adminLogin,
                       @Value("${app.admin.password:admin}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminLogin = adminLogin;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setLogin(adminLogin);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setDisplayName("Администратор");
        admin.setActive(true);
        users.save(admin);
        log.warn("Создан начальный администратор '{}'. Смените пароль после первого входа!", adminLogin);
    }
}
