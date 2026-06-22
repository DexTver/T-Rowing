package ru.rowing.app.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.rowing.app.repo.UserRepository;

import java.util.List;

/** Загружает пользователя из таблицы {@code users} для аутентификации (раздел 3 ТЗ). */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        ru.rowing.app.domain.User u = users.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
        return User.withUsername(u.getLogin())
                .password(u.getPasswordHash())
                .disabled(!u.isActive())
                .authorities(List.of(new SimpleGrantedAuthority(u.getRole().authority())))
                .build();
    }
}
