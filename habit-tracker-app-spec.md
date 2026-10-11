# Habit & Task Tracker — Product Specification (Working title: "Loopwise")

## 1. Vision
A single app that unifies four things people usually juggle across separate apps: **daily to-dos, habit building, habit quitting, and reflection (journal)** — tied together with a lightweight **schedule/calendar** and **focus timer**, and backed by clear **analytics**. Free/open, with no artificial paywall on core tracking.

**Platform & architecture decisions (locked in):**
- **Android only**, native build.
- **Account-based from day one**, with **cloud sync** — this also unlocks the shared/accountability use case below.
- **Shared use**: habits and tasks can optionally be shared with another account (partner, friend, small group) for mutual accountability.

---

## 2. Core Modules

### 2.1 Today View (home screen)
- Horizontal 7-day date strip (current week), tap any day to jump.
- Two collapsible sections by default:
  - **Tasks** — one-off items with due date/time, priority flag, optional note icon, optional reminder bell. Checkbox to complete.
  - **Habits** — recurring items for that day, rendered as colored rows with a progress control appropriate to type (see 2.2).
- Section header shows a count badge + ring showing % complete for the day.
- Long-press a row to reorder or reveal quick actions (edit, skip, delete).

### 2.2 Habit Types (this is the engine of the app)
Every habit is one of four types, each with its own input control:

| Type | Example | Input control | Progress stored as |
|---|---|---|---|
| **Boolean (check-off)** | Wake up early | Single tap checkbox | done / not done |
| **Counter (build-up)** | Drink water, Steps | Tap `+1` / `+custom`, or auto-sync | current / target per day |
| **Duration** | Read, Study | Manual entry or built-in timer | minutes logged / target |
| **Avoidance (quit/limit)** | Quit smoking, Limit caffeine | "Reset streak" button + auto-running clock | continuous streak (d/h/m/s) |

Each habit has: name, color/icon, frequency (daily / specific weekdays / X times per week / X times per month), target value + unit, reminder time(s), and an optional link to Health data (steps, active minutes) for auto-fill.

### 2.3 Quit / Avoidance Habits
- Dedicated sub-section ("Habits I'm Quitting") separate from build habits so streaks are visually distinct (dark/warning color palette vs. bright build-habit palette).
- Live-counting streak (days, hours, minutes, seconds) since last reset.
- "I slipped" button resets the clock and logs the relapse timestamp (used later for pattern insights — e.g., "you tend to slip on weekend evenings").

### 2.4 Journal
- Chronological feed of entries, each auto-tagged with the habit/task it's linked to (or freeform "General" entry).
- Entry types: free text, mood rating (slider or 5-point scale stored as %), checklist (e.g. subject checklist while studying), bullet log.
- Filters: by habit, by date range, by entry type.
- Rich text is optional for v1 — plain text + highlight/bold is enough.

### 2.5 Schedule / Calendar
- Week-grid view (time down the side, days across) auto-populated from habits/tasks that have a time window.
- Color = habit/task color from the Today view, so it's visually consistent.
- Tap a block to jump into edit or to start its timer.
- Toggle to a simple list/agenda view for smaller screens.

### 2.6 Focus Timer
- Two modes: **Stopwatch** (count up, useful for open-ended sessions) and **Countdown** (set duration).
- "Focus" field lets you attach the session to a specific habit (e.g., Read, Study) so logged time feeds that habit's progress automatically.
- Session log underneath shows Start / Pause / Resume / Stop events with timestamps — useful for the reports later ("average session length," "interruptions per session").

### 2.7 Reports / Analytics
- Time range toggle: Week / Month / Year.
- Per-habit row: name, target description, completion %, and a heatmap strip (GitHub-style contribution grid) for the period — darker = more complete that day.
- Sort by completion % or by streak length.
- Export button (CSV) for people who want to own their data.

### 2.8 Health Sync (optional, phase 2)
- Read-only pull from Apple Health / Google Fit for Steps, Stand hours, Distance, Active calories, Workouts.
- Synced habits show a small sync icon instead of a manual `+` button; user can still override manually.

### 2.9 Widgets
- Home-screen widget(s): a single habit's ring/progress, or a stacked list of top 3-4 habits due today, or "add journal entry" quick-launch.
- Widget taps deep-link straight into the relevant logging screen, not just the app root.

### 2.10 Shared Habits / Accountability
- Any habit can be flagged **Shared** at creation and linked to one or more other accounts via an invite (link or username/email lookup).
- Shared habit behavior:
  - Each member logs their own progress independently — a shared habit is "same goal, separate tracking," not one shared checkbox (avoids one person accidentally completing it for another).
  - A small avatar row on the habit card shows each member's status for today (done / in-progress / not started) at a glance.
  - Optional streak framing: "group streak" = every member completed the habit that day; breaks if anyone misses (good for mutual-accountability quitting habits like Quit Smoking together).
- Shared journal entries are opt-in per entry (default private) — journaling stays personal unless explicitly shared.
- Notifications: optional nudge when a shared-habit partner falls behind ("Jordan hasn't logged Steps today").
- Permissions: a member can leave a shared habit at any time, which converts it to a personal (unshared) copy for them without affecting others.

---

## 3. Information Architecture / Navigation

Bottom tab bar, 5 tabs:
1. **Today** (home)
2. **Schedule**
3. **Timer**
4. **Reports**
5. **Journal**

Habit/task creation is a floating action button on Today, not a separate tab — keeps the tab bar focused on "views," not "actions."

---

## 4. Data Model (simplified)

```
User {
  id, email, display_name, avatar_url,
  created_at
}

Habit {
  id, owner_id, name, color, icon,
  type: boolean | counter | duration | avoidance,
  frequency: { kind: daily|weekdays|per_week|per_month, days[]|count },
  target_value, unit,
  reminders: [time],
  health_sync_source: null | "steps" | "stand" | ...,
  is_shared: bool,
  created_at
}

HabitShare {
  id, habit_id, member_user_id,
  status: invited | active | left,
  joined_at
}
// Each active member gets their own HabitLog rows against the same habit_id,
// so progress is tracked per-person even though the habit definition is shared.

HabitLog {
  id, habit_id, user_id, date,
  value (number or bool),
  note (optional),
  logged_at
}

Task {
  id, owner_id, title, due_date, due_time, priority_flag,
  note, reminder_time, completed_at
}

JournalEntry {
  id, owner_id, linked_habit_id (nullable), type: text|mood|checklist,
  content, mood_value (0-100, nullable),
  visibility: private | shared,
  created_at
}

TimerSession {
  id, owner_id, habit_id (nullable), mode: stopwatch|countdown,
  target_seconds (nullable), events: [{type: start|pause|resume|stop, at}]
}
```

---

## 5. Visual/UX Direction
- Distinct color per habit chosen from a curated palette (12–16 colors) so the eye learns to associate color → habit fast, same as calendar apps.
- Build habits: bright, saturated palette. Quit habits: muted/dark palette — reinforces the different psychological framing (growth vs. restraint).
- Strikethrough + faded row once a habit is completed for the day (light celebratory micro-animation, not intrusive).
- Progress bars use dashed/segmented style for counters (visually shows "how many taps to go") vs. solid fill for durations.
- Keep density high but tappable targets ≥44pt — this app lives or dies on daily quick-logging, so friction per tap matters more than visual flourish.

---

## 6. Suggested Tech Stack (Android-native)
- **App:** Kotlin + Jetpack Compose (modern, first-party, best widget/notification support on Android).
- **Local cache:** Room (SQLite) as an offline-first cache — app stays usable without connectivity, syncs when back online.
- **Backend/auth/sync:** Firebase (Auth + Firestore) or Supabase (Postgres + Auth + Realtime) — either handles account creation, cross-device sync, and the realtime updates needed for shared habits (e.g., partner's status updating live). Supabase is a reasonable default if you want SQL and to avoid vendor lock-in; Firebase is faster to stand up if you want less backend config.
- **Sharing/invites:** backend-generated invite links or username lookup, resolved server-side into `HabitShare` rows; realtime listener on shared habits so each member's card updates without a manual refresh.
- **Notifications:** WorkManager + AlarmManager for local reminders; Firebase Cloud Messaging for cross-account nudges (e.g., partner-fell-behind alerts).
- **Health sync:** Health Connect (Android's unified health API, replacing Google Fit) for Steps, Distance, Active minutes, Workouts.
- **Widgets:** Jetpack Glance for home-screen widgets.
- **Charts/heatmaps:** Vico or a hand-rolled Compose Canvas grid for the yearly heatmap.

---

## 7. MVP Scope (Phase 1)
1. Account creation + login (email or Google sign-in), cloud sync of habits/tasks from day one.
2. Today view: tasks + boolean/counter/duration habits, manual logging only.
3. Habit creation/edit flow with frequency + reminders.
4. Basic weekly Reports (completion % + streak, no heatmap yet).
5. Simple Journal (text + mood only, private by default).
6. Local notifications for reminders.

**Phase 2:** Shared habits (invite flow, per-member logging, avatar status row), quit-habit streak tracking, Schedule/calendar view, Focus timer, CSV export.
**Phase 3:** Health Connect sync, home-screen widgets (Glance), yearly heatmap, group-streak logic, partner-nudge notifications, checklist journal entries.

---

## 8. Remaining Decisions
- Sign-in method(s): email/password, Google Sign-In, or both?
- Invite mechanism for shared habits: shareable link, QR code, or username/email search — or all three?
- Firebase vs. Supabase for the backend (affects cost model and how much SQL flexibility you want later).
