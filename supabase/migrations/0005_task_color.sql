-- Adds a per-task color (Today's task rows and Schedule's task blocks previously
-- shared one flat theme color for every task, unlike habits which already have
-- their own color).
alter table public.tasks add column if not exists color text not null default '#3B82F6';
