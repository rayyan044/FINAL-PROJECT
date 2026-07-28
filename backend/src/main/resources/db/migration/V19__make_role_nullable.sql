-- Database Migration: V19__make_role_nullable.sql
-- Drop the NOT NULL constraint on legacy role column since role_id is now the primary role mapping

ALTER TABLE users ALTER COLUMN role DROP NOT NULL;
