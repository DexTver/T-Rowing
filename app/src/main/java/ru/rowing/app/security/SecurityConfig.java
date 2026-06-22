package ru.rowing.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности (раздел 3, 10 ТЗ): сессионная аутентификация, пароли BCrypt,
 * RBAC на уровне сервера, CSRF включён по умолчанию.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // защищённые зоны по ролям
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // ADMIN — надмножество: допускаем к судейской зоне (раздел 3, админ ведёт и пользователей)
                        .requestMatchers("/judge/**").hasAnyRole("JUDGE", "ADMIN")
                        .requestMatchers("/trainer/**").hasAnyRole("TRAINER", "ADMIN")
                        // публичная часть и статика — открыты (защита капчей будет на формах, раздел 3)
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());
        return http.build();
    }
}
