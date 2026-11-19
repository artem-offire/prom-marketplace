# Scrum у проєкті

Проєкт ведеться за спрощеним Scrum. Тут зібрані правила, щоб кожен учасник працював однаково.

## Документи

- [Ролі](roles.md)
- [Церемонії та артефакти](ceremonies.md)
- [Definition of Ready](definition-of-ready.md)
- [Definition of Done](definition-of-done.md)
- [Оцінювання](estimation.md)
- [Шаблон спринта](sprints/_template.md), [Sprint 0](sprints/sprint-0.md)

## Як усе пов'язано

1. Ідеї та вимоги потрапляють у **Product Backlog**: це відкриті issues без віхи (milestone).
2. На плануванні спринта issues отримують віху «Sprint N» і потрапляють у **Sprint Backlog**.
3. **Epic** (мітка `type:epic`) групує історії: у тілі epic є список завдань `- [ ] #n`.
4. Кожна історія має мітки `type:*`, `priority:*`, `area:*`, `sp:*`.
5. Робота рухається дошкою: Backlog, Ready, In progress, Review, Done.
6. Результат спринта: **Increment**, тобто робоча версія в гілці `main`.

## Ініціалізація GitHub

Мітки, віхи, epics та історії створює скрипт `scripts/github/bootstrap.sh` за даними `scripts/github/backlog.json`.

## Календар (орієнтир)

| Спринт | Дати | Тема |
|---|---|---|
| Sprint 0 | 2026-11-23 .. 2026-12-04 | Початковий код і процес |
| Sprint 1 | 2026-12-07 .. 2026-12-18 | Тарифи і вітрина |
| Sprint 2 | 2026-12-21 .. 2027-01-01 | Реклама (потужність близько 60%, свята) |
| Sprint 3 | 2027-01-04 .. 2027-01-15 | Замовлення, оплата, звітність |

Довжина спринта: 2 тижні.
