# Car Sharing

Spring Boot приложение для каршеринга: каталог автомобилей, регистрация и вход пользователей, аренды, платежи, дополнительные услуги, админские операции и демонстрационные endpoints для лабораторных работ.

Проект рассчитан на запуск с PostgreSQL.

## Стек

- Java 17
- Spring Boot 3.1.5
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL 16
- Maven Wrapper
- Docker и Docker Compose
- Render Blueprint
- GitHub Actions
- SonarCloud
- JaCoCo
- Swagger/OpenAPI

## Возможности

- Авторизация и регистрация через `/api/auth`
- Каталог автомобилей через `/api/cars`
- Дополнительные услуги через `/api/services`
- Аренды через `/api/rentals`
- Платежи через `/api/payments`
- Управление пользователями через `/api/users`
- Healthcheck через `/actuator/health`
- Swagger UI через `/swagger-ui/index.html`

## Быстрый запуск

Нужен установленный Docker Desktop.

```powershell
docker compose up --build
```

Приложение будет доступно:

```text
http://localhost:8082/
```

Healthcheck:

```text
http://localhost:8082/actuator/health
```

Остановить контейнеры:

```powershell
docker compose down
```

## Переменные окружения

Основные переменные для Docker Compose:

```text
SERVER_PORT=8082
POSTGRES_DB=car_sharing
DB_USERNAME=postgres
DB_PASSWORD=postgres
ADMIN_LOGIN=admin
ADMIN_PASSWORD=Admin123!
USER_INITIAL_CREDENTIAL=user-initial-credential
SPRING_PROFILES_ACTIVE=docker
```

Настоящий `.env` не хранится в репозитории и игнорируется Git.

## Локальная сборка и тесты

Запустить тесты:

```powershell
.\mvnw.cmd -B test
```

Собрать jar:

```powershell
.\mvnw.cmd -B package
```

Для обычного запуска без Docker должен быть доступен PostgreSQL.

## Docker

`docker-compose.yml` поднимает два сервиса:

- `db` - PostgreSQL 16
- `app` - Spring Boot приложение

Проверить состояние:

```powershell
docker compose ps
```

Посмотреть логи:

```powershell
docker logs carsharing-app
docker logs carsharing-db
```

## Render

Файл `render.yaml` описывает Blueprint:

- web service `car-sharing`
- PostgreSQL database `car-sharing-db`
- Docker runtime
- healthcheck `/actuator/health`

В Render нужно задать секретные значения:

```text
ADMIN_PASSWORD
USER_INITIAL_CREDENTIAL
```

## CI/CD

В проекте настроены GitHub Actions:

- `.github/workflows/sonar.yml` - сборка, тесты, JaCoCo и анализ SonarCloud
- `.github/workflows/ci-cd.yml` - build, test, deploy to Render, post-deploy healthcheck

Для деплоя через GitHub Actions нужны secrets:

```text
RENDER_DEPLOY_HOOK_URL
RENDER_HEALTHCHECK_URL
SONAR_TOKEN
```

## Важные файлы

- `Dockerfile` - сборка Docker-образа приложения
- `docker-compose.yml` - локальный запуск приложения с PostgreSQL
- `render.yaml` - деплой на Render
- `DEPLOYMENT.md` - краткая инструкция по деплою
- `src/main/resources/application.yaml` - основной конфиг приложения

## Статус

Основной сценарий запуска и сдачи лабораторной: Docker Compose локально и Render/GitHub Actions для деплоя.
