# Loopwise — Progress Log

A running record of work on this project: what was asked, what was decided, what was
built, and what was verified vs. still open. Newest entries at the top. This is the
source of truth for "what actually happened" — more reliable than trying to
reconstruct it from git history or memory once a session ends.

---

## 2026-10-11 (cont'd) — Built Schedule; the 5-tab IA is now fully implemented

**Objective:** Build Schedule (spec §2.5) — the last placeholder tab. User asked to
keep building features and defer device testing to one consolidated pass, so this
was built and verified at the compile/lint/unit-test level only, same as Timer.

**Built:**
- `buildScheduleBlocks()`: pure function turning a week's habits + tasks into
  "blocks" (date, time, label, color). Only habits with a reminder set and tasks
  with a due time produce a block — spec says the view is "auto-populated from
  habits/tasks that have a time window," so anything without one is correctly
  skipped rather than guessed at. Kept as a standalone testable function, same
  pattern as `HabitReportMath`.
- `ScheduleViewModel`: week navigation (prev/next/today), reactive to the DB like
  Today/Reports (not imperative like Timer — a calendar view has nothing to
  "tick").
- Two view modes per spec: **Agenda** (day-grouped list, default — spec explicitly
  calls this out as the simpler option "for smaller screens") and **Grid** (7-day ×
  fixed 6 AM-10 PM hour grid). Deliberately implemented the grid as a plain
  Row-per-hour composition rather than a pixel-positioned/time-proportional canvas
  — much lower risk of a subtle layout bug sitting undetected until the next device
  test, at the cost of not dynamically sizing to the data's actual time range.
- Habit and task blocks are both single points in time (neither carries a duration
  in the data model), so every block fits in exactly one hour cell — no
  multi-row-spanning layout needed.
- Tapping a block is intentionally not wired to anything yet. Spec wants it to
  "jump into edit or start its timer," but doing that properly needs either a
  shared cross-tab ViewModel or a nav argument to carry the target habit/date
  across tabs — flagged as a known gap rather than wiring something half-working
  blind.

**Verified:** Two real compile errors on the first attempt, both fixed:
missing `@OptIn(ExperimentalMaterial3Api::class)` on the new screen (same recurring
pattern as every other `TopAppBar` usage in this codebase — worth remembering),
and an invalid `Modifier.padding(horizontal=, top=, bottom=)` call (that overload
doesn't exist — padding is either `start/top/end/bottom` or `horizontal/vertical`,
not a mix). Second attempt: `gradlew assembleDebug testDebugUnitTest lintDebug`
green, 7 new unit tests for `buildScheduleBlocks` (36 total project-wide), lint
unchanged at 0 errors/28 warnings.

**Status: the 5-tab IA from spec §3 is now fully built** — Today, Schedule, Timer,
Reports, Journal all have real implementations, none are placeholders anymore.

**Still NOT device-verified**: Schedule and Timer, both built this session. Also
still open: Google Sign-In, an actual reminder notification firing.

---

## 2026-10-11 (cont'd) — Published to GitHub; built the Focus Timer

**Objective:** Get the project under version control and publicly backed up, then
keep building — the Focus Timer (spec §2.6), the last major placeholder screen in
the 5-tab IA besides Schedule.

**GitHub setup:** First-ever commit for this project (previously zero version
control for ~a month of work). Installed and authenticated `gh` CLI via device-code
flow. Public repo at `github.com/akash01shaw/habit-tracker`. Before committing:
- Added `.claude/` to `.gitignore` (local tooling state, not project source).
- Added `/Reference/` to `.gitignore` and excluded it from the push — it holds
  third-party app screenshots used only as internal design inspiration; publishing
  them in a public repo risked a real copyright/redistribution concern, flagged to
  and confirmed by the user before pushing.
- Ran an explicit secret scan across every staged file (grepping for the real
  Supabase publishable key, DB password, and JWT substrings) before the first
  commit, and again after the final file set was locked in — both came back clean.
  `local.properties` (the real secrets file) was never staged at all; it's been
  gitignored since Milestone 1.
- Git identity set to GitHub's private no-reply email format
  (`<id>+<login>@users.noreply.github.com`) rather than a real address, since the
  repo is public and the account has no public name/email set.

**Built: Focus Timer** (spec §2.6) — full vertical slice:
- Domain: `TimerSession`, `TimerMode` (stopwatch/countdown), `TimerEvent`
  (start/pause/resume/stop + timestamp). Schema needed no changes — the
  `timer_sessions` table was already in the very first migration from Milestone 1,
  just unused until now.
- `TimerViewModel` is deliberately a single imperative state machine (manual
  coroutine tick loop started/cancelled on start/pause/resume/stop), unlike the
  mostly-reactive-from-the-database ViewModels elsewhere — a running timer is
  inherently stateful/imperative, not naturally a transformation of DB data. The
  Focus habit picker list is the one piece still a live DB subscription.
  Countdown mode auto-stops exactly at target and persists the session on every
  transition (not just at the end), so an in-progress session survives a process
  death.
- Spec §2.6's "logged time feeds that habit's progress automatically": on Stop, if
  the linked habit is DURATION type, adds the session's elapsed minutes to that
  habit's today-progress (reads current value first, then calls the same
  `logProgress` path Today's habit rows use — no new write path needed).
- UI: `TimerRing` (custom Compose Canvas arc — static track + colored progress arc,
  empty progress for stopwatch since there's no fixed target), Stopwatch/Countdown
  tab row, Focus habit dropdown, +/-5min duration stepper (countdown only, idle
  only), Start/Pause/Resume/Stop buttons that swap based on run state, and a
  Start/Pause/Resume/Stop event log below the ring.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green on the
first attempt. Added 6 unit tests for `TimerUiState`'s computed display/progress
logic (stopwatch vs. countdown, clamping, habit-name resolution) — 29 tests total
project-wide now. 4 new lint warnings, all "newer dependency version available"
noise (Kotlin 2.4.20→2.4.21 etc.), nothing code-related; left alone per the
established policy of not chasing every patch bump.

**Still NOT verified:** Timer has never been run on a device — the ring, the actual
tick-by-tick countdown, the auto-stop-at-target behavior, and the
habit-auto-feed-on-stop logic are all exactly the kind of thing that could look
right on paper and still have a bug (off-by-one on the countdown boundary, a race
between the tick loop and the UI reading stale state, etc.). Also still open from
before: Google Sign-In, an actual reminder notification firing, Schedule tab.

---

## 2026-10-11 (cont'd) — Fixed chip wrapping, verified Journal on device

**Objective:** Fix the two cosmetic bugs found in the previous device-testing round,
then exercise the one screen that hadn't been touched yet: Journal.

**Fixed:** `CreateEditHabitScreen`'s Type and Frequency chip rows now wrap in
`Modifier.horizontalScroll(rememberScrollState())`. Root cause: a plain `Row` gives
each child a shrinking width budget as later siblings get measured, so the last chips
("Avoidance", "Per month") were being squeezed below their text's natural width and
wrapping. A scrollable row gives every child effectively unbounded width during
measurement, so no chip wraps — on a narrow screen the row just scrolls instead.
Verified: `gradlew assembleDebug testDebugUnitTest lintDebug` green, reinstalled on
the device, confirmed both chips now render on one line.

**Journal verified on-device, both entry types:**
- Text entry with a habit link: typed content, opened the "Link to a habit" dropdown
  (correctly populated with both real habits from the DB), selected "Drink Water",
  saved — appeared correctly in the feed with the right date/time and habit chip.
- Mood entry: dragged the slider to 85% (landed exactly on target), saved — appeared
  as "Mood: 85% (Great)" with a `General`/`85%` chip row, confirming the
  `moodLabel()` descriptor logic (≥80 → "Great") is correct.
- Feed ordering confirmed newest-first.

**Process notes:**
- `adb shell input tap` coordinates guessed from screenshot thumbnails were
  consistently unreliable this session (one dropdown-item tap missed entirely and
  silently closed the menu instead of selecting). `uiautomator dump` + grepping for
  the target `text=`/`content-desc=` attribute's `bounds` is the reliable method —
  used it for every tap after the first couple of misses, no more missed taps after
  that.
- A shell-quoting apostrophe in typed text (`todays` vs `today's`) broke an `adb
  shell input text` call outright ("no closing quote") — avoid apostrophes in text
  typed this way, or escape more carefully.

**Still NOT exercised:** Google Sign-In, an actual reminder notification firing,
Schedule/Timer tabs (still placeholders).

---

## 2026-10-11 — First real run: installed and tested on a physical device

**Objective:** Actually run the app for the first time, on the user's Samsung Galaxy
S23 Ultra, and exercise the biggest previously-unverified pieces.

**Connecting the device:** USB debugging hit a Windows driver wall — Device Manager
showed the "ADB Interface" driver as healthy, but `adb devices` never listed the
phone (not even as "unauthorized"), a known class of Samsung-on-Windows driver
issue. Switched to **Wireless debugging** (Android 11+) instead — paired and
connected over WiFi with no driver install needed. Device stayed reachable at
`192.168.50.66:46009` for the rest of the session.

**Built and installed:** `gradlew assembleDebug`, then `adb install` + `adb shell am
start`. Interaction was driven entirely via `adb shell input tap/text` plus
`adb shell screencap`, with `uiautomator dump` used to get exact element coordinates
after a couple of early taps missed (manual pixel-math from screenshot thumbnails to
real device coordinates was unreliable — the UI hierarchy dump gives exact bounds
and is the right tool for this, not eyeballing).

**What actually got verified, for the first time ever:**
- **Sign-up end-to-end**: email/password → Supabase session → auto-navigation to
  Today. The real client-side flow, not just the REST-API test from 2026-09-17.
- **POST_NOTIFICATIONS permission prompt** appears correctly on first launch
  (Android 13+ path).
- **Create Habit flow**, counter type: type selector correctly reveals Target/Unit
  fields; the custom `TimePickerDialog` (used for reminders) renders and works
  correctly; saved habit appears on Today immediately with the right visual
  treatment (colored pill, progress text, outlined "+" button).
- **Habit logging**: tapping "+" incremented 0/8 → 1/8 and persisted.
- **Reports screen** against real data: correctly showed 0% completion for a
  partially-logged counter habit (1/8, not yet at target) — confirms the
  "completion % only counts fully-met days" design is working as intended, not a
  bug.
- **Avoidance habit creation**: selecting the "Avoidance" type correctly swapped to
  the muted/dark QuitPalette colors and hid the Target/Unit fields.
- **The live-ticking streak clock** — the single most "could look right on paper and
  still not work" piece flagged in every prior entry — actually counted up in real
  time on screen (watched it advance from 0m,1s to 0m,16s across a 15-second wait).
- **The reset ("I slipped") button** correctly restarted the clock from zero.

**Real (minor) bugs found along the way:**
- Two `FilterChip` labels wrap awkwardly onto two lines: "Avoidance" and "Per
  month" in the Create Habit type/frequency selectors. Cosmetic, not blocking —
  worth a follow-up pass widening those chips or shortening the labels.
- (Not an app bug, noted for anyone repeating this process) `adb shell input text`
  silently truncates at the first space unless spaces are encoded as `%s` — cost one
  round of "Drink" instead of "Drink Water" before catching it via screenshot.

**Process notes for next time:**
- Screen timeout interrupted testing twice (device locked mid-session); user set it
  to 10 minutes, which resolved it.
- Wireless debugging survives phone screen lock/unlock fine — no need to re-pair.

**Still NOT exercised this session:** Journal screen with real data, Google
Sign-In, Schedule/Timer tabs (still placeholders), and anything requiring the app to
sit idle long enough to actually fire a scheduled reminder notification (we confirmed
the permission prompt and the scheduling code runs, but never let an alarm actually
fire and checked the notification appeared).

---

## 2026-09-17 — Real Supabase project connected and verified

**Objective:** Close the last real gap — connect an actual Supabase project instead
of placeholder config, and verify the backend (schema, RLS, auto-profile trigger)
actually works, not just that the app compiles against it.

**Project:** "Habit Tracker" on Supabase, ref `dbtytbduxbjudqubilzt`, region
ap-southeast-2 (Sydney). User had already run `0001_init.sql` before this session.
`local.properties` now holds the real `SUPABASE_URL` and the new-style
`sb_publishable_...` key (used instead of the legacy JWT `anon` key — current
supabase-kt usage is built around the publishable-key format). Google Sign-In still
deferred — `GOOGLE_WEB_CLIENT_ID` stays a placeholder.

**Verified directly against the live backend via REST calls (no Android needed for
this part)** — this is meaningfully stronger evidence than "it compiles":
- Sign-up via `/auth/v1/signup` works.
- Found along the way: the project had **email confirmation** on by default, which
  blocks getting a session until a real inbox clicks a confirmation link — not
  usable for a scripted test. User disabled **Confirm sign up** under
  Authentication → Sign In/Providers → Email. Note: this only affects *new*
  sign-ups going forward, not users already created while it was on — the first
  test account stayed stuck unconfirmed; a second fresh sign-up after the toggle
  got a session immediately, confirming the setting's actual behavior.
- Confirmed the `handle_new_user()` trigger fires: a `profiles` row was created
  automatically on sign-up, no app code involved.
- Confirmed the authenticated write/read path: inserted a `habits` row via
  PostgREST using the session token (same shape `HabitDto` produces), read it back
  successfully.
- Confirmed RLS actually blocks what it should: the same `habits` read with no
  auth token (anon-only) returned `[]` instead of the data.
- Deleted the test habit row; two test auth users remain in the project's
  Authentication → Users list (`loopwise.verify.9f3a@...`, stuck unconfirmed, and
  `loopwise.verify.b7c2@...`, confirmed) — harmless fake addresses, but the user may
  want to delete them from the dashboard for a clean user list.
- Also reran `gradlew assembleDebug` after wiring in the real keys — still green.

**Worth flagging back to the user:** "Confirm email" is now **off** project-wide as
a side effect of this test — fine for continued testing, but worth turning back on
before any real/public use, since right now anyone can sign up with an email they
don't own.

**Still NOT verified:** the Android app itself has still never been run — this
session confirmed the *backend* is configured correctly and reachable, which is a
big derisking step, but the client-side flow (Compose UI, Credential Manager,
Room↔Supabase sync as the app actually performs it) remains unverified until it
runs on a device or emulator.

---

## 2026-09-15 — Local notifications; Milestone 4 complete

**Objective:** Close out Milestone 4's last item — local reminder notifications for
habits and tasks (spec §7 MVP scope) — after resuming from a session gap.

**Session-start check:** confirmed the JDK/SDK toolchain installed on 2026-09-13 was
still intact (user chose to keep it rather than uninstall) and a sanity build still
passed before starting new work.

**Real gap found and fixed along the way:** there was no task-creation screen at all
— `TaskRepository.upsertTask` was never called from any UI, only `HabitRepository`'s
create flow existed. Fixed as part of this work rather than filed for later, since it
was needed to make task reminders testable and is a real MVP-completeness gap (Today
already displayed and toggled tasks, just couldn't create them).

**Built:**
- `NotificationHelper`: notification channel + builder (checks POST_NOTIFICATIONS
  before posting, silently no-ops rather than crashing if not granted).
- `ReminderScheduler` (AlarmManager, not WorkManager): WorkManager's periodic jobs
  have a 15-minute minimum granularity and can't guarantee firing at an exact time of
  day, so AlarmManager's `setExactAndAllowWhileIdle` is the correct tool here — at the
  cost of needing manual bookkeeping AlarmManager doesn't do for you.
- `ReminderBroadcastReceiver`: shows the notification when an alarm fires; for habit
  reminders, immediately reschedules itself for the same time tomorrow (AlarmManager
  alarms are one-shot, so a "daily" reminder means the receiver re-arms its own next
  occurrence every time it fires).
- `BootReceiver` + `RescheduleRemindersWorker`: AlarmManager alarms are cleared on
  device reboot, so boot re-arms every habit/task reminder from what's in Room. Runs
  via WorkManager rather than directly in the receiver, since re-reading the database
  and rescheduling many alarms can exceed a BroadcastReceiver's execution window.
- Runtime POST_NOTIFICATIONS permission request on first launch (Android 13+ only).
- Real reminder-time picker UI replacing the "coming later" placeholder in Create
  Habit (`ReminderPicker`: chips + a `TimePicker` dialog), wired to actually call
  `ReminderScheduler` on save.
- New `CreateTaskScreen`/`CreateTaskViewModel` (title, due date/time via date/time
  picker dialogs, priority flag, note, optional reminder), wired into a FAB menu on
  Today ("New Task" / "New Habit" — previously the FAB only offered habit creation).
- Extracted a shared `ui/common/TimePickerDialog` once it was needed in two places
  (habit reminders and task due-time), rather than duplicating the dialog code.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` passed on the first
attempt for the whole batch (notifications + task creation together — the largest
single addition so far). One new lint warning appeared (a redundant SDK-version check
now that minSdk is 26) and was cleaned up; back to the baseline 0 errors / 24
warnings, 23 unit tests still passing. No new unit tests added — AlarmManager/
notification code is framework-heavy and not meaningfully testable without
Robolectric or instrumentation, unlike the branchy pure-logic code tested earlier.

**Still NOT verified:** the biggest one yet — none of this has fired on a real
device. Exact-alarm scheduling, the boot-reschedule path, and the permission prompt
are exactly the kind of thing that looks right on paper but can only really be
trusted once seen working. This is the strongest argument so far for actually running
the app, whenever that happens.

---

## 2026-09-13 — Journal screen

**Objective:** Build spec §2.4's Journal, scoped to the MVP subset per the spec's own
Phase 1 list: text + mood entries only, private by default (checklist/bullet-log
entry types and sharing are explicitly Phase 2/3 — `JournalEntryType.CHECKLIST` is
modeled in the enum so the schema won't need a later migration, but isn't creatable
from the UI yet).

**Built:** full vertical slice, matching the pattern from Habits/Tasks —
- Domain: `JournalEntry`, `JournalEntryType`, `JournalVisibility`.
- Data: `JournalEntryEntity` + Room converters, `JournalEntryDao` (filter by habit
  and/or type, newest-first), `JournalEntryDto` (maps to the `journal_entries` table
  already defined in the original SQL migration — no schema changes needed),
  `JournalRepository` (same offline-first push-on-write pattern as Habits/Tasks).
- `JournalViewModel`: reactive feed (filtered by habit/type) plus a separate
  new-entry form (type toggle, content or mood slider, optional habit link).
- UI: `JournalEntryCard` (content + date/habit/mood chips, matching the reference
  screenshots' card style), `JournalScreen` (dropdown filters + feed + FAB),
  `NewJournalEntryScreen` (composer).

**Design note:** `NewJournalEntryScreen` first pulled the habit list by instantiating
`TodayViewModel` just to read its habits — wrong tool for the job (pulls in unrelated
Today-screen state, creates a redundant ViewModel instance). Caught and fixed before
verifying: `JournalUiState` already carries the habits list `JournalViewModel` needs
anyway, so `NewJournalEntryScreen` now reads it from there instead.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green on the
first attempt. No new unit tests added this round (Journal is mostly CRUD/filtering
wiring, not the kind of branchy calculation logic that habit-scheduling and report
math were — proportionate effort, not a gap). Lint still 0 errors.

**Still NOT verified:** same standing caveat — the filters, feed, and composer have
never been run/tapped through, only compiled.

---

## 2026-09-13 — Reports screen

**Objective:** Build spec §2.7's Reports screen: Week/Month/Year range, per-habit
completion % + streak, GitHub-style heatmap, sortable by completion or streak.

**Built:**
- `HabitReportMath.kt`: pure, non-Composable calculation logic — `buildHabitReport()`
  computes completion % (only counting days a habit was actually scheduled, so a
  weekday-only habit isn't penalized for its off days), current streak (consecutive
  scheduled days ending today with no gap), and a heatmap cell list. Kept deliberately
  separate from the ViewModel so it's directly unit-testable.
- `HabitLogDao`/`HabitRepository.observeLogsInRange()`: wrapped a DAO query that
  already existed from Milestone 1-3 but was unused until now.
- `ReportsViewModel`: reactive pipeline (user → range/sort → habits → per-habit logs
  in range) using `combine()` over a dynamic list of per-habit flows.
- UI: `HeatmapStrip` (small colored squares, opacity = completion intensity, neutral
  gray for non-scheduled days), `HabitReportRow` (name, target description, streak
  flame icon, completion %), `ReportsScreen` with range/sort filter chips.
- Scoping decision: avoidance/quit habits are excluded from Reports for now — they
  use a continuous streak model, not daily completion, so the heatmap/percent math
  doesn't apply to them as-is. Worth a dedicated reports treatment later.

**Verified:** Two rounds of real fixes needed — both missing imports for extension
functions defined in another package (`Frequency.describe()` and `Habit.isScheduledOn()`
live in `ui.today`, not auto-visible from `ui.reports`; Kotlin doesn't resolve
extension functions across packages without an explicit import, unlike regular
members). After that, `gradlew assembleDebug testDebugUnitTest lintDebug` all green:
6 new unit tests for the report math (23 total project-wide), lint still 0 errors.

**Still NOT verified:** the heatmap/streak/sort UI has never been seen rendered —
only compiled and logic-tested, same caveat as everything else so far.

---

## 2026-09-13 — Milestone 4 started: avoidance/quit habits

**Objective:** Add the fourth habit type (avoidance/"quit" habits, spec §2.2-2.3) —
the last piece of the core habit engine, previously selectable in Create Habit but
unrenderable on Today.

**Built:**
- `HabitLogDao.observeLatestLogPerHabit(userId)`: a Room query joining each habit to
  its most-recent log row, used to derive "time since last reset."
- Reused the existing `logProgress()` upsert-by-date write path for "I slipped"
  resets — no new write path needed, since upserting today's log row naturally
  advances its `loggedAt` to now. Known limitation: because the log table is one row
  per habit per day, a second slip on the same day overwrites the first slip's
  timestamp rather than recording both — acceptable for the MVP streak clock, but
  means per-slip history (spec's "pattern insights" idea) isn't fully captured yet.
  Reset always targets `LocalDate.now()`, independent of whatever date the user has
  scrolled to on the date strip.
- `TodayViewModel`: new `avoidanceHabits: List<AvoidanceHabitState>` in `TodayUiState`
  (habit + "since" timestamp), `resetAvoidanceStreak()` action.
- `AvoidanceHabitRow` composable: dark QuitPalette pill, live-updating "Xd, Yh, Zm, Ws"
  clock (ticks every second via `produceState`), circular reset button — matches the
  reference-screenshot "Quit Habits" visual pattern.
- New "Habits I'm Quitting" collapsible section on Today, below Daily Habits.
- Streak duration formatting extracted to a plain (non-Composable) function
  specifically so it's unit-testable without Compose test infra.
- Create Habit screen needed no changes — avoidance was already selectable and
  correctly skipped target-value validation.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — all green on the
first attempt (no new version/tooling issues this time). Added 5 new unit tests for
streak-duration formatting (17 total across the project now, all passing). Lint: 0
errors, only the same pre-existing benign Gradle-version deprecation notice.

**Still NOT verified:** same caveat as always — nothing has run on an emulator or
device yet, so the live-ticking clock, section collapsing, and reset button have
never actually been seen working, only compiled and logic-tested.

---

## 2026-09-13 — Compile / lint / unit-test verification

**Objective:** Verify the Milestone 1-3 code actually builds, since it was written
without a local Android toolchain available.

**Decisions:** User chose to (a) have a headless JDK + Android SDK installed on this
machine so compile/lint/test checks can run without a person driving them, and (b)
skip emulator/visual testing for now.

**Actions:**
- Installed JDK 17 (Temurin), Android SDK command-line tools, and Gradle on this
  machine; generated the project's Gradle wrapper (`gradlew`/`gradlew.bat`).
- Ran `gradlew :app:compileDebugKotlin` — failed. Diagnosed and fixed, in order:
  1. `androidx.browser` (pulled in by Credential Manager) required `compileSdk 36` —
     project was pinned to 35. Bumped `compileSdk`/`targetSdk` to 36.
  2. supabase-kt 3.8.0's binary metadata required Kotlin ≥2.4.0 — project was pinned
     to Kotlin 2.0.21. Bumped Kotlin to 2.4.20.
  3. KSP 2.3.x (matching that Kotlin) required AGP ≥8.12.0 — project was on AGP
     8.7.3/8.9.2. Bumped AGP to 8.13.0, Gradle wrapper to 8.13.
  4. Room's KSP annotation processor (2.6.1) was too old for the new toolchain.
     Bumped Room to 2.8.4.
  5. Real source bugs: wrong import path for Supabase's `SessionStatus` (it's under
     `...auth.status.SessionStatus`, not `...auth.SessionStatus`), a few Compose
     `TopAppBar` calls missing `@OptIn(ExperimentalMaterial3Api::class)`, a missing
     `getValue` import for a `by` delegate in `BottomNavBar.kt`.
- Re-ran `gradlew assembleDebug` — **succeeded**, produced a real debug APK.
- Ran `gradlew lintDebug` — **0 errors**, 24 warnings (all "newer version available"
  dependency nags + minor launcher-icon polish — left alone deliberately, since
  chasing every latest version is what caused the round of breakage above).
- Cleaned up 4 minor deprecation warnings surfaced by the compiler (deprecated
  `fallbackToDestructiveMigration()` overload, two non-AutoMirrored icon refs, one
  missing coroutines `@OptIn`).
- Added a JUnit unit test suite (`app/src/test/...`): habit-scheduling rules
  (`HabitSchedulingTest`, 5 tests), `HabitType` wire-format round-trips
  (`HabitTypeTest`, 3 tests), habit color hex conversion (`HabitDtoTest`, 4 tests).
  Ran `gradlew testDebugUnitTest` — **12/12 pass**.

**Result:** Build is green end-to-end (compile, lint, unit tests). Verified package
versions now live in `gradle/libs.versions.toml` — trust that file over version
numbers mentioned in the build plan, which reflect the original pre-verification
picks.

**Still NOT verified:** nothing has run on an emulator or device. `local.properties`
holds placeholder Supabase keys, so auth, cloud sync, and all UI/navigation behavior
are unexercised — only structural/logical correctness is confirmed.

---

## 2026-09-13 — Project kickoff: spec review, plan, initial build (Milestones 1-3)

**Objective:** Read `habit-tracker-app-spec.md` and start building the Android app it
describes.

**Decisions (via user):**
- Backend: **Supabase** (not Firebase) — SQL flexibility, no vendor lock-in.
- Auth: **both** email/password and Google Sign-In for MVP.
- Reviewed 8 user-supplied reference screenshots (onboarding shots from a paid app,
  "HelloHabit") for UI/interaction inspiration — folded into the build plan as a
  visual-reference section (colored habit-pill rows, shape-shifting completion
  control, heatmap reports, etc.) without copying branding/copy.

**Plan:** Full plan written to
`C:\Users\akash\.claude\plans\immutable-foraging-boot.md` — scopes Milestones 1-3
(foundation, auth, Today view + habit engine) now, with avoidance habits / Reports /
Journal / notifications explicitly deferred to Milestone 4.

**Built:**
- Gradle/Kotlin/Compose project scaffold; Supabase SQL migration
  (`supabase/migrations/0001_init.sql`) with Row Level Security locked to each user's
  own rows from day one.
- Koin DI, Room offline-first cache, Supabase client wiring (push-on-write sync with
  an `isSynced` flag per row, retried on next foreground).
- Auth: email/password + Google Sign-In (Credential Manager), session-gated
  navigation start destination.
- Today screen: 7-day date strip, collapsible Tasks/Habits sections, habit row
  supporting boolean/counter/duration types with the reference-inspired visual
  pattern (colored pill, progress text, action control that fills in on completion).
- Create Habit screen: name, type, color palette, frequency, target/unit.
- Bottom nav with all 5 tabs; Schedule/Timer/Reports/Journal are "coming soon"
  placeholders.
- `README.md` with first-time setup instructions (Supabase project, SQL migration,
  Google OAuth client, `local.properties`).

**Not built yet:** avoidance/quit-habit streak tracking, Reports, Journal, local
notifications (all Milestone 4), then Phase 2/3 spec items (sharing, Health Connect,
widgets).

---

## Open items / next steps (last reviewed 2026-10-11)
- **Published**: public repo at `github.com/akash01shaw/habit-tracker`, first commit
  in. `Reference/` (third-party screenshots) and `.claude/` deliberately kept out of
  git — see the "Published to GitHub" log entry for why. Plan: keep committing and
  pushing as features land, not waiting for some notion of "fully done."
- **Device-verified so far**: sign-up, habit creation/logging (all types), Reports,
  the avoidance-habit live streak clock, and Journal (text + mood, habit-linking).
  Two chip-wrapping bugs found and fixed.
- **Built but NOT yet device-verified**: Focus Timer (ring, tick loop, countdown
  auto-stop, habit-auto-feed-on-stop) and Schedule (agenda list, week grid, week
  navigation) — both compile/lint/test clean but have never actually run. User
  wants to batch these into one consolidated device-testing pass rather than
  testing each feature individually.
- **Known gap, not a bug**: tapping a Schedule block does nothing yet — spec wants
  it to jump into habit edit or start the linked timer, deferred until there's a
  clean way to carry a habit/date argument across bottom-tab destinations.
- **Still genuinely unverified**: Google Sign-In, a reminder notification actually
  firing.
- **Google Sign-In isn't wired up** — `GOOGLE_WEB_CLIENT_ID` in `local.properties` is
  still a placeholder. Email/password is the only working sign-in method right now.
- **Security follow-up from the 2026-09-17 Supabase test**: "Confirm email" is off
  project-wide on the live Supabase project — should be turned back on before any
  real/public use. A few throwaway test accounts are also sitting in
  Authentication → Users if the user wants a clean list.
- A local dev toolchain (JDK 17, Android SDK cmdline-tools, Gradle, platform-tools,
  `gh` CLI) is installed on this machine so builds/lint/tests/`adb`/git push can all
  run without Android Studio. Still no Android Studio and no emulator — device
  testing goes through the physical phone over wireless `adb`.
- **Feature-wise, the full 5-tab IA (spec §3) is now built**: Today, Schedule, Timer,
  Reports, and Journal all have real implementations — nothing left as a
  placeholder. Not started: shared habits/accountability, CSV export (Phase 2), and
  all of Phase 3 (Health Connect sync, home-screen widgets, yearly heatmap,
  group-streak logic, partner-nudge notifications, checklist journal entries).
