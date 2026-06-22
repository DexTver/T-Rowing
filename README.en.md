# 🚣 T-Rowing — canoe/kayak regatta management system

[Русский](README.md) · [English](README.en.md)

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

A web system for running canoe/kayak sprint regattas: managing competitors, producing start
protocols and — the core value — **automatic draw and bracket generation** (preliminaries →
semifinals → finals) according to the official qualification system.

> A practical project for a specific rowing club. The bracket logic lives in an engine that
> interprets external JSON plan files instead of being hard-coded.

## Features

| Role | Capabilities |
|---|---|
| **Spectator** (no login) | Competition list, competition page as a start protocol (heats grouped by day, expand in place), results, filters by athlete/coach/category/region/club |
| **Coach** | Submit and edit own entries during the registration window: substring search, add athletes, age-mismatch warning |
| **Referee** | "Create competition" (multi-day, categories, finals B/C toggles), close registration → draw, quick result entry, confirmed next-stage formation; **import a start protocol from Excel** |
| **Admin** | Manage accounts and master data, upload/validate bracket JSON files |

## Tech stack

- **Java 21**, **Spring Boot 3.5** (Web, Security, Data JPA), **Thymeleaf** + a little vanilla JS
- **PostgreSQL** + **Flyway** (schema versioned via migrations)
- **Apache POI** — Excel protocol import
- **Maven** build (wrapper included), **JUnit 5** tests (some on H2)
- **Docker** / docker-compose

## Architecture

Two Maven modules:

- **`seeding-engine`** — pure draw/seeding logic, **no DB, no web**. Interprets JSON plans (A–N),
  validates their invariants, and forms stages deterministically (from a stored random seed).
- **`app`** — Spring Boot: `web` (controllers/Thymeleaf) → `service` (business logic, the category
  state machine, Excel import) → `repository` (JPA) → PostgreSQL. Depends on `seeding-engine`.

## Quick start (Docker)

```bash
docker compose up --build
```

App: <http://localhost:8080> · initial login **admin / admin** (change it in production!).

To see a populated UI (demo coach `trainer/trainer`):

```bash
APP_SEED_DEMO=true docker compose up --build
```

## Build & test (no Docker, needs JDK 21+)

```bash
./mvnw test                       # all tests (engine + app on H2)
./mvnw -pl seeding-engine test    # seeding engine only
```

Running the app needs PostgreSQL; configure it via env vars
(`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`).

## Excel protocol import

Referees author protocols in Excel with a reference structure: a **"база"** (database) sheet
(number, full name, birth year, rank, region, club, coach) and a protocol sheet where each athlete
is referenced **by number**, with the name pulled from the database via a formula. The importer
understands this: it loads the whole database, links heats to athletes by number, and reconstructs
days/categories/stages.

A synthetic example of the format is in
[`samples/example-protocol.xlsx`](samples/example-protocol.xlsx) — real athlete data is **not**
included in the repository (see [samples/README.md](samples/README.md)).

## Seeding engine

Each plan A–N is a JSON file (`seeding-engine/src/main/resources/seeding/plan_*.json`) describing the
number of preliminary heats, heat sizes and the **seeding** of every stage transition. Three selector
types cover all wordings of the source system:

- `place` — "place P of heat H";
- `best_time` — "fastest among those not advanced by place";
- `place_by_time` — cross-heat "X-th of the Y-th places" (for finals B/C).

Uploaded files pass a validator (lane completeness, in = out balance, full coverage of 10–135
participants with no gaps). See [seeding-engine/README.md](seeding-engine/README.md).

## Project layout

```
seeding-engine/   draw & bracket engine (pure logic + plans A–N)
app/              Spring Boot: domain, repositories, services, controllers, templates, migrations
samples/          synthetic protocol example (no personal data)
docker-compose.yml, Dockerfile
```

## Security

- Server-side RBAC (Spring Security), BCrypt passwords, CSRF enabled.
- Default credentials (`admin/admin`) and DB settings are for local development —
  **change them in production** (via environment variables).

## Status

All functional parts are implemented: the seeding engine, public part, coach, referee (incl. Excel
import) and admin. Deployment ships as a basic `docker-compose`; reverse-proxy/TLS/backups are next.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Issues and PRs are welcome.

## License

[Apache License 2.0](LICENSE).
