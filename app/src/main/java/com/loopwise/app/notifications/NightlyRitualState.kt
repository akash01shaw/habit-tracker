package com.loopwise.app.notifications

import android.content.Context
import java.time.LocalDate

/**
 * Tracks whether the nightly ritual has been completed for the current date — a single
 * scalar with no relations or query needs, so SharedPreferences fits better here than a
 * Room entity + DAO + migration (every other local-persistence need in this app is
 * genuinely relational and synced, unlike this one — it's purely "did the user already do
 * tonight's review," used to decide whether to show/re-show the notification).
 */
class NightlyRitualState(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompletedToday(): Boolean =
        prefs.getString(KEY_LAST_COMPLETED_DATE, null) == LocalDate.now().toString()

    fun markCompletedToday() {
        prefs.edit().putString(KEY_LAST_COMPLETED_DATE, LocalDate.now().toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "nightly_ritual"
        const val KEY_LAST_COMPLETED_DATE = "last_completed_date"
    }
}
