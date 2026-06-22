-- Хранилище файлов сеток для управления через админку (раздел 7.4, п.3).
-- Переопределяет план из classpath по букве; пусто — используются встроенные ресурсы.
create table seeding_plan (
    id         bigint generated always as identity primary key,
    letter     varchar(1)  not null unique,
    json       text        not null,
    updated_at timestamptz not null default now()
);
