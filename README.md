<div align="center">

# 🚗 CAR SHARING API

### REST API сервис для управления каршерингом

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=500&size=24&duration=3000&pause=500&color=2F81F7&center=true&vCenter=true&width=535&lines=Java+17;Spring+Boot+4.0;PostgreSQL+%7C+JPA+Hibernate;REST+API;Checkstyle+%26+SonarCloud;N%2B1+Problem+%7C+Transactions" alt="Typing SVG" />

[![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-%236DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-%23316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
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

### ER-диаграмма
