package com.loopwise.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.loopwise.app.domain.model.HealthSyncSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only Health Connect access for the two sources spec §2.2 names against a
 * habit: Steps and Active Minutes. `client` is lazily null when Health Connect isn't
 * installed/supported on this device — every call degrades to "no data" rather than
 * crashing, since this is an optional auto-fill, not a required dependency.
 */
class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val isAvailable: Boolean get() = client != null

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun permissionRequestContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return runCatching { c.permissionController.getGrantedPermissions() }
            .getOrNull()
            ?.containsAll(permissions) ?: false
    }

    /** Today's total for [source], or null if Health Connect isn't available/permitted. */
    suspend fun readValueForToday(source: HealthSyncSource): Double? {
        val c = client ?: return null
        if (!hasAllPermissions()) return null
        val range = todayTimeRangeFilter()
        return runCatching {
            when (source) {
                HealthSyncSource.STEPS -> readStepsTotal(c, range)
                HealthSyncSource.ACTIVE_MINUTES -> readActiveMinutesTotal(c, range)
            }
        }.getOrNull()
    }

    private suspend fun readStepsTotal(client: HealthConnectClient, range: TimeRangeFilter): Double {
        val result = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))
        return (result[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble()
    }

    private suspend fun readActiveMinutesTotal(client: HealthConnectClient, range: TimeRangeFilter): Double {
        val sessions = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records
        return sumExerciseMinutes(sessions.map { it.startTime to it.endTime })
    }

    private fun todayTimeRangeFilter(): TimeRangeFilter {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        return TimeRangeFilter.between(startOfDay, Instant.now())
    }
}
