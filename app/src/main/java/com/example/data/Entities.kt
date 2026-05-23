package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_targets")
data class DailyTarget(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String, // 'Physics' | 'Chemistry' | 'Maths' | 'Biology' | 'General' | 'Other'
    val type: String, // 'Lecture' | 'DPP' | 'Homework' | 'Backlog' | 'Revision' | 'Self Study' | 'Test'
    val batch: String?,
    val status: String, // 'not-started' | 'in-progress' | 'completed'
    val durationProposed: Int, // in minutes
    val durationLogged: Int, // in seconds
    val createdAt: String,
    val targetDate: String, // YYYY-MM-DD
    val dppQuestionsCount: Int? = null,
    val dppSolvedCount: Int? = null,
    val dppCorrectCount: Int? = null,
    val chapter: String? = null,
    val lectureNumber: String? = null
)

@Entity(tableName = "backlog_items")
data class BacklogItem(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val type: String,
    val difficulty: String, // 'Easy' | 'Medium' | 'Critical'
    val notes: String?,
    val createdAt: String,
    val status: String // 'pending' | 'resolved'
)

@Entity(tableName = "dpp_history_logs")
data class DPPHistoryLog(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val date: String, // YYYY-MM-DD
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val timeSpentMinutes: Int,
    val notes: String?
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey val id: String,
    val startTime: String, // ISO String
    val endTime: String, // ISO String
    val durationSeconds: Int,
    val subject: String,
    val type: String,
    val associatedTargetId: String?,
    val notes: String?
)

@Entity(tableName = "daily_aspirations")
data class DailyAspiration(
    @PrimaryKey val id: String,
    val text: String,
    val isCompleted: Boolean
)
