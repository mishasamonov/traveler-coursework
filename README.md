# Traveler — інформаційна система планування подорожей

Курсова робота з дисципліни «Сучасні фреймворки програмування».
Вебзастосунок на Spring Boot 3.3 (Spring MVC, Thymeleaf, Spring Data JPA, Spring Security,
Spring Boot Actuator) над базою даних TravelerAPI, створеною в лабораторних роботах з дисципліни
«Розподілені сховища даних» (https://github.com/mishasamonov/traveler-api-dds-lab1).

Автор: Самонов Михайло Володимирович, група ІН.м-62.

## Структура

| Модуль | Шар | Вміст |
|---|---|---|
| `traveler-core` | бізнес-логіка | предметна модель (records), порти доступу до даних, сервіси, правила, помилки |
| `traveler-persistence` | доступ до даних | JPA-сутності, Spring Data репозиторії, адаптери портів, Flyway-міграції |
| `traveler-web` | представлення | контролери Spring MVC, форми з валідацією, шаблони Thymeleaf, Spring Security, Actuator |

Міграції: `V1` — схема TravelerAPI з лабораторних без змін, `V2` — користувачі та власник плану,
`V3` (`db/demo`) — демонстраційні дані.

## Запуск

1. PostgreSQL 16 (порт 5433): `docker compose up -d`
2. Застосунок: конфігурація IntelliJ **Traveler App** або
   `mvn -pl traveler-web -am package -DskipTests` і `java -jar traveler-web/target/traveler.jar`
3. Відкрити http://localhost:8088

Тести: `mvn verify` (модульні + інтеграційні `*IT` на окремій базі `traveler_test`, потрібен
запущений контейнер PostgreSQL).

Необов'язково — REST API TravelerAPI з лабораторних на тій самій базі:
`docker compose --profile dds up -d`, запити в `requests/traveler-api-dds.http`.

## Демонстраційні облікові записи

| Роль | E-mail | Пароль |
|---|---|---|
| Адміністратор | admin@traveler.test | admin123 |
| Мандрівник | olena@traveler.test | olena123 |
| Мандрівник | andrii@traveler.test | andrii123 |

## Actuator

Публічні: `/actuator/health`, `/actuator/info`. Для ADMIN: `metrics`, `prometheus`, `loggers`,
`logfile`, `flyway`, `mappings`, `beans`, `env`, `configprops`, `conditions`, `threaddump`,
`scheduledtasks`, `httpexchanges` і власний вузол `travel` (статистика предметної області).
