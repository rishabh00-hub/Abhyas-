package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate

class StudyRepository(private val db: AppDatabase) {

    // Target API
    val allTargets: Flow<List<DailyTarget>> = db.targetDao().getAllTargets()
        .onStart { migratePastUncompletedTargetsToBacklog() }
        .onEach { targets ->
            val today = LocalDate.now()
            val hasPastUncompleted = targets.any { target ->
                target.status != "completed" &&
                        runCatching { LocalDate.parse(target.targetDate) }.getOrNull()?.isBefore(today) == true
            }
            if (hasPastUncompleted) {
                migratePastUncompletedTargetsToBacklog()
            }
        }
    fun getTargetsByDate(date: String): Flow<List<DailyTarget>> = db.targetDao().getTargetsByDate(date)
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

    private suspend fun migratePastUncompletedTargetsToBacklog() {
        val today = LocalDate.now()
        val overdueTargets = db.targetDao().getAllTargetsSnapshot().filter { target ->
            if (target.status == "completed") return@filter false
            val targetDate = runCatching { LocalDate.parse(target.targetDate) }.getOrNull() ?: return@filter false
            targetDate.isBefore(today)
        }
        if (overdueTargets.isEmpty()) return

        val backlogItems = overdueTargets.map { target ->
            BacklogItem(
                id = target.id,
                title = target.title,
                subject = target.subject,
                type = target.type,
                difficulty = "Medium",
                notes = buildString {
                    append("Migrated from target dated ${target.targetDate}.")
                    target.chapter?.takeIf { it.isNotBlank() }?.let { append("\nChapter: $it") }
                    target.lectureNumber?.takeIf { it.isNotBlank() }?.let { append("\nLecture: $it") }
                    target.batch?.takeIf { it.isNotBlank() }?.let { append("\nBatch: $it") }
                },
                createdAt = target.createdAt,
                status = "pending"
            )
        }
        db.targetDao().migrateTargetsToBacklog(overdueTargets, backlogItems)
    }
}
