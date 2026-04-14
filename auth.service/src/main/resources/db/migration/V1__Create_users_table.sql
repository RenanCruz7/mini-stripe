-- V1__Create_users_table.sql
-- Criação da tabela users com todas as colunas necessárias

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT email_not_empty CHECK (email != ''),
    CONSTRAINT name_not_empty CHECK (name != ''),
    CONSTRAINT role_valid CHECK (role IN ('USER', 'ADMIN', 'MERCHANT'))
);

-- Índices para otimização de queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

