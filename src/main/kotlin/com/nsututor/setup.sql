-- Удаление базы данных, если она уже существует
DROP DATABASE IF EXISTS nsututor;

-- Создание новой базы данных
CREATE DATABASE nsututor;

-- Подключение к созданной базе данных
\c nsututor;

-- Создание таблицы users
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);
-- вроде не нужен???