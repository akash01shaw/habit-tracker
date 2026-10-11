-- Loopwise initial schema. Run this in the Supabase SQL editor, or via
-- `supabase db push` if you're using the Supabase CLI.

create extension if not exists "uuid-ossp";

-- One row per authenticated user, mirrors auth.users.
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  email text not null,
  display_name text,
  avatar_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.habits (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  color text not null,
  icon text,
  type text not null check (type in ('boolean', 'counter', 'duration', 'avoidance')),
  frequency jsonb not null default '{"kind":"daily"}'::jsonb,
  target_value double precision,
  unit text,
  reminders jsonb not null default '[]'::jsonb,
  health_sync_source text,
  is_shared boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.habit_shares (
  id uuid primary key default uuid_generate_v4(),
  habit_id uuid not null references public.habits (id) on delete cascade,
  member_user_id uuid not null references auth.users (id) on delete cascade,
  status text not null check (status in ('invited', 'active', 'left')) default 'invited',
  joined_at timestamptz
);

create table if not exists public.habit_logs (
  id uuid primary key default uuid_generate_v4(),
  habit_id uuid not null references public.habits (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  date date not null,
  value double precision not null default 0,
  is_done boolean not null default false,
  note text,
  logged_at timestamptz not null default now(),
  unique (habit_id, user_id, date)
);

create table if not exists public.tasks (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid not null references auth.users (id) on delete cascade,
  title text not null,
  due_date date,
  due_time time,
  priority_flag boolean not null default false,
  note text,
  reminder_time timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.journal_entries (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid not null references auth.users (id) on delete cascade,
  linked_habit_id uuid references public.habits (id) on delete set null,
  type text not null check (type in ('text', 'mood', 'checklist')),
  content text,
  mood_value int check (mood_value between 0 and 100),
  visibility text not null check (visibility in ('private', 'shared')) default 'private',
  created_at timestamptz not null default now()
);

create table if not exists public.timer_sessions (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid not null references auth.users (id) on delete cascade,
  habit_id uuid references public.habits (id) on delete set null,
  mode text not null check (mode in ('stopwatch', 'countdown')),
  target_seconds int,
  events jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now()
);

-- Row Level Security: owner-only access for MVP. Shared-habit policies are
-- added in Phase 2 once habit_shares is actually used for cross-account reads.
alter table public.profiles enable row level security;
alter table public.habits enable row level security;
alter table public.habit_shares enable row level security;
alter table public.habit_logs enable row level security;
alter table public.tasks enable row level security;
alter table public.journal_entries enable row level security;
alter table public.timer_sessions enable row level security;

create policy "profiles: self read/write" on public.profiles
  for all using (auth.uid() = id) with check (auth.uid() = id);

create policy "habits: owner read/write" on public.habits
  for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

create policy "habit_shares: member read" on public.habit_shares
  for select using (
    auth.uid() = member_user_id
    or auth.uid() = (select owner_id from public.habits where id = habit_id)
  );

create policy "habit_logs: owner read/write" on public.habit_logs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "tasks: owner read/write" on public.tasks
  for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

create policy "journal_entries: owner read/write" on public.journal_entries
  for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

create policy "timer_sessions: owner read/write" on public.timer_sessions
  for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

-- Auto-create a profile row whenever a new auth user signs up.
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, display_name, avatar_url)
  values (
    new.id,
    new.email,
    new.raw_user_meta_data ->> 'display_name',
    new.raw_user_meta_data ->> 'avatar_url'
  );
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();
