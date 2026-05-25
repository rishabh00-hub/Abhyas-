package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM daily_targets ORDER BY targetDate DESC, createdAt DESC")
    fun getAllTargets(): Flow<List<DailyTarget>>

    @Query("SELECT * FROM daily_targets WHERE targetDate = :date ORDER BY createdAt DESC")
    fun getTargetsByDate(date: String): Flow<List<DailyTarget>>

    @Query("SELECT * FROM daily_targets WHERE id = :id LIMIT 1")
    suspend fun getTargetById(id: String): DailyTarget?

    @Query("SELECT * FROM daily_targets")
    suspend fun getAllTargetsSnapshot(): List<DailyTarget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: DailyTarget)

    @Update
    suspend fun updateTarget(target: DailyTarget)

    @Query("DELETE FROM daily_targets WHERE id = :id")
    suspend fun deleteTargetById(id: String)

    @Query("UPDATE daily_targets SET durationLogged = durationLogged + :seconds WHERE id = :id")
    suspend fun incrementLoggedDuration(id: String, seconds: Int)

    @Query("UPDATE daily_targets SET status = :status WHERE id = :id")
    suspend fun updateTargetStatus(id: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMigratedBacklog(item: BacklogItem)

    @Transaction
    suspend fun migrateTargetsToBacklog(targets: List<DailyTarget>, backlogItems: List<BacklogItem>) {
        backlogItems.forEach { insertMigratedBacklog(it) }
        targets.forEach { deleteTargetById(it.id) }
    }
}

@Dao
interface BacklogDao {
    @Query("SELECT * FROM backlog_items ORDER BY createdAt DESC")
    fun getAllBacklogs(): Flow<List<BacklogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBacklog(item: BacklogItem)

    @Update
    suspend fun updateBacklog(item: BacklogItem)

    @Query("DELETE FROM backlog_items WHERE id = :id")
    suspend fun deleteBacklogById(id: String)

    @Query("UPDATE backlog_items SET status = :status WHERE id = :id")
    suspend fun updateBacklogStatus(id: String, status: String)
}

@Dao
interface DPPDao {
    @Query("SELECT * FROM dpp_history_logs ORDER BY date DESC, id DESC")
    fun getAllDPPLogs(): Flow<List<DPPHistoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDPPLog(log: DPPHistoryLog)

    @Query("DELETE FROM dpp_history_logs WHERE id = :id")
    suspend fun deleteDPPLogById(id: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM study_sessions WHERE associatedTargetId = :targetId")
    suspend fun deleteSessionsByTargetId(targetId: String)
}

@Dao
interface AspirationDao {
    @Query("SELECT * FROM daily_aspirations")
    fun getAllAspirations(): Flow<List<DailyAspiration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAspiration(aspiration: DailyAspiration)

    @Update
    suspend fun updateAspiration(aspiration: DailyAspiration)

    @Query("DELETE FROM daily_aspirations WHERE id = :id")
    suspend fun deleteAspirationById(id: String)
}

@Database(
    entities = [
        DailyTarget::class,
        BacklogItem::class,
        DPPHistoryLog::class,
        StudySession::class,
        DailyAspiration::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao
    abstract fun backlogDao(): BacklogDao
    abstract fun dppDao(): DPPDao
    abstract fun sessionDao(): SessionDao
    abstract fun aspirationDao(): AspirationDao
}
