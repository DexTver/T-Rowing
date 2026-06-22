package ru.rowing.app.domain;

/**
 * Роль пользователя (раздел 3 ТЗ). VIEWER (зритель/участник) — анонимен и в БД не хранится,
 * поэтому в перечислении отсутствует.
 */
public enum Role {
    TRAINER,
    JUDGE,
    ADMIN;

    /** Authority для Spring Security (с префиксом ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
