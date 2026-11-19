# prom-marketplace

[![CI](https://github.com/OWNER/prom-marketplace/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/prom-marketplace/actions/workflows/ci.yml)

Навчальний маркетплейс на Java 21 і Spring Boot 3 з **моделлю монетизації на зразок Prom.ua**: тарифні пакети продавця, реклама з оплатою за клік, комісія за замовлення, комісія за онлайн-оплату. Усе обліковується в подвійному записі (double-entry ledger).

> Проєкт навчальний і не пов'язаний з Prom.ua. Назва, логотипи і торгові марки Prom.ua в коді та інтерфейсі не використовуються. Усі ціни, ставки й відсотки в проєкті **ілюстративні**.
> Замініть `OWNER` у посиланнях на ваш GitHub-акаунт.

## Що вміє проєкт (стартовий обсяг, Tier A)

- Реєстрація продавця: автоматично відкриваються рахунки в обліку і підписка на пакет FREE.
- Баланс продавця: поповнення з ідемпотентним ключем, історія проводок.
- Тарифні пакети FREE, START, PRO, BUSINESS: ліміт товарів, видимість у вітрині, доступ до функцій.
- Категорії з комісією та мінімальною ставкою; товари з лімітом пакета; вітрина з пошуком.
- Реклама: кампанії, ставки, денний бюджет, рейтинг оголошень (ставка x показник якості), списання за клік.
- Замовлення зі станами NEW, CONFIRMED, SHIPPED, COMPLETED, CANCELLED, RETURNED; комісія знімається при підтвердженні, повертається при скасуванні або поверненні.
- Онлайн-оплата через заглушку провайдера: комісія за платіж, повернення оплати.

Усе, що не входить у стартовий обсяг (Tier B), описано як issues у `scripts/github/backlog.json`.

## Швидкий старт

Потрібні: JDK 21 і Git. Maven встановлювати не потрібно (є Maven Wrapper).

```bash
git clone https://github.com/OWNER/prom-marketplace.git
cd prom-marketplace
./mvnw -B verify                # збірка і всі тести (H2 в пам'яті)
./mvnw spring-boot:run          # запуск на http://localhost:8080
curl http://localhost:8080/actuator/health
```

### Запуск на PostgreSQL 16

```bash
docker run --rm -e POSTGRES_USER=marketplace -e POSTGRES_PASSWORD=marketplace \
  -e POSTGRES_DB=marketplace -p 5432:5432 postgres:16
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Змінні середовища: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

## Приклад сценарію через API

```bash
# 1. Реєстрація продавця
curl -s -X POST localhost:8080/api/v1/sellers -H 'Content-Type: application/json' \
  -d '{"name":"Мій магазин","email":"shop@example.com","phone":"+380501234567"}'

# 2. Поповнення балансу (ключ ідемпотентності захищає від подвійного списання)
curl -s -X POST localhost:8080/api/v1/sellers/1/balance/top-ups -H 'Content-Type: application/json' \
  -d '{"amount":1000.00,"idempotencyKey":"demo-1"}'

# 3. Купівля пакета START на місяць
curl -s -X POST localhost:8080/api/v1/sellers/1/subscription -H 'Content-Type: application/json' \
  -d '{"planCode":"START","period":"MONTHLY"}'

# 4. Баланс після покупки
curl -s localhost:8080/api/v1/sellers/1/balance
```

Усі помилки повертаються у форматі RFC 7807 з полем `code`, наприклад `INSUFFICIENT_FUNDS`.

## Структура репозиторію

| Шлях | Призначення |
|---|---|
| `src/main/java/ua/marketplace/` | код за модулями: `common`, `seller`, `billing`, `subscription`, `catalog`, `advertising`, `order`, `payment` |
| `src/main/resources/db/migration/` | міграції Flyway V1..V7 |
| `docs/` | архітектура, монетизація, ADR, Scrum-документи |
| `scripts/github/` | `backlog.json` і `bootstrap.sh` для створення міток, віх та issues |
| `.github/` | шаблони issues і PR, CI, Dependabot |

## Документація

- [Архітектура](docs/architecture.md)
- [Модель монетизації](docs/monetization.md)
- [Архітектурні рішення (ADR)](docs/adr/0001-modular-monolith.md)
- [Scrum-процес](docs/scrum/README.md)
- [Правила внеску](CONTRIBUTING.md)

## Ініціалізація GitHub (мітки, віхи, беклог)

```bash
gh auth login
REPO=OWNER/prom-marketplace ./scripts/github/bootstrap.sh
```

Скрипт ідемпотентний: повторний запуск не створює дублікатів.

## Ризики і обмеження

- Spring Boot 3.5 незабаром завершить відкриту підтримку: міграція на Spring Boot 4 винесена в беклог.
- Торгова марка: використовується лише фраза «модель монетизації на зразок Prom.ua».
- Ілюстративні числа: тарифи, ставки комісій і мінімальні ставки не є офіційними.
- Розбіжність H2 і PostgreSQL: інтеграційні тести на PostgreSQL (Testcontainers) заплановані в Sprint 1.
- Конкурентність на балансах: захищена блокуванням рахунків і оптимістичною версією, але потребує навантажувального тестування.
- Округлення по рядках замовлення (ADR 0003): підсумок не округлюється вдруге.
- Баланс продавця може піти в мінус через комісію за замовлення (це свідоме рішення, дивись docs/monetization.md).
- Немає автентифікації: не розгортайте застосунок у відкритій мережі.
- Sprint 0 завеликий, а Sprint 2 припадає на новорічні свята (потужність близько 60%).
- Юридичні питання (ПРРО, ПДВ, вибір платіжного провайдера) не вирішені; для цього є спайк.

## Ліцензія

Ліцензія буде визначена окремо.
