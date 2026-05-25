package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val db: AppDatabase) {

    // Target API
    val allTargets: Flow<List<DailyTarget>> = db.targetDao().getAllTargets()
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
}
