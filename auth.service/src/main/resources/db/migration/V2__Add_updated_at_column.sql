-- V2__Add_updated_at_column.sql
-- Adicionar coluna updated_at à tabela users

ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

