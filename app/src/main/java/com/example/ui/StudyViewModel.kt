package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AbhyasApplication
import com.example.data.BacklogItem
import com.example.data.DailyAspiration
import com.example.data.DailyTarget
import com.example.data.DPPHistoryLog
import com.example.data.StudySession
import com.example.data.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    // --- NAVIGATION TABS ---
    // Tab values: "targets", "timer", "backlog", "dpp", "history"
    var activeTab by mutableStateOf("targets")
        private set

    fun switchTab(tab: String) {
        activeTab = tab
    }

    // --- TARGETS STATE ---
    // Filter values: "today", "upcoming", "all"
    var targetDateFilter by mutableStateOf("today")
        private set

    fun setTargetFilter(filter: String) {
        targetDateFilter = filter
    }

    var isTargetFormExpanded by mutableStateOf(false)

    // --- USER PROFILE STATE ---
    private val prefs = application.getSharedPreferences("abhyas_profile_prefs", android.content.Context.MODE_PRIVATE)

    var isDarkTheme by mutableStateOf(prefs.getBoolean("is_dark_theme", true))
        private set

    var isHistoryEnabled by mutableStateOf(prefs.getBoolean("is_history_enabled", true))
        private set

    fun toggleHistoryEnabled() {
        isHistoryEnabled = !isHistoryEnabled
        prefs.edit().putBoolean("is_history_enabled", isHistoryEnabled).apply()
    }

    private var lastThemeToggleTime = 0L

    fun toggleTheme() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastThemeToggleTime < 500L) {
            return
        }
        lastThemeToggleTime = currentTime
        isDarkTheme = !isDarkTheme
        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
    }

    var userName by mutableStateOf(prefs.getString("user_name", "Aspirant") ?: "Aspirant")
        private set
    var userPlatform by mutableStateOf(prefs.getString("user_platform", "Physics Wallah") ?: "Physics Wallah")
        private set
    var userBatch by mutableStateOf(prefs.getString("user_batch", "Lakshya JEE 2026") ?: "Lakshya JEE 2026")
        private set
    var userPreparation by mutableStateOf(prefs.getString("user_preparation", "IIT JEE Preparation") ?: "IIT JEE Preparation")
        private set

    fun updateProfile(name: String, platform: String, batch: String, preparation: String) {
        userName = name.trim().ifEmpty { "Aspirant" }
        userPlatform = platform.trim().ifEmpty { "Physics Wallah" }
        userBatch = batch.trim().ifEmpty { "Lakshya JEE 2026" }
        userPreparation = preparation.trim().ifEmpty { "IIT JEE Preparation" }

        prefs.edit().apply {
            putString("user_name", userName)
            putString("user_platform", userPlatform)
            putString("user_batch", userBatch)
            putString("user_preparation", userPreparation)
            apply()
        }
    }

    // --- TEMPLATES STATE ---
    var targetTemplates by mutableStateOf<List<TargetTemplate>>(emptyList())
        private set

    var dppPresets by mutableStateOf<List<DppPreset>>(emptyList())
        private set

    var todayDate by mutableStateOf(currentDateString())
        private set

    init {
        loadTemplates()
        loadDppPresets()
        startTodayDateUpdater()
    }

    private fun loadTemplates() {
        val saved = prefs.getString("target_templates_json", null)
        if (saved != null) {
            try {
                val list = mutableListOf<TargetTemplate>()
                saved.split("|||").forEach { raw ->
                    val parts = raw.split("###")
                    if (parts.size >= 6) {
                        list.add(
                            TargetTemplate(
                                id = parts[0],
                                subject = parts[1],
                                batch = parts[2],
                                type = parts[3],
                                chapter = parts[4],
                                durationMinutes = parts[5].toIntOrNull() ?: 45
                            )
                        )
                    }
                }
                targetTemplates = list
            } catch (e: Exception) {
                targetTemplates = getDefaultTemplates()
            }
        } else {
            targetTemplates = getDefaultTemplates()
        }
    }

    private fun getDefaultTemplates(): List<TargetTemplate> {
        return listOf(
            TargetTemplate(UUID.randomUUID().toString(), "Physics", "Lakshya PW", "Lecture", "Electrostatics", 90),
            TargetTemplate(UUID.randomUUID().toString(), "Chemistry", "Arjuna PW", "DPP", "Coordination Compounds", 45),
            TargetTemplate(UUID.randomUUID().toString(), "Maths", "Lakshya PW", "Lecture", "Vectors & 3D Geometry", 90),
            TargetTemplate(UUID.randomUUID().toString(), "Biology", "Yakeen NEET PW", "Self Study", "Cell Structure", 60)
        )
    }

    private fun saveTemplates() {
        val serialized = targetTemplates.joinToString("|||") {
            "${it.id}###${it.subject}###${it.batch}###${it.type}###${it.chapter}###${it.durationMinutes}"
        }
        prefs.edit().putString("target_templates_json", serialized).apply()
    }

    private fun loadDppPresets() {
        val saved = prefs.getString("dpp_presets_json", null)
        if (saved != null) {
            try {
                val list = mutableListOf<DppPreset>()
                saved.split("|||").forEach { raw ->
                    val parts = raw.split("###")
                    if (parts.size >= 7) {
                        list.add(
                            DppPreset(
                                id = parts[0],
                                title = parts[1],
                                subject = parts[2],
                                totalQuestions = parts[3].toIntOrNull() ?: 10,
                                attempted = parts[4].toIntOrNull() ?: 0,
                                correct = parts[5].toIntOrNull() ?: 0,
                                durationMinutes = parts[6].toIntOrNull() ?: 20
                            )
                        )
                    }
                }
                dppPresets = list
            } catch (e: Exception) {
                dppPresets = emptyList()
            }
        } else {
            dppPresets = emptyList()
        }
    }

    private fun saveDppPresets() {
        val serialized = dppPresets.joinToString("|||") {
            "${it.id}###${it.title}###${it.subject}###${it.totalQuestions}###${it.attempted}###${it.correct}###${it.durationMinutes}"
        }
        prefs.edit().putString("dpp_presets_json", serialized).apply()
    }

    fun addDppPreset(
        title: String,
        subject: String,
        totalQuestions: Int,
        attempted: Int,
        correct: Int,
        durationMinutes: Int
    ) {
        val newPreset = DppPreset(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            subject = subject,
            totalQuestions = totalQuestions,
            attempted = attempted,
            correct = correct,
            durationMinutes = durationMinutes
        )
        dppPresets = dppPresets + newPreset
        saveDppPresets()
    }

    fun deleteDppPreset(id: String) {
        dppPresets = dppPresets.filter { it.id != id }
        saveDppPresets()
    }

    fun addTemplate(subject: String, batch: String, type: String, chapter: String, duration: Int) {
        val newTemplate = TargetTemplate(
            id = UUID.randomUUID().toString(),
            subject = subject,
            batch = batch,
            type = type,
            chapter = chapter,
            durationMinutes = duration
        )
        targetTemplates = targetTemplates + newTemplate
        saveTemplates()
    }

    fun deleteTemplate(id: String) {
        targetTemplates = targetTemplates.filter { it.id != id }
        saveTemplates()
    }

    val allTargets: StateFlow<List<DailyTarget>> = repository.allTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- BACKLOGS STATE ---
    val allBacklogs: StateFlow<List<BacklogItem>> = repository.allBacklogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- DPP STATE ---
    val allDPPLogs: StateFlow<List<DPPHistoryLog>> = repository.allDPPLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter DPP by specific subject in chart
    var dppSubjectFilter by mutableStateOf("All Subjects")
        private set

    fun setDppFilter(subject: String) {
        dppSubjectFilter = subject
    }

    // --- SESSIONS STATE ---
    val allSessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History log Filters
    var historySubjectFilter by mutableStateOf("All")
    var historyTimelineFilter by mutableStateOf("All") // "All", "Day", "Week", "Month"

    // --- ASPIRATION STATE ---
    val allAspirations: StateFlow<List<DailyAspiration>> = repository.allAspirations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FOCUS TIMER HUB STATE ---
    var timerMode by mutableStateOf("Pomodoro") // "Pomodoro" or "Stopwatch"
    var timerState by mutableStateOf("Idle") // "Idle", "Running", "Paused", "Finished"
    var durationProposedMinutes by mutableStateOf(25) // Selected Pomodoro minutes (e.g., 15, 25, 45, or custom)
    var secondsElapsedOrRemaining by mutableStateOf(25 * 60) // Active remaining or elapsed seconds

    var boundTargetId by mutableStateOf<String?>(null) // Link study session to target ID
    var timerNotes by mutableStateOf("")
    var alarmAlertsEnabled by mutableStateOf(true)

    // Stopwatch alerts feature settings
    var stopwatchAlertType by mutableStateOf("None") // "None", "Interval", "Single"
    var stopwatchAlertIntervalSeconds by mutableStateOf(300)

    // Stopwatch Checkpoints state
    var stopwatchCheckpoints by mutableStateOf(listOf<StopwatchCheckpoint>())
        private set

    private var timerJob: Job? = null
    private var baseTimeSeconds = 0
    private var actualSessionStartISO: String? = null

    init {
        // Pre-populate seed data (Aspirations & initial guidelines) if database is empty
        viewModelScope.launch {
            val currentAspirations = repository.allAspirations.first()
            if (currentAspirations.isEmpty()) {
                repository.insertAspiration(DailyAspiration(UUID.randomUUID().toString(), "Resolve today's maths vector backlog", false))
                repository.insertAspiration(DailyAspiration(UUID.randomUUID().toString(), "Submit Physics Dynamic Practice Problems (DPP)", false))
                repository.insertAspiration(DailyAspiration(UUID.randomUUID().toString(), "Complete 2 hours of focused revision", false))
            }
        }
    }

    // --- TIMER CORE OPERATORS ---
    fun selectTimerMode(mode: String) {
        if (timerState == "Running" || timerState == "Paused") return // Lock changes during run
        timerMode = mode
        resetTimer()
    }

    fun selectBoundTarget(targetId: String?) {
        boundTargetId = targetId
    }

    fun setCustomPomodoroMinutes(minutes: Int) {
        if (timerState == "Running" || timerState == "Paused") return
        durationProposedMinutes = minutes
        if (timerMode == "Pomodoro") {
            secondsElapsedOrRemaining = minutes * 60
        }
    }

    fun startTimer() {
        if (timerState == "Running") return
        if (timerState == "Idle" || timerState == "Finished") {
            actualSessionStartISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
            if (timerMode == "Pomodoro") {
                secondsElapsedOrRemaining = durationProposedMinutes * 60
            } else {
                secondsElapsedOrRemaining = 0
            }
        }
        timerState = "Running"

        timerJob = viewModelScope.launch {
            while (timerState == "Running") {
                delay(1000L)
                if (timerMode == "Pomodoro") {
                    if (secondsElapsedOrRemaining > 0) {
                        secondsElapsedOrRemaining -= 1
                        // Warning chime 1 minute before finish
                        if (secondsElapsedOrRemaining == 60) {
                            playWarningTone()
                        }
                    } else {
                        // Finished Pomodoro countdown
                        timerState = "Finished"
                        playCompletionTone()
                    }
                } else {
                    // Stopwatch counts up
                    secondsElapsedOrRemaining += 1

                    // Interval/Target ring checks
                    if (secondsElapsedOrRemaining > 0 && stopwatchAlertIntervalSeconds > 0) {
                        if (stopwatchAlertType == "Interval") {
                            val intervalSec = stopwatchAlertIntervalSeconds
                            if (secondsElapsedOrRemaining % intervalSec == 0) {
                                playIntervalBeep()
                            }
                        } else if (stopwatchAlertType == "Single") {
                            val targetSec = stopwatchAlertIntervalSeconds
                            if (secondsElapsedOrRemaining == targetSec) {
                                playIntervalBeep()
                            }
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        if (timerState != "Running") return
        timerState = "Paused"
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerState = "Idle"
        if (timerMode == "Pomodoro") {
            secondsElapsedOrRemaining = durationProposedMinutes * 60
        } else {
            secondsElapsedOrRemaining = 0
        }
        boundTargetId = null
        timerNotes = ""
        actualSessionStartISO = null
        stopwatchCheckpoints = emptyList()
    }

    fun addStopwatchCheckpoint() {
        if (timerMode != "Stopwatch" || timerState != "Running") return
        val currentElapsed = secondsElapsedOrRemaining
        val lastElapsed = stopwatchCheckpoints.lastOrNull()?.elapsedSeconds ?: 0
        val split = currentElapsed - lastElapsed
        val qNum = stopwatchCheckpoints.size + 1
        val formattedTotal = String.format(Locale.getDefault(), "%02d:%02d", currentElapsed / 60, currentElapsed % 60)
        val formattedSplit = String.format(Locale.getDefault(), "%02d:%02d", split / 60, split % 60)
        val newCheckpoint = StopwatchCheckpoint(
            id = UUID.randomUUID().toString(),
            questionNumber = qNum,
            elapsedSeconds = currentElapsed,
            splitSeconds = split,
            timeFormatted = formattedTotal,
            splitFormatted = formattedSplit,
            label = "Question $qNum"
        )
        stopwatchCheckpoints = stopwatchCheckpoints + newCheckpoint
        playWarningTone() // Play a soft tone for audio verification feedback
    }

    fun updateCheckpointLabel(id: String, newLabel: String) {
        stopwatchCheckpoints = stopwatchCheckpoints.map {
            if (it.id == id) it.copy(label = newLabel) else it
        }
    }

    fun deleteStopwatchCheckpoint(id: String) {
        val filtered = stopwatchCheckpoints.filter { it.id != id }
        val updated = mutableListOf<StopwatchCheckpoint>()
        filtered.forEachIndexed { index, cp ->
            val prevElapsed = if (index == 0) 0 else updated[index - 1].elapsedSeconds
            val split = cp.elapsedSeconds - prevElapsed
            val fSplit = String.format(Locale.getDefault(), "%02d:%02d", split / 60, split % 60)
            val newLabel = if (cp.label.startsWith("Question ")) "Question ${index + 1}" else cp.label
            updated.add(
                cp.copy(
                    questionNumber = index + 1,
                    splitSeconds = split,
                    splitFormatted = fSplit,
                    label = newLabel
                )
            )
        }
        stopwatchCheckpoints = updated
    }

    fun logCurrentSession() {
        viewModelScope.launch {
            val endISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
            val startISO = actualSessionStartISO ?: endISO

            val calculatedDurationSeconds = if (timerMode == "Pomodoro") {
                val totalSeconds = durationProposedMinutes * 60
                val diff = totalSeconds - secondsElapsedOrRemaining
                if (diff > 0) diff else totalSeconds
            } else {
                secondsElapsedOrRemaining
            }

            if (calculatedDurationSeconds <= 0) {
                resetTimer()
                return@launch
            }

            // Find bound target if any to determine Subject & Type
            var targetSubject = "General"
            var targetType = "Self Study"
            val targetId = boundTargetId

            if (targetId != null) {
                val t = repository.getTargetById(targetId)
                if (t != null) {
                    targetSubject = t.subject
                    targetType = t.type
                    // Increment the logged time on the associated target, update status if appropriate
                    repository.incrementLoggedDuration(targetId, calculatedDurationSeconds)
                    if (t.status == "not-started") {
                        repository.updateTargetStatus(targetId, "in-progress")
                    }
                }
            }

            // Append stopwatch checkpoints list to active session comments
            var finalizedNotes = timerNotes
            if (timerMode == "Stopwatch" && stopwatchCheckpoints.isNotEmpty()) {
                val checkpointSummary = stopwatchCheckpoints.joinToString("\n") { cp ->
                    "· ${cp.label}: ${cp.timeFormatted} (Attempt: ${cp.splitFormatted})"
                }
                finalizedNotes = if (finalizedNotes.trim().isNotEmpty()) {
                    "$finalizedNotes\n\nCheckpoints:\n$checkpointSummary"
                } else {
                    "Checkpoints:\n$checkpointSummary"
                }
            }

            // Add new study session log record
            val newSession = StudySession(
                id = UUID.randomUUID().toString(),
                startTime = startISO,
                endTime = endISO,
                durationSeconds = calculatedDurationSeconds,
                subject = targetSubject,
                type = targetType,
                associatedTargetId = targetId,
                notes = finalizedNotes.trim().ifEmpty { null }
            )

            repository.insertSession(newSession)
            resetTimer()
        }
    }

    // --- ACTIONS FOR TARGETS ---
    fun addDailyTarget(
        title: String,
        subject: String,
        type: String,
        targetDate: String,
        durationProposed: Int,
        questionsCount: Int? = null,
        chapter: String? = null,
        lectureNumber: String? = null,
        batch: String? = null,
        autoAddConfig: AutoAddConfig? = null
    ) {
        viewModelScope.launch {
            val dateStr = normalizeTargetDate(targetDate) ?: return@launch

            val newTarget = DailyTarget(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                subject = subject,
                type = type,
                batch = batch?.trim()?.ifEmpty { null } ?: "Regular Batch",
                status = "not-started",
                durationProposed = durationProposed,
                durationLogged = 0,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()),
                targetDate = dateStr,
                dppQuestionsCount = questionsCount,
                dppSolvedCount = if (questionsCount != null) 0 else null,
                dppCorrectCount = if (questionsCount != null) 0 else null,
                chapter = chapter?.trim()?.ifEmpty { null },
                lectureNumber = lectureNumber?.trim()?.ifEmpty { null }
            )
            repository.insertTarget(newTarget)
            if (autoAddConfig != null) {
                insertAutoTargets(newTarget, autoAddConfig)
            }
        }
    }

    fun toggleTargetCompletion(target: DailyTarget) {
        viewModelScope.launch {
            val newStatus = if (target.status == "completed") "not-started" else "completed"
            repository.updateTargetStatus(target.id, newStatus)
            if (newStatus == "completed") {
                if (isHistoryEnabled) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
                    val notesDetail = "Target Achieved: ${target.title}\n" +
                            "Chapter: ${target.chapter ?: "N/A"}\n" +
                            "Batch: ${target.batch ?: "N/A"}\n" +
                            "Lecture Number: ${target.lectureNumber ?: "N/A"}\n" +
                            "Target Date: ${target.targetDate}"
                    val completedSession = StudySession(
                        id = UUID.randomUUID().toString(),
                        startTime = nowStr,
                        endTime = nowStr,
                        durationSeconds = if (target.durationLogged > 0) target.durationLogged else target.durationProposed * 60,
                        subject = target.subject,
                        type = "Target: ${target.type}",
                        associatedTargetId = target.id,
                        notes = notesDetail
                    )
                    repository.insertSession(completedSession)
                }
            } else {
                repository.deleteSessionsByTargetId(target.id)
            }
        }
    }

    fun logTargetTimeManually(targetId: String, minutes: Int) {
        viewModelScope.launch {
            repository.incrementLoggedDuration(targetId, minutes * 60)
            repository.updateTargetStatus(targetId, "completed")
            if (isHistoryEnabled) {
                val target = repository.getTargetById(targetId)
                if (target != null) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
                    val notesDetail = "Target Achieved: ${target.title}\n" +
                            "Chapter: ${target.chapter ?: "N/A"}\n" +
                            "Batch: ${target.batch ?: "N/A"}\n" +
                            "Lecture Number: ${target.lectureNumber ?: "N/A"}\n" +
                            "Target Date: ${target.targetDate}"
                    val completedSession = StudySession(
                        id = UUID.randomUUID().toString(),
                        startTime = nowStr,
                        endTime = nowStr,
                        durationSeconds = minutes * 60,
                        subject = target.subject,
                        type = "Target: ${target.type}",
                        associatedTargetId = target.id,
                        notes = notesDetail
                    )
                    repository.insertSession(completedSession)
                }
            }
        }
    }

    fun deleteTarget(id: String) {
        viewModelScope.launch {
            repository.deleteTargetById(id)
        }
    }

    // --- ACTIONS FOR BACKLOGS ---
    var customSubjects = mutableStateOf(setOf<String>())
        private set

    fun addCustomSubject(subject: String) {
        val trimmed = subject.trim()
        if (trimmed.isNotEmpty()) {
            customSubjects.value = customSubjects.value + trimmed
        }
    }

    fun addBacklog(
        title: String,
        subject: String,
        type: String,
        difficulty: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val item = BacklogItem(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                subject = subject,
                type = type,
                difficulty = difficulty,
                notes = notes?.trim()?.ifEmpty { null },
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()),
                status = "pending"
            )
            repository.insertBacklog(item)
        }
    }

    fun resolveBacklog(item: BacklogItem) {
        viewModelScope.launch {
            val newStatus = if (item.status == "resolved") "pending" else "resolved"
            repository.updateBacklogStatus(item.id, newStatus)
        }
    }

    fun convertBacklogToTarget(item: BacklogItem) {
        viewModelScope.launch {
            // Remove from backlight items db
            repository.deleteBacklogById(item.id)

            // Spawn target
            val targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newTarget = DailyTarget(
                id = UUID.randomUUID().toString(),
                title = "Resolve: " + item.title,
                subject = item.subject,
                type = item.type,
                batch = "Backlog Recovery",
                status = "not-started",
                durationProposed = 45, // default
                durationLogged = 0,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()),
                targetDate = targetDate
            )
            repository.insertTarget(newTarget)

            // Switch Tab to targets
            switchTab("targets")
            setTargetFilter("today")
        }
    }

    fun deleteBacklog(id: String) {
        viewModelScope.launch {
            repository.deleteBacklogById(id)
        }
    }

    // --- ACTIONS FOR DPP LOGS ---
    fun addDPPLog(
        title: String,
        subject: String,
        totalQuestions: Int,
        attempted: Int,
        correct: Int,
        minutes: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            val dppLog = DPPHistoryLog(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                subject = subject,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                totalQuestions = totalQuestions,
                attempted = attempted,
                correct = correct,
                timeSpentMinutes = minutes,
                notes = notes?.trim()?.ifEmpty { null }
            )
            repository.insertDPPLog(dppLog)

            // Optionally auto-add custom DPP as solved in DailyTarget if matched by name
        }
    }

    fun deleteDPPLog(id: String) {
        viewModelScope.launch {
            repository.deleteDPPLogById(id)
        }
    }

    // --- ACTIONS FOR MANUAL STUDY LOGGING OVERRIDE ---
    fun addManualStudyLog(
        title: String,
        subject: String,
        type: String,
        minutes: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
            val newSession = StudySession(
                id = UUID.randomUUID().toString(),
                startTime = nowStr,
                endTime = nowStr,
                durationSeconds = minutes * 60,
                subject = subject,
                type = type,
                associatedTargetId = null,
                notes = notes?.trim()?.ifEmpty { null }
            )
            repository.insertSession(newSession)
        }
    }

    fun deleteSessionLog(id: String) {
        viewModelScope.launch {
            repository.deleteSessionById(id)
        }
    }

    // --- ACTIONS FOR DAILY ASPIRATIONS ---
    fun addAspiration(text: String) {
        viewModelScope.launch {
            if (text.trim().isNotEmpty()) {
                val newAspVal = DailyAspiration(
                    id = UUID.randomUUID().toString(),
                    text = text.trim(),
                    isCompleted = false
                )
                repository.insertAspiration(newAspVal)
            }
        }
    }

    fun toggleAspiration(aspiration: DailyAspiration) {
        viewModelScope.launch {
            val updated = aspiration.copy(isCompleted = !aspiration.isCompleted)
            repository.updateAspiration(updated)
        }
    }

    fun deleteAspiration(id: String) {
        viewModelScope.launch {
            repository.deleteAspirationById(id)
        }
    }

    // --- TONE GENERATION AUDIO SYNTH ---
    private fun playWarningTone() {
        if (!alarmAlertsEnabled) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 80)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                delay(300)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("AudioSynth", "Error warning beep", e)
            }
        }
    }

    private fun playCompletionTone() {
        if (!alarmAlertsEnabled) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                delay(500)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("AudioSynth", "Error completion beep", e)
            }
        }
    }

    private fun playIntervalBeep() {
        if (!alarmAlertsEnabled) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 250) // Sharp, clean beep tone
                delay(300)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("AudioSynth", "Error interval beep", e)
            }
        }

        private fun startTodayDateUpdater() {
            viewModelScope.launch {
                while (true) {
                    todayDate = currentDateString()
                    delay(calculateDelayToNextDay())
                }
            }
        }

        private fun currentDateString(): String {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            return format.format(Date())
        }

        private fun normalizeTargetDate(targetDate: String): String? {
            val trimmed = targetDate.trim()
            if (trimmed.isEmpty()) {
                return todayDate
            }
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            return try {
                val parsed = format.parse(trimmed) ?: return null
                format.format(parsed)
            } catch (e: Exception) {
                null
            }
        }

        private fun parseStrictDate(dateStr: String): Date? {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            return try {
                format.parse(dateStr)
            } catch (e: Exception) {
                null
            }
        }

        private fun calculateDelayToNextDay(): Long {
            val now = Calendar.getInstance()
            val next = now.clone() as Calendar
            next.add(Calendar.DAY_OF_YEAR, 1)
            next.set(Calendar.HOUR_OF_DAY, 0)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 5)
            next.set(Calendar.MILLISECOND, 0)
            val delayMillis = next.timeInMillis - now.timeInMillis
            return if (delayMillis < 60_000L) 60_000L else delayMillis
        }

        private suspend fun insertAutoTargets(baseTarget: DailyTarget, config: AutoAddConfig) {
            if (config.maxLectureNumber == null && config.endDate == null) return
            val lectureText = baseTarget.lectureNumber ?: return
            val lectureInfo = extractNumberInfo(lectureText) ?: return
            val titleInfo = extractNumberInfo(baseTarget.title)
            val baseDate = parseStrictDate(baseTarget.targetDate) ?: return
            val endDate = config.endDate?.let { parseStrictDate(it) }
            var nextNumber = lectureInfo.number + 1
            val calendar = Calendar.getInstance().apply {
                time = baseDate
                add(Calendar.DAY_OF_YEAR, 1)
            }

            while (true) {
                if (config.maxLectureNumber != null && nextNumber > config.maxLectureNumber) break
                if (endDate != null && calendar.time.after(endDate)) break

                val nextLecture = formatWithNumber(lectureInfo, nextNumber)
                val nextTitle = titleInfo?.let { formatWithNumber(it, nextNumber) } ?: baseTarget.title
                val nextTarget = baseTarget.copy(
                    id = UUID.randomUUID().toString(),
                    title = nextTitle,
                    status = "not-started",
                    durationLogged = 0,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()),
                    targetDate = currentDateFormatter().format(calendar.time),
                    lectureNumber = nextLecture,
                    dppSolvedCount = if (baseTarget.dppQuestionsCount != null) 0 else null,
                    dppCorrectCount = if (baseTarget.dppQuestionsCount != null) 0 else null
                )
                repository.insertTarget(nextTarget)
                nextNumber += 1
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        private fun extractNumberInfo(text: String): NumberInfo? {
            val match = "(\\d+)(?!.*\\d)".toRegex().find(text) ?: return null
            val numberStr = match.value
            val number = numberStr.toIntOrNull() ?: return null
            val start = match.range.first
            val end = match.range.last + 1
            return NumberInfo(
                prefix = text.substring(0, start),
                number = number,
                suffix = text.substring(end),
                width = numberStr.length
            )
        }

        private fun formatWithNumber(info: NumberInfo, number: Int): String {
            val padded = number.toString().padStart(info.width, '0')
            return "${info.prefix}$padded${info.suffix}"
        }

        private fun currentDateFormatter(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }
        }
    }
}

class StudyViewModelFactory(
    private val application: Application,
    private val repository: StudyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class StopwatchCheckpoint(
    val id: String,
    val questionNumber: Int,
    val elapsedSeconds: Int,
    val splitSeconds: Int,
    val timeFormatted: String,
    val splitFormatted: String,
    val label: String
)

data class TargetTemplate(
    val id: String,
    val subject: String,
    val batch: String,
    val type: String,
    val chapter: String,
    val durationMinutes: Int
)

data class DppPreset(
    val id: String,
    val title: String,
    val subject: String,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val durationMinutes: Int
)

data class AutoAddConfig(
    val maxLectureNumber: Int?,
    val endDate: String?
)

private data class NumberInfo(
    val prefix: String,
    val number: Int,
    val suffix: String,
    val width: Int
)
