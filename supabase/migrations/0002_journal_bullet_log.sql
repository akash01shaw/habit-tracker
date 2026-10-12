-- Adds the bullet-log journal entry type (spec §2.4). journal_entries.type was
-- created with an inline, unnamed check constraint, which Postgres names
-- "<table>_<column>_check" by default.
alter table public.journal_entries drop constraint if exists journal_entries_type_check;
alter table public.journal_entries
  add constraint journal_entries_type_check check (type in ('text', 'mood', 'checklist', 'bullet_log'));
