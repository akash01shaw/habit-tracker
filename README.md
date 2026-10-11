# Loopwise

Habit & task tracker — Android native, Kotlin + Jetpack Compose, Supabase backend.
See `habit-tracker-app-spec.md` for the full product spec and
`C:\Users\akash\.claude\plans\immutable-foraging-boot.md` for the build plan this
codebase follows.

## What's built so far (Milestones 1-3)

- Project scaffold: Gradle setup, Compose theme with the build/quit habit color
  palettes, Koin DI, Room offline cache, Supabase client wiring.
- Auth: email/password + Google Sign-In (Credential Manager), session-gated navigation.
- Today view: date strip, Tasks section, Habits section for boolean/counter/duration
  types, Create Habit screen (name, type, color, frequency, target).

**Not built yet** (see the plan file's Milestone 4 for what's next): avoidance/quit
habit streak tracking, Reports, Journal, local notifications, Schedule/Timer screens
(currently placeholders), Phase 2/3 items (sharing, Health Connect, widgets).

## First-time setup

### 1. Create a Supabase project
Go to [supabase.com](https://supabase.com), create a project, then open
**SQL Editor** and run the contents of `supabase/migrations/0001_init.sql`. This
creates all tables (`habits`, `tasks`, `habit_logs`, etc.) with Row Level Security
already locked down to each user's own rows.

### 2. Get your Supabase API keys
In **Project Settings -> API**, copy the **Project URL** and the **anon public** key.

### 3. Set up Google Sign-In
In [Google Cloud Console](https://console.cloud.google.com), create an OAuth 2.0
**Web application** client ID (not Android). Add that same client ID as the Google
provider's Client ID in Supabase's **Authentication -> Providers -> Google** settings,
and enable the provider there.

### 4. Configure local secrets
Copy `local.properties.example` to `local.properties` (same folder) and fill in:
- `sdk.dir` — your Android SDK path
- `SUPABASE_URL`, `SUPABASE_ANON_KEY` — from step 2
- `GOOGLE_WEB_CLIENT_ID` — from step 3

`local.properties` is gitignored — never commit real keys.

### 5. Open in Android Studio
Open the project root in Android Studio. It should detect the Gradle project and
offer to generate the Gradle wrapper if `gradlew`/wrapper jar aren't present. Let it
sync, then run on an emulator or device (min SDK 26 / Android 8.0+).

**Note on dependency versions:** the Kotlin/Supabase-kt/Compose ecosystem moves fast,
and the versions pinned in `gradle/libs.versions.toml` were verified against current
docs at time of writing but weren't compiled in this environment (no Android SDK/JDK
available here). If Android Studio flags a version conflict or suggests an upgrade on
first sync, accept it — the versions here are a solid starting point, not gospel.
The libraries most likely to need a quick fix-up if their API drifted slightly: the
Supabase Auth provider imports in `ui/auth/AuthRepository.kt` (Google/IDToken/Email
provider classes) — Android Studio's "Import class" quick-fix will resolve these in
seconds if the exact sub-package differs from what's written.

## Verifying it works
Once configured, run through: sign up with email -> land on Today (empty state) ->
tap + -> create a boolean habit ("Wake up early") -> back on Today, tap it to
complete -> create a counter habit ("Drink water", target 8, unit "cups") -> tap +
a few times -> force-close and reopen the app to confirm state persisted (Room) ->
check the Supabase dashboard's Table Editor to confirm the same rows appear in
`habits` and `habit_logs` (cloud sync).
