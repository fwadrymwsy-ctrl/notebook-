package dz.arabic.konnachetaleb.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dz.arabic.konnachetaleb.KonnachApp
import dz.arabic.konnachetaleb.alarm.ClassAlarmScheduler
import dz.arabic.konnachetaleb.backup.BackupManager
import dz.arabic.konnachetaleb.backup.BackupRestoreResult
import dz.arabic.konnachetaleb.data.AppPreferences
import dz.arabic.konnachetaleb.data.BlockListStyle
import dz.arabic.konnachetaleb.data.LessonBlockEntity
import dz.arabic.konnachetaleb.data.LessonEntity
import dz.arabic.konnachetaleb.data.PenColor
import dz.arabic.konnachetaleb.data.SearchHitItem
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.data.StarredHighlightItem
import dz.arabic.konnachetaleb.data.SubjectEntity
import dz.arabic.konnachetaleb.data.TaskEntity
import dz.arabic.konnachetaleb.data.ThemeMode
import dz.arabic.konnachetaleb.data.TimetableSlotEntity
import dz.arabic.konnachetaleb.domain.ActiveClassMatch
import dz.arabic.konnachetaleb.domain.TimetableHelper
import dz.arabic.konnachetaleb.pdf.LessonPdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

class KonnachViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KonnachApp
    private val db = app.database
    private val repo = app.repository
    private val settingsStore = app.settingsStore

    val preferences: StateFlow<AppPreferences> = settingsStore.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferences()
    )

    val subjects: StateFlow<List<SubjectEntity>> = repo.observeSubjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val timetableSlots: StateFlow<List<TimetableSlotEntity>> = repo.observeTimetable().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val allHighlights: StateFlow<List<StarredHighlightItem>> = repo.observeAllHighlights().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val nextClassMatch: StateFlow<ActiveClassMatch?> = combine(
        timetableSlots,
        subjects
    ) { slots, subjList ->
        TimetableHelper.findNextUpcomingClass(
            now = LocalDateTime.now(),
            slots = slots,
            subjectsById = subjList.associateBy { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchResults = MutableStateFlow<List<SearchHitItem>>(emptyList())
    val searchResults: StateFlow<List<SearchHitItem>> = _searchResults.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Keep alarms synchronized whenever timetable, subjects, or alarm setting changes
        viewModelScope.launch {
            combine(timetableSlots, subjects, preferences) { slots, subjList, prefs ->
                Triple(slots, subjList, prefs.classAlarmEnabled)
            }.collect { (slots, subjList, enabled) ->
                ClassAlarmScheduler.rescheduleAllSlots(app, slots, subjList, enabled)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun completeOnboardingAndLoadTrack(
        cycle: String,
        year: String,
        specialty: String,
        semester: String,
        replaceExistingForSemester: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveAcademicProfile(cycle, year, specialty, semester)
            repo.loadCatalogSubjects(cycle, year, specialty, semester, replaceExistingForSemester)
        }
    }

    fun findAutoOpenSubjectId(): String? {
        val prefs = preferences.value
        if (!prefs.onboardingDone || !prefs.autoOpenCurrentClass) return null
        val match = TimetableHelper.findOngoingOrSoonClass(
            now = LocalDateTime.now(),
            slots = timetableSlots.value,
            subjectsById = subjects.value.associateBy { it.id },
            lookaheadMinutes = 10L
        )
        return match?.subject?.id
    }

    fun observeLessons(subjectId: String): Flow<List<LessonEntity>> = repo.observeLessons(subjectId)
    fun observeBlocks(lessonId: String): Flow<List<LessonBlockEntity>> = repo.observeBlocks(lessonId)
    fun observeTasks(subjectId: String): Flow<List<TaskEntity>> = repo.observeTasks(subjectId)
    fun observeSubjectHighlights(subjectId: String): Flow<List<StarredHighlightItem>> =
        repo.observeSubjectHighlights(subjectId)

    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.saveSubject(subject) }
    }

    fun addCustomSubject(
        name: String,
        semester: String,
        nature: String,
        professorName: String,
        coefficient: String,
        credits: String,
        colorHex: Long,
        iconKey: String
    ) {
        val prefs = preferences.value
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveSubject(
                SubjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    semester = semester.trim().ifBlank { prefs.semester },
                    cycle = prefs.cycle,
                    year = prefs.year,
                    specialty = prefs.specialty,
                    nature = nature,
                    professorName = professorName.trim(),
                    coefficient = coefficient.trim().ifBlank { "2" },
                    credits = credits.trim().ifBlank { "4" },
                    colorHex = colorHex,
                    iconKey = iconKey,
                    sortOrder = subjects.value.size
                )
            )
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteSubject(subject) }
    }

    fun createLesson(
        subjectId: String,
        sessionType: SessionType,
        title: String,
        onCreated: (LessonEntity) -> Unit
    ) {
        viewModelScope.launch {
            val lesson = withContext(Dispatchers.IO) {
                repo.createLesson(subjectId, sessionType, title)
            }
            onCreated(lesson)
        }
    }

    fun updateLesson(lesson: LessonEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.updateLesson(lesson) }
    }

    fun deleteLesson(lesson: LessonEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteLesson(lesson) }
    }

    fun upsertBlock(block: LessonBlockEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.upsertBlock(block) }
    }

    fun deleteBlock(block: LessonBlockEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteBlock(block) }
    }

    fun addTextBlock(
        lessonId: String,
        penColor: PenColor,
        isBold: Boolean,
        isUnderline: Boolean,
        listStyle: BlockListStyle
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addTextBlock(lessonId, penColor, isBold, isUnderline, listStyle)
        }
    }

    fun addImageBlock(lessonId: String, imageFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addImageBlock(lessonId, imageFileName)
        }
    }

    fun saveTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.saveTimetableSlot(slot) }
    }

    fun deleteTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ClassAlarmScheduler.cancelSlot(app, slot.id)
            repo.deleteTimetableSlot(slot)
        }
    }

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.saveTask(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteTask(task) }
    }

    fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = repo.search(trimmed)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setAutoOpenCurrentClass(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoOpenCurrentClass(enabled) }
    }

    fun setClassAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setClassAlarmEnabled(enabled) }
    }

    fun exportLessonPdf(
        subject: SubjectEntity,
        lesson: LessonEntity,
        blocks: List<LessonBlockEntity>,
        onReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                LessonPdfExporter.exportLessonToPdf(app, subject, lesson, blocks)
            }
            onReady(file)
        }
    }

    fun exportBackupToUri(uri: Uri, includeMedia: Boolean) {
        viewModelScope.launch {
            val result: BackupRestoreResult = withContext(Dispatchers.IO) {
                BackupManager.exportToUri(app, db, uri, preferences.value.specialty, includeMedia)
            }
            if (result.success) settingsStore.recordBackupNow()
            _statusMessage.value = result.message
        }
    }

    fun createShareableBackup(includeMedia: Boolean, onReady: (File) -> Unit) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                BackupManager.exportToShareableFile(app, db, preferences.value.specialty, includeMedia)
            }
            settingsStore.recordBackupNow()
            onReady(file)
        }
    }

    fun restoreBackupFromUri(uri: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupManager.restoreFromUri(app, db, uri, replaceAll)
            }
            _statusMessage.value = result.message
        }
    }
}
