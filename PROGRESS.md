# Loopwise — Progress Log

A running record of work on this project: what was asked, what was decided, what was
built, and what was verified vs. still open. Newest entries at the top. This is the
source of truth for "what actually happened" — more reliable than trying to
reconstruct it from git history or memory once a session ends.

---

## 2026-10-20 (cont'd) — Verified long-press menu + Skip/Unskip on-device

Picked back up after a multi-day gap; wireless debugging needed a full re-pair
(toggling it off/on gave a fresh port, the old pairing had gone stale — not a
code issue). Directly exercised what the previous entry only verified via code
review: long-pressed the "Push-ups" name pill → the Edit/Skip today/Delete menu
appeared correctly. Tapped "Skip today" → row updated to show "Skipped" with the
muted dash-circle action control, exactly as designed. Tapped the action control
again → correctly un-skipped back to "1/1 time", not-done. Long-press quick
actions and Skip are now confirmed working end-to-end, not just unit-tested.

---

## 2026-10-20 (cont'd) — Built long-press quick actions + Skip; found and fixed a real data-loss bug

**Objective:** Second half of the audit follow-through: long-press quick actions on
Today rows (spec §2.1 — "Long-press a row to reorder or reveal quick actions (edit,
skip, delete)"). Reorder explicitly scoped out — it needs a persisted sort-order
field and drag gesture handling, a meaningfully separate feature; flagged as a
known remaining gap rather than half-built.

**Built: Skip.** A real domain concept, not just a UI affordance — skipping a day
needed to mean something to the streak/completion math, not just hide the row.
Added `HabitLog.isSkipped` (new Room column, DB version bumped to 3, new Supabase
migration `0003_habit_log_skipped.sql`, same pattern as the bullet-log migration).
A skipped day is **transparent**: `computeCurrentStreak()` and
`HabitReportMath.buildHabitReport()` both now skip over it without breaking a
streak or counting against completion% — same treatment as a day the habit wasn't
even scheduled on. `HabitRow`'s action control gets a third visual state (muted
circle, dash icon) alongside done/not-done.

**Built: long-press menus.** `HabitRow`, `AvoidanceHabitRow`, and `TaskRow` all
gained `combinedClickable` (tap = existing edit navigation, long-press = a
`DropdownMenu`). Habits get Edit/Skip-or-Unskip/Delete; avoidance habits get
Edit/Delete only (no skip — the continuous streak clock is already the avoidance
equivalent); tasks get Edit/Delete. Delete goes through a shared confirm
`AlertDialog` in `TodayScreen` (a `PendingDelete` sealed type covers both habit
and task deletes) rather than acting immediately — same irreversible-action
caution as every other delete in this app. `TodayViewModel` gained
`skipHabit`/`unskipHabit`/`deleteHabit`/`deleteTask`, the latter two cancelling
any scheduled reminders before deleting.

**Found and fixed a real, pre-existing data-loss bug while verifying this**:
`HabitRepository.refreshFromRemote()` only ever pulled the `habits` table down
from Supabase — `habit_logs` were only ever pushed up, never pulled down. Any
wiped or reinstalled local Room cache silently lost **all logged history**, even
though it was sitting safely in Supabase the whole time. This wasn't a new bug —
it predates this session — but it was effectively invisible until the DB version
bump this session forced a wipe mid-testing and a habit's "Done" state didn't
come back after reinstall. Root cause two-layered: (1) logs were never synced
down at all, and (2) the push itself had been silently failing since `isSkipped`
was added to the DTO, because `0003_habit_log_skipped.sql` hadn't been applied to
the live database yet (same class of issue as the bullet-log migration earlier —
`runCatching` swallows the Postgrest rejection with no visible error). **Fix**:
added `HabitLogDao.upsertAll()` and extended `refreshFromRemote()` to also pull
every `habit_logs` row for the user, same one-shot shape `getAllLogsForExport`
already uses. Walked the user through applying the migration.

**Verified, including a real destructive test**: `gradlew assembleDebug
testDebugUnitTest lintDebug` green (7 `HabitStreakTest` + 8 `HabitReportMathTest`
cases, including new skip-transparency cases). On-device: marked "Push-ups" done
(streak badge appeared), then `adb shell pm clear com.loopwise.app` — a full
local data + session wipe — reinstalled nothing (same APK), signed back in with
Google, and confirmed the Reports heatmap still showed the earlier completed day
(14% = 1/7, the historical day lit up) — proof the log genuinely round-tripped
through Supabase and not just local cache luck.

**Still open**: reordering habits (deferred, noted above). The wireless-debugging
connection needed a full re-pair this session (toggled off/on gave a fresh port,
old pairing had gone stale) — not a code issue, just a note for next time this
comes up.

---

## 2026-10-15 (cont'd) — Built task editing

**Objective:** Closes a real gap flagged in the previous audit: tasks could be
created and toggled complete, but never edited or deleted once saved — a typo in
a title had no fix. Mirrors the habit-edit pattern already established in
`CreateEditHabitViewModel`/`Screen`.

**Built:**
- `TaskDao.getById()` + `TaskRepository.getTask()` — same shape as the habit
  equivalent.
- `CreateTaskViewModel.loadForEdit(taskId)`: loads the task, pre-fills the form,
  preserves `id`/`ownerId`/`createdAt`/`completedAt` across the edit (completion
  status isn't touched by this form — that stays a Today-row toggle). `save()`
  now diffs the reminder: a time change just reschedules (same request-code
  overwrites the old alarm), but removing the reminder entirely needs an
  explicit `cancelTaskReminder` call since nothing would overwrite it otherwise.
  Added `delete()`.
- `CreateTaskScreen`: takes an optional `taskId`, dynamic "Edit Task"/"New Task"
  title, a delete icon + confirm dialog (only in edit mode) — same shape as
  `CreateEditHabitScreen`.
- `Destination.EditTask` route, wired in `NavGraph.kt`.
- `TaskRow` gained an `onClick` (tapping the title/row opens edit, separate from
  the existing checkbox toggle) wired from `TodayScreen`.
- **Bonus close-out**: Schedule's TASK block tap, which a few commits ago routed
  to the Today tab as a stand-in ("tasks don't have an edit screen yet"), now
  routes to the real Edit Task screen — the same honest fix HABIT blocks already
  got.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` green (one
real compile error self-caught: missing `import java.time.Instant` in
`CreateTaskViewModel.kt`). Confirmed on-device: created "Buy groceries", tapped
its row, landed on Edit Task pre-filled with the title, delete icon present,
"Save Changes" button — full loop works.

---

## 2026-10-15 (cont'd) — Product audit against spec; fixed five motivation/UX gaps

**Objective:** User asked for a step back — are the originally-intended spec
features all there, is the app actually meaningful for productivity/discipline,
and what's worth improving? Audited the built app against
`habit-tracker-app-spec.md` section by section and gave an honest readout: the
four habit engines, auth, Timer→habit feed, Reports heatmap, and Journal are all
solid; the biggest motivational miss was that streak data (already computed for
Reports) was never surfaced on the Today screen itself, where it's seen daily.
User picked five items from that audit to fix in this pass.

**1. Streak badge on Today habit rows.** New `computeCurrentStreak()` (pure,
tested) in `ui/today/HabitStreak.kt` — counts consecutive scheduled days ending
today with no miss, same logic family as `HabitReportMath`'s but standalone so
Today doesn't need the Reports range machinery. Needed a new DAO query
(`observeLogsInRangeForUser`) to pull all habits' logs for the lookback window in
one shot rather than N per-habit flows. `HabitRow` now shows a 🔥-and-count badge
next to the name pill once `streak > 0`.

**2. Completion micro-animation.** `HabitActionControl`'s checkmark now does a
one-shot scale pulse (1 → 1.35 → 1, spring-damped) the moment a habit flips from
not-done to done — tracked via a `previousDone` guard so it doesn't replay on
every recomposition or fire on initial load of an already-completed habit.

**3. Journal date-range filter.** Added `JournalDateRange` (All time / Last 7
days / Last 30 days) to `JournalFilterState`; filtering happens client-side on
`createdAt` after the existing habit/type DB query, same pattern as the other two
filters. Spec asked for a real date-range picker; went with presets instead to
match the existing dropdown-filter UI rather than introducing a different control
type for one filter.

**4. Schedule block tap.** `ScheduleScreen` now takes `onEditHabit`/`onViewTask`
callbacks, wired in `NavGraph.kt`. A HABIT block jumps to that habit's Edit
screen (same destination as Today's name-pill tap). A TASK block jumps to the
Today tab — tasks don't have their own edit screen yet, so this is the closest
honest match to spec's "jump into edit" rather than a dead tap. Full task editing
remains a known gap.

**5. Wired `JournalRepository.refreshFromRemote()`.** Added the same
init-block-collects-currentUser pattern `TodayViewModel` already uses, plus
`syncPending()` for parity — journal entries now actually sync from Supabase on
launch instead of only reflecting whatever was upserted locally.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` green (5 new
`HabitStreakTest` cases), reinstalled on the Galaxy S23 Ultra, confirmed all five
on-device: streak badge appeared on first completion, checkmark pulse fires,
"Last 7 days" filter correctly kept a same-day entry, tapping a Schedule block
landed on the right habit's Edit screen.

---

## 2026-10-15 (cont'd) — Finished the consolidated device-testing pass; added sign-out

**Objective:** Finish verifying every feature built since the GitHub publish
(Schedule, CSV export, checklist/bullet-log journal, Google Sign-In) that had only
ever been compile/lint/unit-test checked.

**Schedule**: both Agenda and Grid views render correctly — Agenda lists
reminder-based blocks per day, Grid aligns colored blocks to the right
day-column/hour-row. Tap-to-navigate on a block remains a known, documented gap
(not fixed this pass).

**CSV export**: full share-sheet flow works — tapped the download icon, got a
real Android share sheet with the generated `loopwise-export-*.csv` recognized as
a spreadsheet file. Pulled the file directly via `adb run-as` and confirmed the
content is correct (header + row, full-precision value as intended for a raw data
export).

**Checklist journal entries**: created a 2-item checklist ("Chapter 1", "Chapter
2"), saved, confirmed it renders as real checkboxes in the feed, and confirmed
tapping a checkbox toggles + persists (strikethrough applied, state survived).

**Bullet-log journal entries**: created one Task line and one Event line, saved,
confirmed Task renders as a toggleable checkbox and Event renders as static
symbol-prefixed text (only Task lines are meant to be interactive) — toggled the
task line and confirmed it persisted.

**Found a real gap while trying to test Google Sign-In: no way to sign out.**
`AuthRepository.signOut()` existed but was never wired to any UI control anywhere
in the app — there was no settings screen, no profile menu, nothing. This silently
blocked testing Google Sign-In (can't reach the sign-in screen once logged in
without uninstalling). **Fix:** added a sign-out `IconButton` to the Today
screen's top bar (`TodayViewModel.signOut()` → `authRepository.signOut()`) — the
simplest correct place for it given there's no dedicated settings screen yet.

**Google Sign-In: verified fully end-to-end twice.** Tapped "Continue with
Google" → system Credential Manager "Sign in with Google" sheet appeared →
selected the registered test account (akash01shaw@gmail.com) → one-time Google
consent screen → landed signed-in on Today (correctly empty, since this Google
account maps to a brand-new Supabase user distinct from the email/password
account used throughout testing — not a bug). Signed out and signed back in with
Google a second time to confirm repeatability: no re-consent needed, instant
sign-in straight to Today. This closes out the last unverified piece of the
Google Sign-In OAuth setup from earlier this session.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` green after the
sign-out addition (one deprecation warning self-caught and fixed: used
`Icons.AutoMirrored.Filled.Logout` instead of the deprecated `Icons.Filled.Logout`).

**Still open**: `JournalRepository.refreshFromRemote()` still exists but nothing
calls it — journal entries don't pull from Supabase on launch the way
habits/tasks now do (noted in the crash-fix entry above). Not blocking, since
Room is the source of truth for a signed-in session and nothing was lost, but
worth closing for consistency. Also: a real settings/profile screen would be a
better long-term home for sign-out than a bare top-bar icon, but that's a larger
scope than this pass.

---

## 2026-10-15 (cont'd) — Verified Timer, fixed a progress-display formatting bug

**Objective:** Continuing the device-testing pass after the crash fix. Tested
Timer end-to-end: Stopwatch start/pause/stop, Focus habit picker, and the
Timer→habit feed path (`feedLinkedHabit()`), which hadn't been exercised on a
device before.

**Created a Duration-type habit on-device for the first time** ("Study", target 30
min) — the three other habit types (Boolean, Counter, Avoidance) were covered in
the original device-testing round, but Duration never was. Form, color picker, and
save all worked correctly.

**Found and fixed a real display bug**: linked the Timer's Focus to "Study", ran a
~74-second stopwatch session, stopped it, and the Today screen showed `Study
1.3666666666666667 / 30 min` — raw `Double` precision leaking straight into the
UI. Root cause in `HabitRow.kt`'s `progressLabel()`: its local `formatNumber()`
helper only special-cased whole numbers (`value.toLong()`) and fell through to the
Double's raw `.toString()` for anything fractional. **Fix:** fractional values now
format via `"%.1f".format(value)`, giving `1.4 / 30 min`. Checked
`CsvExport.kt`'s `formatCsvNumber()`, which has the identical pattern — left it
alone, since raw precision in a data export (vs. a UI label) is arguably the
correct behavior for someone exporting "their data," not the same bug.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` green,
reinstalled, confirmed on-device — Study now reads `1.4 / 30 min` after a timer
session. Stopwatch start/pause/stop/reset all behaved correctly; Focus picker
correctly disables while a session is running.

**Still in progress**: continuing through the rest of the consolidated
device-testing checklist (Schedule, CSV export, journal entries, Google Sign-In).

---

## 2026-10-15 (cont'd) — Started the consolidated device-testing pass; fixed a crash-on-launch bug

**Objective:** Run the pass that's been queued since Timer/Schedule — every feature
built since the GitHub publish (Timer, Schedule, habit editing, CSV export,
checklist/bullet-log journal, Google Sign-In) had only been compile/lint/unit-test
verified, never actually run.

**Found and fixed a real crash on first launch:** `LoopwiseDatabase`'s
`@Database(version = 1, ...)` was never bumped despite `TimerSessionEntity` (and the
schema changes since) landing after the on-device database was first created.
Because the version number stayed the same, Room never triggers its
upgrade/destructive-migration path at all (`fallbackToDestructiveMigration` in
`AppModule` never got a chance to run) — it just tried to open the stale on-disk
file and failed Room's schema identity check in `onOpen()`, which is a fatal
exception, not a caught error: `IllegalStateException: Room cannot verify the data
integrity... forgot to update the version number`. Confirmed via `adb logcat -b
crash`. **Fix:** bumped `version` to 2 in `LoopwiseDatabase.kt`, which lets the
already-configured destructive-migration fallback actually fire.

**Found and fixed a related gap while investigating:** `TodayViewModel` never
refreshed from Supabase on cold start — `refresh()` was only wired to
`selectDate()`, so after the Room cache gets wiped by the migration above (or on
any fresh install), the Today screen would show empty until the user manually
tapped a date chip, which would have looked like a second bug during testing.
Added an `init` block that calls `refresh()` once per signed-in user as soon as
`authRepository.currentUser` first emits a non-null value.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` green, reinstalled
on the Galaxy S23 Ultra over wireless debugging, relaunched — Today screen now
loads cleanly and correctly re-synced the existing Drink Water (counter) and Quit
Smoking (avoidance) habits from Supabase automatically, no manual refresh needed.
`adb logcat -b crash` clean on relaunch.

**Still open**: `JournalRepository.refreshFromRemote()` exists but nothing calls it
(unlike habits/tasks) — journal entries only populate Room from whatever was
upserted locally, not pulled from Supabase on launch. Not fixed yet since it wasn't
blocking this pass (no journal data was lost — journal entries created today are
still local-only and never left this device to begin with). Flagging as a
pre-existing gap to close alongside the rest of the testing pass.

---

## 2026-10-15 — Set up Google Sign-In (external OAuth config)

**Objective:** Fifth item in the reprioritized list. The app-side code
(`GoogleSignInHelper.kt`'s Credential Manager flow, `AuthRepository.signInWithGoogleIdToken`,
the "Continue with Google" button on `SignInScreen`) was already built in an earlier
session — all that was missing was the real OAuth credentials, which only the user
could create since it needs their Google account. This session was entirely external
setup, walked through step by step rather than written as code.

**Done, in order:**
1. Created a new Google Cloud project ("Loopwise") and ran through the Google Auth
   Platform / OAuth consent screen wizard (External user type, app name + contact
   email).
2. Created a **Web application** OAuth client ("Loopwise Web") — its Client ID is
   what the app uses as `GOOGLE_WEB_CLIENT_ID` / `GetGoogleIdOption.setServerClientId`;
   its Client Secret goes only into Supabase (never into the app or this repo).
3. Enabled the Google provider in Supabase (Authentication → Providers → Google),
   pasting the Web client's Client ID + Client Secret.
4. Created an **Android** OAuth client, package `com.loopwise.app`, registered
   against the debug keystore's SHA-1 fingerprint — required for Credential
   Manager to trust the app's signature; this client's own ID isn't used in code.
5. Added the test Google account under Audience → Test users — required because
   the consent screen is still in **Testing** publishing status (not yet
   verified/published), so sign-in would otherwise fail with "access blocked" for
   any account not explicitly listed. **Known limit**: capped at 100 test users,
   and a production launch would need the consent screen published (and possibly
   Google's app verification, depending on scopes) — not needed for personal/dev
   use, flagged here for later if this ever ships more broadly.
6. Replaced the placeholder in `local.properties` with the real Web Client ID
   (gitignored, never touches the repo).

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green, same as
every other feature this session (this one had no new app code, so no new tests).

**Still NOT device-verified**: the actual sign-in flow (Credential Manager sheet →
Google account picker → Supabase session) — queued with Timer, Schedule, habit
editing, CSV export, and checklist/bullet-log journal entries for the consolidated
device-testing pass.

---

## 2026-10-12 (cont'd) — Built bullet-log journal entries

**Objective:** Fourth item in the reprioritized list, and the last journal entry
type from spec §2.4 ("free text, mood rating, checklist... bullet log").

**Scoping decision:** modeled as the actual Bullet Journal rapid-log method
(distinct from checklist, which is just checkable items) — each line has a kind:
Task (•, togglable done/not-done), Event (○, static), or Note (–, static). This is
the detail spec §2.4 left implicit ("bullet log" as a term of art) rather than
spelling out, so it was worth getting right rather than just re-skinning the
checklist type.

**New, unlike checklist/CSV/habit-editing: needed a real schema change.**
`journal_entries.type`'s check constraint (`'text','mood','checklist'`) was written
before `bullet_log` existed as a value, so items are still JSON-in-`content` (no
new column) but the type string itself needed a new allowed value at the database
level. Added `supabase/migrations/0002_journal_bullet_log.sql` (drops and
recreates the constraint to add `'bullet_log'`) and walked the user through
applying it by hand via the Supabase SQL editor (no `supabase` CLI link in this
environment, same as the original Milestone 1 setup) — confirmed applied on
2026-10-15 (`pg_get_constraintdef` now shows `bullet_log` in the allowed list).

**Built:**
- `BulletKind` (TASK/EVENT/NOTE) + `BulletItem(text, kind, done)` +
  `encodeBulletItems`/`decodeBulletItems` — same JSON-in-content pattern as
  `ChecklistItem`.
- `JournalEntryType.BULLET_LOG` (wire value `bullet_log`, since the enum's default
  `name.lowercase()` wouldn't produce the underscore).
- `NewJournalEntryScreen`: a "Bullet Log" type chip, a kind selector (3 chips show
  the •/○/– symbols), add-line field, and a removable list of pending lines.
- `JournalEntryCard`: Task lines render as a real toggleable `Checkbox` (same
  interactive-in-the-feed pattern as checklist); Event/Note lines render as plain
  symbol-prefixed text, since only tasks have a meaningful "done" state.
- `JournalViewModel.toggleBulletItem()` — same decode/flip/re-encode/upsert shape
  as `toggleChecklistItem`, with a guard that no-ops on non-TASK lines.
- Added "Bullet Log" to the Journal screen's type filter dropdown.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green on the
first attempt (no compile errors this time). Added 4 unit tests (round-trip
through mixed kinds, null/malformed-JSON fallback, symbol uniqueness).

**Still NOT device-verified**: queued with Timer, Schedule, habit editing, CSV
export, and checklist journal entries for the consolidated device-testing pass.

---

## 2026-10-12 (cont'd) — Built checklist journal entries

**Objective:** Third item in the reprioritized list — `JournalEntryType.CHECKLIST`
already existed in the domain enum and the Supabase `journal_entries.type` check
constraint already allowed `'checklist'` (modeled ahead of time to avoid a later
migration); this session just built the UI and storage for it.

**Scoping decision:** items are stored as JSON inside the existing `content` text
column — same pattern `Frequency` and reminders already use for Room/Supabase. No
new table, column, or migration needed. Went a step further than "create-only" and
made checklist items interactively checkable from the Journal feed itself (tapping
a checkbox toggles it and re-upserts the entry) — a checklist you can't check off
later isn't materially different from a text entry, so this was worth the small
extra scope.

**Built:**
- `ChecklistItem(text, checked)` + `encodeChecklistItems`/`decodeChecklistItems`:
  pure, testable JSON helpers in `domain/model/ChecklistItem.kt`, decode falls back
  to an empty list on malformed/unrelated JSON instead of throwing.
- `NewJournalEntryScreen`: a "Checklist" type chip, an add-item text field + button,
  and a removable list of pending items; `saveEntry()` now requires at least one
  item for this type and JSON-encodes them into `content`.
- `JournalEntryCard`: renders checklist items as real `Checkbox` rows (checked
  items get a strikethrough) instead of the generic content-text fallback; each
  checkbox calls back up to `JournalViewModel.toggleChecklistItem(entry, index)`,
  which decodes, flips one item, re-encodes, and calls the existing
  `JournalRepository.upsertEntry` (already upsert-by-id, so no repository changes
  needed for the edit-in-place behavior).
- Added the missing "Checklist" option to the Journal screen's type filter dropdown
  (was already listed in the label `when`, just never offered as a selectable
  filter).

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green (one
real compile error caught and fixed: `Modifier.weight()` doesn't take a top-level
import, it's a `RowScope`/`ColumnScope` member extension — removing the bad import
fixed it). Added 4 unit tests for the encode/decode round-trip and the
malformed/null/unrelated-JSON fallback paths. Lint unchanged at 0 errors.

**Still NOT device-verified**: queued with Timer, Schedule, habit editing, and CSV
export for the consolidated device-testing pass.

---

## 2026-10-12 (cont'd) — Built CSV export

**Objective:** Second item in the reprioritized list — spec §2.7's "Export button
(CSV) for people who want to own their data."

**Scoping decision:** exports the full raw habit-log history (one row per log:
habit name, type, date, value, unit, completed, logged-at), not just the Reports
screen's computed completion percentages — "own their data" reads as data
portability, and a summary printout wouldn't give someone their actual history
back if they wanted to leave.

**Built:**
- `buildHabitLogsCsv()`: pure function, proper CSV field escaping (quotes fields
  containing commas/quotes/newlines, doubles embedded quotes) — kept standalone and
  testable rather than inlined into the ViewModel, same pattern as the other
  "math" files.
- `CsvFileWriter`: writes the CSV to the app's cache dir and returns a
  `FileProvider` content:// Uri. Needed a `<provider>` entry in the manifest and a
  `file_paths.xml` resource — the one new piece of Android plumbing in this
  feature, since sharing a file off-device requires a content Uri, not a raw file
  path, on modern Android.
- `ReportsViewModel.exportCsv()`: fetches all habits + all logs for the user
  (one-shot, not reactive — this is a single user action), builds the CSV, writes
  it, and exposes the resulting Uri once for the screen to launch a
  `ACTION_SEND` share-sheet intent.
- UI: a download icon in the Reports top bar, with a loading spinner while
  exporting and an inline error message if it fails.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green on the
first attempt. Added 7 unit tests for the CSV builder (43 total project-wide),
including the escaping edge cases (commas, quotes, a log whose habit was since
deleted) that are exactly the kind of thing worth testing explicitly rather than
trusting by inspection. Lint unchanged at 0 errors/28 warnings.

**Still NOT device-verified**: the share-sheet flow itself (does the CSV actually
open/attach correctly in another app) — queued with Timer, Schedule, and habit
editing for the consolidated device-testing pass.

---

## 2026-10-12 — Reprioritized roadmap; built habit editing

**Objective:** User reviewed the "not yet implemented" list and chose to prioritize
Google Sign-In, habit editing, CSV export, Health Connect sync, and checklist/
bullet-log journal entries — explicitly **deprioritizing shared
habits/accountability** (the single largest remaining chunk of the original spec,
both the Phase 2 feature itself and the Phase 3 items that depend on it: group
streaks, partner-nudge notifications).

Before starting: answered the user's question about how Health Connect data
fetching actually works (permission flow, `readRecords`/`aggregate` calls scoped to
a time range, writing results into a habit's log the same way a manual tap would) —
and flagged honestly that it's the one feature on the list that can't be verified
without the device *and* real health data already flowing into it from somewhere.
Agreed order: habit editing → CSV export → checklist journal → bullet-log journal →
Google Sign-In → Health Connect (self-contained/no-new-dependency items first,
items needing external setup or real external data last).

**Built: habit editing** (closes the one remaining Phase 1 MVP gap):
- `HabitRepository.getHabit(id)` — trivial wrapper, the DAO method already existed.
- `CreateEditHabitViewModel` now supports a `loadForEdit(habitId)` path that
  prefills the form from an existing habit (preserving its id/ownerId/createdAt so
  saving is an update, not a new row) alongside the original create path. On save,
  reminder times removed during editing are explicitly cancelled via
  `ReminderScheduler.cancelHabitReminder` rather than left as orphaned alarms;
  current reminders are (re)scheduled.
- Added delete, with a confirmation dialog (per the "confirm before destructive
  actions" standard) — cancels that habit's reminders too before removing it.
- Entry point: tapping a habit's name pill on Today (both regular and
  avoidance/quit rows) now opens it for editing, via the `EditHabit` nav route that
  existed in `Destinations.kt` since Milestone 1-3 but was never wired to a screen.

**Verified:** `gradlew assembleDebug testDebugUnitTest lintDebug` — green on the
first attempt, lint unchanged at 0 errors/28 warnings. No new unit tests this
round — the change is mostly CRUD wiring and navigation, not new branchy
calculation logic.

**Still NOT device-verified**: habit editing (built this session), Timer, Schedule.
Queued together for one consolidated device-testing pass per the user's standing
instruction.

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
