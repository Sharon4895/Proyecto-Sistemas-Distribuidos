CREATE DATABASE financiero_db;
USE financiero_db;

-- Tabla de Usuarios
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    curp VARCHAR(18) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- En producción esto debe ir encriptado
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER'
);

-- Tabla de Cuentas (Balance)
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabla de Transacciones
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAW, etc.
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Datos Semilla (Para que puedas probar ya)
INSERT INTO users (curp, password, name, role) VALUES ('NARS031210HMCJMHA4', '123456', 'Sharon Leonardo', 'USER');
INSERT INTO accounts (user_id, balance) VALUES (1, 15450.00);