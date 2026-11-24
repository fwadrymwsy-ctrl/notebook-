package dz.arabic.konnachetaleb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dz.arabic.konnachetaleb.ui.screens.GlobalHighlightsScreen
import dz.arabic.konnachetaleb.ui.screens.GlobalSearchScreen
import dz.arabic.konnachetaleb.ui.screens.HomeNotebooksScreen
import dz.arabic.konnachetaleb.ui.screens.LessonEditorScreen
import dz.arabic.konnachetaleb.ui.screens.OnboardingScreen
import dz.arabic.konnachetaleb.ui.screens.SettingsScreen
import dz.arabic.konnachetaleb.ui.screens.SubjectDetailScreen
import dz.arabic.konnachetaleb.ui.screens.TimetableScreen

private data class BottomTab(val label: String, val icon: ImageVector)

@Composable
fun KonnachNavHost(
    viewModel: KonnachViewModel,
    initialSubjectIdFromNotification: String?
) {
    val prefs by viewModel.preferences.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val slots by viewModel.timetableSlots.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activeSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTrackSelector by rememberSaveable { mutableStateOf(false) }
    var autoOpenAttempted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Handle direct jump from 5-minute class notification
    LaunchedEffect(initialSubjectIdFromNotification) {
        if (!initialSubjectIdFromNotification.isNullOrBlank()) {
            activeLessonId = null
            activeSubjectId = initialSubjectIdFromNotification
            autoOpenAttempted = true
        }
    }

    // Smart Auto-Open: open active or starting-in-10-min subject notebook automatically on launch
    LaunchedEffect(prefs.onboardingDone, subjects, slots) {
        if (!autoOpenAttempted && prefs.onboardingDone && subjects.isNotEmpty() && slots.isNotEmpty()) {
            val autoSubject = viewModel.findAutoOpenSubjectId()
            if (autoSubject != null) {
                activeSubjectId = autoSubject
            }
            autoOpenAttempted = true
        }
    }

    if (!prefs.onboardingDone || showTrackSelector) {
        OnboardingScreen(
            initialPrefs = prefs,
            isFirstLaunch = !prefs.onboardingDone,
            onConfirm = { cycle, year, specialty, semester, replace ->
                viewModel.completeOnboardingAndLoadTrack(cycle, year, specialty, semester, replace)
                showTrackSelector = false
            },
            onCancel = if (prefs.onboardingDone) { { showTrackSelector = false } } else null
        )
        return
    }

    if (activeLessonId != null && activeSubjectId != null) {
        BackHandler { activeLessonId = null }
        LessonEditorScreen(
            subjectId = activeSubjectId!!,
            lessonId = activeLessonId!!,
            viewModel = viewModel,
            onBack = { activeLessonId = null }
        )
        return
    }

    if (activeSubjectId != null) {
        BackHandler { activeSubjectId = null }
        SubjectDetailScreen(
            subjectId = activeSubjectId!!,
            viewModel = viewModel,
            onBack = { activeSubjectId = null },
            onOpenLesson = { lessonId -> activeLessonId = lessonId }
        )
        return
    }

    val tabs = listOf(
        BottomTab("الكراريس", Icons.Default.Book),
        BottomTab("التوقيت", Icons.Default.Schedule),
        BottomTab("للامتحان", Icons.Default.Star),
        BottomTab("بحث", Icons.Default.Search),
        BottomTab("الإعدادات", Icons.Default.Settings)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { idx, tab ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeNotebooksScreen(
                    viewModel = viewModel,
                    onOpenSubject = { subjId -> activeSubjectId = subjId },
                    onGoToBackupSettings = { selectedTab = 4 }
                )
                1 -> TimetableScreen(
                    viewModel = viewModel,
                    onOpenSubject = { subjId -> activeSubjectId = subjId }
                )
                2 -> GlobalHighlightsScreen(
                    viewModel = viewModel,
                    onJumpToLesson = { subjId, lessonId ->
                        activeSubjectId = subjId
                        activeLessonId = lessonId
                    }
                )
                3 -> GlobalSearchScreen(
                    viewModel = viewModel,
                    onJumpToLesson = { subjId, lessonId ->
                        activeSubjectId = subjId
                        activeLessonId = lessonId
                    }
                )
                4 -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenTrackSelector = { showTrackSelector = true }
                )
            }
        }
    }
}
