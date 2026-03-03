<div align="center">

# 🚗 CAR SHARING API

### REST API сервис для управления автопарком каршеринга

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=500&size=24&duration=3000&pause=500&color=2F81F7&center=true&vCenter=true&width=435&lines=Java+17;Spring+Boot+4.0;REST+API;Checkstyle+%26+SonarCloud" alt="Typing SVG" />

[![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-%236DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-Quality%20Gate-%23F3702A?style=for-the-badge&logo=sonarcloud&logoColor=white)](https://sonarcloud.io/summary/overall?id=AlexGurianov207_car-sharing&branch=main)

</div>

---

## 📋 СОДЕРЖАНИЕ

- [📖 О проекте](#-о-проекте)
- [✨ Функциональность](#-функциональность)
- [🛠️ Технологический стек](#️-технологический-стек)
- [🔗 SonarCloud анализ](#-sonarcloud-анализ)
- [🚀 Запуск проекта](#-запуск-проекта)
- [📬 Примеры запросов](#-примеры-запросов)

---

## 📖 О ПРОЕКТЕ

Данный проект представляет собой REST API сервис для управления автопарком каршеринга. Разработан в рамках лабораторной работы по дисциплине *"Программирование на языках высокого уровня"*.

**Цель работы:** Создание Spring Boot приложения с классической многослойной архитектурой (Controller → Service → Repository), реализация REST endpoints для управления автомобилями, настройка статического анализатора кода Checkstyle и интеграция с SonarCloud для непрерывной оценки качества кода.

API позволяет добавлять новые автомобили в автопарк, получать полный список доступных машин, искать по марке автомобиля (`brand`) и просматривать детали конкретного автомобиля. Проект сделан с акцентом на чистоту кода, следование стандартам Java Code Conventions и документирование API.

---

## ✨ ФУНКЦИОНАЛЬНОСТЬ

### 🚘 Управление автомобилями (`/api/cars`)

| Метод   | Endpoint                      | Описание                                              |
|---------|-------------------------------|-------------------------------------------------------|
| `GET`   | `/api/cars`                   | Получение списка всех автомобилей                     |
| `GET`   | `/api/cars?brand={brand}`     | 🔍 **Поиск автомобилей по марке** (регистронезависимо)|
| `GET`   | `/api/cars/{id}`              | Просмотр детальной информации о конкретном автомобиле |
| `POST`  | `/api/cars`                   | Добавление нового автомобиля в автопарк               |

**Ключевая функция:** Поиск автомобилей по марке (`brand`) с фильтрацией по подстроке. Если автомобили по заданному критерию не найдены, API возвращает `404 Not Found`.

---

## 🛠️ ТЕХНОЛОГИЧЕСКИЙ СТЕК

<div align="center">

| Категория           | Технологии                                                                                                                                                                                                     |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Язык**            | ![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)                                                                                                            |
| **Фреймворк**       | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat-square&logo=spring&logoColor=white)                                                                                            |
| **Сборка**          | ![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apache-maven&logoColor=white)                                                                                                    |
| **Архитектура**     | REST API, Controller-Service-Repository                                                                                                                                                                        |
| **Хранение данных** | In-memory коллекции (ConcurrentHashMap)                                                                                                                                                                        |
| **Утилиты**         | ![Lombok](https://img.shields.io/badge/Lombok-2022-ff69b4?style=flat-square)                                                                                                                                   |
| **Качество кода**   | ![Checkstyle](https://img.shields.io/badge/Checkstyle-10.12-00BFFF?style=flat-square) ![SonarCloud](https://img.shields.io/badge/SonarCloud-Analysis-F3702A?style=flat-square&logo=sonarcloud&logoColor=white) |

</div>

- **Архитектура:** Многослойная (Controller, Service, Repository)
- **Модель данных:** Автомобиль (`Car`) с полями: id, brand, model, licensePlate (госномер), year, pricePerHour, status
- **Обработка данных:** Хранение в памяти с использованием потокобезопасных коллекций (`ConcurrentHashMap`)
- **Качество кода:**
   - **Checkstyle:** Настроен на проверку стиля кода по правилам (длина строки 120, отступы 4 пробела, именование и т.д.) — файл `checkstyle.xml`
   - **SonarCloud:** Непрерывный анализ кода на наличие багов, уязвимостей и технического долга

---

## 🔗 SONARCLOUD АНАЛИЗ

<div align="center">
  <a href="https://sonarcloud.io/summary/overall?id=AlexGurianov207_car-sharing&branch=main">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=AlexGurianov207_car-sharing&metric=alert_status" alt="Quality Gate Status">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=AlexGurianov207_car-sharing&metric=bugs" alt="Bugs">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=AlexGurianov207_car-sharing&metric=code_smells" alt="Code Smells">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=AlexGurianov207_car-sharing&metric=coverage" alt="Coverage">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=AlexGurianov207_car-sharing&metric=duplicated_lines_density" alt="Duplicated Lines">
  </a>

👉 [Перейти к полному анализу на SonarCloud](https://sonarcloud.io/summary/overall?id=AlexGurianov207_car-sharing&branch=main)
</div>

---

## 🚀 ЗАПУСК ПРОЕКТА

### Предварительные требования

- JDK 17 или выше
- Maven 3.6+ (или используйте встроенный Maven Wrapper)

### Установка и запуск

```bash
# 1. Клонируйте репозиторий
git clone https://github.com/AlexGurianov207/car-sharing.git

# 2. Перейдите в директорию проекта
cd car-sharing

# 3. Соберите проект с помощью Maven Wrapper
./mvnw clean package

# 4. Запустите приложение
java -jar target/Carsharing-0.0.1-SNAPSHOT.jar