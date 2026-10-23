-- V3: Add cascade deletion on created_by constraints to support clean user teardown
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_created_by_fkey;
ALTER TABLE projects ADD CONSTRAINT projects_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE research_runs DROP CONSTRAINT IF EXISTS research_runs_created_by_fkey;
ALTER TABLE research_runs ADD CONSTRAINT research_runs_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE scheduled_research DROP CONSTRAINT IF EXISTS scheduled_research_created_by_fkey;
ALTER TABLE scheduled_research ADD CONSTRAINT scheduled_research_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;
