<div align="center">

# 🚗 CAR SHARING API

### REST API сервис для управления каршерингом

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=500&size=24&duration=3000&pause=500&color=2F81F7&center=true&vCenter=true&width=535&lines=Java+17;Spring+Boot+3.1.5;PostgreSQL+%7C+JPA+Hibernate;REST+API;Checkstyle+%26+SonarCloud;N%2B1+Problem+%7C+Transactions" alt="Typing SVG" />

[![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-%236DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-%23316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Hibernate](https://img.shields.io/badge/Hibernate-6.4-%2359666C?style=for-the-badge&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-Quality%20Gate-%23F3702A?style=for-the-badge&logo=sonarcloud&logoColor=white)](https://sonarcloud.io/summary/overall?id=AlexGurianov207_car-sharing&branch=main)

</div>

---

## 📋 СОДЕРЖАНИЕ

- [📖 О проекте](#-о-проекте)
- [✨ Функциональность](#-функциональность)
- [📊 Модель данных и ER-диаграмма](#-модель-данных-и-er-диаграмма)
- [🛠️ Технологический стек](#️-технологический-стек)
- [🔍 Ключевые особенности реализации](#-ключевые-особенности-реализации)
- [📈 Производительность и оптимизация](#-производительность-и-оптимизация)
- [🔗 SonarCloud анализ](#-sonarcloud-анализ)
- [🚀 Запуск проекта](#-запуск-проекта)

---

## 📖 О ПРОЕКТЕ

Данный проект представляет собой **полноценное REST API** для управления автопарком каршеринга, разработанное в рамках лабораторных работ по дисциплине *"Программирование на языках высокого уровня"*.

**Архитектура:** Классическая многослойная (Controller → Service → Repository) с подключением реляционной базы данных PostgreSQL и использованием JPA/Hibernate для объектно-реляционного отображения.

### 🎯 Что реализовано:

- ✅ Подключение и настройка PostgreSQL
- ✅ 6 связанных сущностей с отношениями `@OneToMany`, `@ManyToMany`, `@OneToOne`
- ✅ Полный CRUD для всех сущностей
- ✅ Демонстрация и решение проблемы N+1 через `@EntityGraph`
- ✅ Работа с транзакциями (`@Transactional`) и демонстрация rollback при ошибках
- ✅ Каскадные операции (CascadeType.PERSIST) для связанных сущностей
- ✅ Статический анализ кода через Checkstyle и SonarCloud

---

## ✨ ФУНКЦИОНАЛЬНОСТЬ

### 🚘 Управление автомобилями (`/api/cars`)

| Метод   | Endpoint                               | Описание                                              |
|---------|----------------------------------------|-------------------------------------------------------|
| `GET`   | `/api/cars`                            | Получение списка всех автомобилей (с фильтрацией)     |
| `GET`   | `/api/cars?status=AVAILABLE`           | Фильтрация по статусу                                 |
| `GET`   | `/api/cars?brand={brand}&model={model}`| Поиск по марке и модели                               |
| `GET`   | `/api/cars?maxPrice={price}`           | Поиск автомобилей с ценой до указанной                |
| `GET`   | `/api/cars/{id}`                       | Детальная информация об автомобиле                    |
| `GET`   | `/api/cars/by-license/{plate}`         | Поиск по госномеру                                    |
| `POST`  | `/api/cars`                            | Добавление нового автомобиля                          |
| `PUT`   | `/api/cars/{id}`                       | Обновление информации об автомобиле                   |
| `DELETE`| `/api/cars/{id}`                       | Удаление автомобиля (только если не в аренде)         |

### 👤 Управление пользователями (`/api/users`)

| Метод   | Endpoint                    | Описание                              |
|---------|-----------------------------|---------------------------------------|
| `GET`   | `/api/users`                | Список всех пользователей             |
| `GET`   | `/api/users/{id}`           | Информация о пользователе             |
| `GET`   | `/api/users/by-email?email=`| Поиск по email                        |
| `POST`  | `/api/users`                | Регистрация нового пользователя       |
| `PUT`   | `/api/users/{id}`           | Обновление данных пользователя        |
| `PATCH` | `/api/users/{id}/status`    | Изменение статуса (ACTIVE/BLOCKED)    |
| `DELETE`| `/api/users/{id}`           | Удаление пользователя                  |

### 📅 Управление арендой (`/api/rentals`)

| Метод   | Endpoint                       | Описание                                    |
|---------|--------------------------------|---------------------------------------------|
| `GET`   | `/api/rentals/active`          | Все активные аренды                         |
| `GET`   | `/api/rentals/user/{userId}`   | Аренды пользователя                          |
| `GET`   | `/api/rentals/car/{carId}`     | История аренд автомобиля                     |
| `GET`   | `/api/rentals/{id}`            | Детали аренды                                |
| `POST`  | `/api/rentals`                 | Начать аренду                                |
| `PATCH` | `/api/rentals/{id}/complete`   | Завершить аренду (авто создание платежа)     |
| `DELETE`| `/api/rentals/{id}`            | Удалить аренду (только завершенную)          |

### 💳 Платежи (`/api/payments`)

| Метод   | Endpoint                 | Описание                      |
|---------|--------------------------|-------------------------------|
| `GET`   | `/api/payments`          | Все платежи                   |
| `GET`   | `/api/payments/{id}`     | Детали платежа                |
| `POST`  | `/api/payments`          | Создать платеж (для аренды)   |
| `PATCH` | `/api/payments/{id}/refund` | Возврат платежа             |
| `DELETE`| `/api/payments/{id}`     | Удалить платеж                 |

### 🛠️ Дополнительные услуги (`/api/services`)

| Метод   | Endpoint                          | Описание                          |
|---------|-----------------------------------|-----------------------------------|
| `GET`   | `/api/services`                   | Все услуги (с фильтрацией)        |
| `GET`   | `/api/services?category=SAFETY`   | Фильтр по категории               |
| `GET`   | `/api/services?onlyActive=true`   | Только активные услуги            |
| `POST`  | `/api/services`                   | Создать услугу                    |
| `PATCH` | `/api/services/{id}/status`       | Активировать/деактивировать услугу |

### 🧪 Демонстрационные endpoints (для лабораторной)

| Метод   | Endpoint                          | Описание                                      |
|---------|-----------------------------------|-----------------------------------------------|
| `GET`   | `/api/rentals/demo/n-plus-one`    | Демонстрация проблемы N+1                     |
| `GET`   | `/api/rentals/demo/solution`      | Решение N+1 через `@EntityGraph`              |
| `POST`  | `/api/rentals/demo/without-tx`    | Создание аренды БЕЗ транзакции (с частичным сохранением при ошибке) |
| `POST`  | `/api/rentals/demo/with-tx`       | Создание аренды С транзакцией (полный rollback при ошибке) |

---

## 📊 МОДЕЛЬ ДАННЫХ И ER-ДИАГРАММА

### Сущности (6)

| Сущность        | Описание                              | Связи                                      |
|-----------------|---------------------------------------|--------------------------------------------|
| **User**        | Пользователь сервиса                  | `@OneToMany` → Rental                      |
| **Car**         | Автомобиль в автопарке                | `@OneToMany` → Rental, `@ManyToMany` → ExtraService |
| **Rental**      | Аренда автомобиля                     | `@ManyToOne` → User, Car; `@ManyToMany` → ExtraService; `@OneToOne` → Payment |
| **ExtraService**| Дополнительная услуга (страховка, детское кресло и т.д.) | `@ManyToMany` → Car, Rental |
| **Payment**     | Платеж за аренду                      | `@OneToOne` → Rental                       |

---

Дополнительные endpoints:
- GET /api/rentals/search/jpql
- GET /api/rentals/search/native
- GET /api/rentals/search/paged

Демо endpoints:
- GET /api/rentals/demo/n-plus-one
- GET /api/rentals/demo/solution
- POST /api/rentals/demo/without-tx
- POST /api/rentals/demo/with-tx

## Поиск и фильтрация аренд
Реализованы два варианта сложного поиска с фильтрацией по вложенным сущностям (rental -> car, rental -> user):

1. JPQL-запрос (/api/rentals/search/jpql)
2. Native SQL-запрос (/api/rentals/search/native)

Параметры фильтрации:
- carBrand (опционально)
- userId (опционально)
- status (опционально)

## Пагинация
Пагинация вынесена в отдельный endpoint:
- GET /api/rentals/search/paged

Поддерживаются стандартные параметры Spring Pageable:
- page
- size
- sort (например, sort=startTime,desc)

## Кэш запросов (in-memory HashMap)
В сервисе аренд используется индекс ранее выполненных запросов:
- Map<RentalSearchCacheKey, Page<RentalResponse>>

Ключ кэша составной и включает:
- фильтры (carBrand, userId, status)
- параметры страницы (page, size, sort)
- тип запроса (JPQL/NATIVE)

Для ключа используется record, поэтому equals()/hashCode() корректны по всем полям.

Для записи в кэш используется отдельный метод putToIndex(...) с логикой PUT/UPDATE.

## Инвалидация кэша
Кэш очищается при изменении данных, влияющих на поиск:
- в RentalService после create/complete/delete
- в CarService после изменений автомобилей

## Логи кэша
В консоли доступны диагностические сообщения:
- [CACHE] MISS — ключ не найден, идём в БД
- [CACHE] PUT — добавили новое значение
- [CACHE] UPDATE — перезаписали существующий ключ
- [CACHE] HIT — ответ взят из кэша
- [CACHE] INVALIDATE — кэш очищен после изменений

## 🛠️ ТЕХНОЛОГИЧЕСКИЙ СТЕК

<div align="center">

| Категория           | Технологии                                                                                                                                                                                                                                                                                     |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Язык**            | ![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)                                                                                                                                                                                            |
| **Фреймворк**       | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)                                                |
| **База данных**     | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-316192?style=flat-square&logo=postgresql&logoColor=white) ![Hibernate](https://img.shields.io/badge/Hibernate-6.4-59666C?style=flat-square&logo=hibernate&logoColor=white)                                                          |
| **Сборка**          | ![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apache-maven&logoColor=white)                                                                                                                                                                                    |
| **Архитектура**     | REST API, Controller-Service-Repository, DTO Pattern, Mapper Layer                                                                                                                                                                                                                             |
| **Утилиты**         | ![Lombok](https://img.shields.io/badge/Lombok-2022-ff69b4?style=flat-square)                                                                                                                                                                                                                   |
| **Качество кода**   | ![Checkstyle](https://img.shields.io/badge/Checkstyle-10.12-00BFFF?style=flat-square) ![SonarCloud](https://img.shields.io/badge/SonarCloud-Analysis-F3702A?style=flat-square&logo=sonarcloud&logoColor=white)                                                                                 |

</div>

---

## 🔍 КЛЮЧЕВЫЕ ОСОБЕННОСТИ РЕАЛИЗАЦИИ

### 1. **Работа с каскадами (CascadeType)**
   - Использован `CascadeType.PERSIST` для связи `Rental → Payment`
   - Это гарантирует, что при завершении аренды платеж создается автоматически
   - Отсутствие `CascadeType.REMOVE` защищает от случайного удаления платежей

### 2. **Оптимизация загрузки данных (FetchType)**
   - Все `@ManyToOne` и `@OneToOne` связи настроены на `FetchType.LAZY`
   - Это предотвращает загрузку ненужных данных и повышает производительность
   - Для конкретных запросов используется `@EntityGraph` для eager-загрузки

### 3. **Демонстрация N+1 проблемы и её решение**
   ```java
   // Проблема: для каждого rental выполняется отдельный запрос
   @Query("SELECT r FROM Rental r")
   List<Rental> findAllSlow();  // N+1 запросов
   
   // Решение: один запрос с JOIN через EntityGraph
   @EntityGraph(attributePaths = {"user", "car", "selectedServices"})
   List<Rental> findAll();  // 1 запрос
```

### 4. **Транзакционность**
```java
// Без @Transactional - частичное сохранение при ошибке
public RentalResponse createRentalWithoutTransaction(...) {
    rentalRepository.save(rental);  // Сохранится даже при ошибке ниже
}

// С @Transactional - полный rollback при ошибке
@Transactional
public RentalResponse createRentalWithTransaction(...) {
    rentalRepository.save(rental);  // Откатится при ошибке
}
```
### 5. **Снэпшоты (исторические данные) в Payment**
При создании платежа сохраняются "снэпшоты" данных на момент оплаты

Это гарантирует неизменность исторических данных

Реализовано через @PrePersist в сущности Payment

## УСТАНОВКА И ЗАПУСК

```java

# 1. Клонируйте репозиторий
git clone https://github.com/AlexGurianov207/car-sharing.git

# 2. Перейдите в директорию проекта
cd car-sharing

# 3. Настройте параметры подключения к БД
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# 4. Соберите проект
./mvnw clean package

# 5. Запустите приложение
java -jar target/Carsharing-0.0.1-SNAPSHOT.jar


Конфигурация:

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/car_sharing
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

## Лабораторная работа 8: Docker, Render, CI/CD

В проект добавлена инфраструктура для запуска и деплоя приложения с PostgreSQL.

### Что сделано

- Подготовлен `Dockerfile` для сборки Spring Boot приложения.
- Подготовлен `docker-compose.yml`: приложение + PostgreSQL 16.
- Настройки вынесены в переменные окружения.
- Добавлен `render.yaml` для деплоя на Render.
- Добавлен healthcheck через `/actuator/health`.
- Настроен GitHub Actions workflow `.github/workflows/ci-cd.yml`:
  - build;
  - tests;
  - deploy to Render;
  - post-deploy healthcheck.
- Настроен SonarCloud workflow `.github/workflows/sonar.yml`.

### Запуск через Docker Compose

```powershell
docker compose up --build
```

Приложение:

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

### Основные переменные окружения

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

Настоящие `.env` файлы не хранятся в репозитории и игнорируются Git.

### Render

Файл `render.yaml` создает:

- web service `car-sharing`;
- PostgreSQL database `car-sharing-db`;
- Docker runtime;
- healthcheck `/actuator/health`.

В Render нужно задать secret-переменные:

```text
ADMIN_PASSWORD
USER_INITIAL_CREDENTIAL
```

### GitHub Actions secrets

Для деплоя и проверки после деплоя нужны repository secrets:

```text
RENDER_DEPLOY_HOOK_URL
RENDER_HEALTHCHECK_URL
SONAR_TOKEN
```

### Важные файлы для 8 лабы

- `Dockerfile`
- `docker-compose.yml`
- `render.yaml`
- `DEPLOYMENT.md`
- `.github/workflows/ci-cd.yml`
- `.github/workflows/sonar.yml`
- `src/main/resources/application.yaml`
