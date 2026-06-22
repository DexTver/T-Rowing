package ru.rowing.seeding;

/** Доменная ошибка движка сеток (например, нет плана для данного N, или неразрешимый источник посева). */
public class SeedingException extends RuntimeException {
    public SeedingException(String message) {
        super(message);
    }

    public SeedingException(String message, Throwable cause) {
        super(message, cause);
    }
}
