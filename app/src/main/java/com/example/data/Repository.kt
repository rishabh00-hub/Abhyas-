package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class StudyRepository(private val db: AppDatabase) {

    // Target API
    private val targetDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val allTargets: Flow<List<DailyTarget>> = getAllTargetsWithMigration()

    fun getTargetsByDate(date: String): Flow<List<DailyTarget>> =
        db.targetDao().getTargetsByDate(date).map { targets -> migrateExpiredTargets(targets) }

    fun getAllTargetsWithMigration(): Flow<List<DailyTarget>> =
        db.targetDao().getAllTargets().map { targets -> migrateExpiredTargets(targets) }
    suspend fun getTargetById(id: String): DailyTarget? = db.targetDao().getTargetById(id)
    suspend fun insertTarget(target: DailyTarget) = db.targetDao().insertTarget(target)
    suspend fun updateTarget(target: DailyTarget) = db.targetDao().updateTarget(target)
    suspend fun deleteTargetById(id: String) = db.targetDao().deleteTargetById(id)
    suspend fun incrementLoggedDuration(id: String, seconds: Int) = db.targetDao().incrementLoggedDuration(id, seconds)
    suspend fun updateTargetStatus(id: String, status: String) = db.targetDao().updateTargetStatus(id, status)

    // Backlog API
    val allBacklogs: Flow<List<BacklogItem>> = db.backlogDao().getAllBacklogs()
    suspend fun insertBacklog(item: BacklogItem) = db.backlogDao().insertBacklog(item)
    suspend fun updateBacklog(item: BacklogItem) = db.backlogDao().updateBacklog(item)
    suspend fun deleteBacklogById(id: String) = db.backlogDao().deleteBacklogById(id)
    suspend fun updateBacklogStatus(id: String, status: String) = db.backlogDao().updateBacklogStatus(id, status)
    suspend fun completeBacklogAndMigrateToHistory(item: BacklogItem, durationSeconds: Int = 45 * 60) {
        val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
        val historySession = StudySession(
            id = item.id,
            startTime = nowStr,
            endTime = nowStr,
            durationSeconds = durationSeconds,
            subject = item.subject,
            type = "Backlog: ${item.type}",
            associatedTargetId = null,
            notes = "Backlog Completed: ${item.title}\nDifficulty: ${item.difficulty}\nCreated At: ${item.createdAt}"
        )
        db.backlogDao().migrateBacklogToHistory(item.id, historySession)
    }

    // DPP API
    val allDPPLogs: Flow<List<DPPHistoryLog>> = db.dppDao().getAllDPPLogs()
    suspend fun insertDPPLog(log: DPPHistoryLog) = db.dppDao().insertDPPLog(log)
    suspend fun deleteDPPLogById(id: String) = db.dppDao().deleteDPPLogById(id)

    // Session API
    val allSessions: Flow<List<StudySession>> = db.sessionDao().getAllSessions()
    suspend fun insertSession(session: StudySession) = db.sessionDao().insertSession(session)
    suspend fun deleteSessionById(id: String) = db.sessionDao().deleteSessionById(id)
    suspend fun deleteSessionsByTargetId(targetId: String) = db.sessionDao().deleteSessionsByTargetId(targetId)

    // Aspiration API
    val allAspirations: Flow<List<DailyAspiration>> = db.aspirationDao().getAllAspirations()
    suspend fun insertAspiration(aspiration: DailyAspiration) = db.aspirationDao().insertAspiration(aspiration)
    suspend fun updateAspiration(aspiration: DailyAspiration) = db.aspirationDao().updateAspiration(aspiration)
    suspend fun deleteAspirationById(id: String) = db.aspirationDao().deleteAspirationById(id)

    private suspend fun migrateExpiredTargets(targets: List<DailyTarget>): List<DailyTarget> {
        val today = LocalDate.now()
        val remainingTargets = mutableListOf<DailyTarget>()
        targets.forEach { target ->
            val targetDate = runCatching { LocalDate.parse(target.targetDate, targetDateFormatter) }.getOrNull()
            val shouldMigrate = targetDate != null && targetDate.isBefore(today) && target.status != "completed"
            if (shouldMigrate) {
                val backlogItem = BacklogItem(
                    id = target.id,
                    title = target.title,
                    subject = target.subject,
                    type = target.type,
                    difficulty = "Critical",
                    notes = null,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()),
                    status = "pending"
                )
                db.targetDao().migrateTargetToBacklog(target, backlogItem)
            } else {
                remainingTargets.add(target)
            }
        }
        return remainingTargets
    }
}
