-- Adds journal entry categories (General / Ideas / Personal / Psychological)
-- so entries can be filtered/browsed by topic, not just by habit/type/date.
alter table public.journal_entries add column if not exists category text not null default 'general';
