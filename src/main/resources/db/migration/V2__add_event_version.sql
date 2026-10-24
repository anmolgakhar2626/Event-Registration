-- Add version column to events table for optimistic/pessimistic locking
ALTER TABLE events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
