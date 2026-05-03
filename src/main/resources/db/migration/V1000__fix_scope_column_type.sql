-- Fix scope column type in projects table
-- Change from text/varchar to jsonb

ALTER TABLE projects 
ALTER COLUMN scope TYPE jsonb USING scope::jsonb;
