-- Начальная схема (раздел 5 ТЗ + многодневность). PostgreSQL.
-- Enum-поля хранятся как varchar с CHECK для переносимости; в JPA — @Enumerated(STRING).

create table users (
    id            bigint generated always as identity primary key,
    login         varchar(64)  not null unique,
    password_hash varchar(100) not null,
    role          varchar(16)  not null check (role in ('TRAINER', 'JUDGE', 'ADMIN')),
    display_name  varchar(128),
    is_active     boolean      not null default true
);

create table coach (
    id        bigint generated always as identity primary key,
    full_name varchar(128) not null,
    user_id   bigint references users (id)
);

create table athlete (
    id           bigint generated always as identity primary key,
    full_name    varchar(128) not null,
    birth_year   int          not null,
    rank         varchar(64),
    region       varchar(128),
    sport_school varchar(128),
    coach_id     bigint references coach (id)
);
create index idx_athlete_coach on athlete (coach_id);

create table competition (
    id          bigint generated always as identity primary key,
    name        varchar(200) not null,
    description text,
    starts_at   timestamptz,
    status      varchar(16)  not null default 'SCHEDULED'
        check (status in ('SCHEDULED', 'LIVE', 'FINISHED'))
);

-- Многодневное соревнование: день + время старта; к дню привязываются категории.
create table competition_day (
    id             bigint generated always as identity primary key,
    competition_id bigint not null references competition (id) on delete cascade,
    ordinal        int    not null,
    day_date       date   not null,
    start_time     time
);
create index idx_day_competition on competition_day (competition_id);

create table category (
    id                     bigint generated always as identity primary key,
    competition_id         bigint       not null references competition (id) on delete cascade,
    competition_day_id     bigint references competition_day (id),
    name                   varchar(200) not null,
    birth_year_from        int,
    birth_year_to          int,
    boat_class             varchar(4)   not null check (boat_class in ('K1', 'K2', 'C1', 'C2')),
    gender                 varchar(8)   not null check (gender in ('MALE', 'FEMALE', 'MIXED')),
    distance_m             int          not null check (distance_m in (200, 500, 1000, 5000)),
    final_b_enabled        boolean      not null default false,
    final_c_enabled        boolean      not null default false,
    status                 varchar(24)  not null default 'DRAFT'
        check (status in ('DRAFT', 'REGISTRATION_OPEN', 'PROTOCOL_FORMED', 'PRELIMS_RUNNING',
                          'SEMIS_READY', 'SEMIS_RUNNING', 'FINALS_READY', 'FINALS_RUNNING', 'FINISHED')),
    registration_opens_at  timestamptz,
    registration_closes_at timestamptz,
    plan                   varchar(1),
    active_variant         varchar(8),
    draw_seed              bigint,
    default_interval_sec   int          not null default 180
);
create index idx_category_competition on category (competition_id);
create index idx_category_day on category (competition_day_id);

create table entry (
    id                    bigint generated always as identity primary key,
    category_id           bigint      not null references category (id) on delete cascade,
    athlete_id            bigint      not null references athlete (id),
    submitted_by_coach_id bigint references coach (id),
    created_at            timestamptz not null default now(),
    constraint uq_entry_category_athlete unique (category_id, athlete_id)
);
create index idx_entry_category on entry (category_id);

create table stage (
    id          bigint generated always as identity primary key,
    category_id bigint      not null references category (id) on delete cascade,
    type        varchar(16) not null check (type in ('PRELIM', 'SEMIFINAL', 'FINAL')),
    ordinal     int         not null
);
create index idx_stage_category on stage (category_id);

create table heat (
    id              bigint generated always as identity primary key,
    stage_id        bigint  not null references stage (id) on delete cascade,
    number          int     not null,
    index_in_stage  int     not null,
    final_letter    varchar(1),
    scheduled_start timestamptz,
    mass_start      boolean not null default false
);
create index idx_heat_stage on heat (stage_id);

create table result (
    id         bigint generated always as identity primary key,
    heat_id    bigint      not null references heat (id) on delete cascade,
    athlete_id bigint      not null references athlete (id),
    lane       int         not null,
    status     varchar(16) not null default 'NOT_STARTED'
        check (status in ('NOT_STARTED', 'OK', 'DNF', 'DNS', 'DSQ')),
    time_ms    bigint
);
create index idx_result_heat on result (heat_id);
