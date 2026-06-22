package ru.rowing.seeding;

import java.util.List;

/** Результат проверки файла сетки: список нарушений (пустой — файл валиден). */
public record ValidationResult(List<String> violations) {

    public boolean isValid() {
        return violations.isEmpty();
    }

    public void throwIfInvalid() {
        if (!isValid()) {
            throw new SeedingException("Файл сетки невалиден:\n - " + String.join("\n - ", violations));
        }
    }
}
