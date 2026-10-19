-- Adds "skip today" for a habit (Today screen's long-press quick actions): a day
-- a user explicitly skips doesn't break a streak or count against completion%,
-- unlike an ordinary unlogged miss.
alter table public.habit_logs add column if not exists is_skipped boolean not null default false;
