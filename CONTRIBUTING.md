# Участие в проекте / Contributing

[Русский](#по-русски) · [English](#in-english)

## По-русски

Спасибо за интерес к проекту! Вклад приветствуется.

### Перед началом
- Нужен **JDK 21+**. Maven ставить не обязательно — используйте wrapper (`./mvnw`).
- Соберите и прогоните тесты: `./mvnw test` — должно быть зелено.

### Как внести изменения
1. Сделайте форк и ветку: `git checkout -b feature/краткое-описание`.
2. Вносите изменения небольшими осмысленными коммитами.
3. **Добавляйте тесты** к новой логике. Движок сеток покрывается юнит-тестами;
   веб/сервисы — тестами на H2.
4. Убедитесь, что `./mvnw test` проходит.
5. Откройте Pull Request с описанием сути и причины изменения.

### Договорённости
- Стиль кода — как в окружающем коде (отступы, именование, плотность комментариев).
- Логику жеребьёвки/сеток не «зашивать» в код — она живёт в JSON-планах
  (`seeding-engine/src/main/resources/seeding/`) и проходит валидатор.
- **Не добавляйте персональные данные** (реальные ФИО, даты рождения и т.п.) в репозиторий —
  только синтетические примеры.
- Изменения схемы БД — только через новую миграцию Flyway (`app/src/main/resources/db/migration/`),
  существующие миграции не редактировать.

## In English

Thanks for your interest! Contributions are welcome.

### Prerequisites
- **JDK 21+**. Maven is optional — use the wrapper (`./mvnw`).
- Build and test: `./mvnw test` should be green.

### Workflow
1. Fork and branch: `git checkout -b feature/short-description`.
2. Keep commits small and meaningful.
3. **Add tests** for new logic (engine via unit tests; web/services via H2 tests).
4. Make sure `./mvnw test` passes.
5. Open a Pull Request describing what and why.

### Conventions
- Match the surrounding code style.
- Don't hard-code bracket/seeding logic — it lives in JSON plans and passes the validator.
- **Never add personal data** (real names, birth dates, etc.) — synthetic samples only.
- DB schema changes go through a new Flyway migration; never edit existing ones.
