package dz.arabic.konnachetaleb.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.konnachPrefs by preferencesDataStore(name = "konnach_etaleb_prefs")

data class AppPreferences(
    val onboardingDone: Boolean = false,
    val cycle: String = "ليسانس",
    val year: String = "L1",
    val specialty: String = "جذع مشترك",
    val semester: String = "S1",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoOpenCurrentClass: Boolean = true,
    val classAlarmEnabled: Boolean = true,
    val lastBackupEpochMs: Long = 0L
) {
    fun shouldRemindBackup(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!onboardingDone) return false
        if (lastBackupEpochMs == 0L) return false
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        return (nowMs - lastBackupEpochMs) > thirtyDaysMs
    }
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CYCLE = stringPreferencesKey("cycle")
        val YEAR = stringPreferencesKey("year")
        val SPECIALTY = stringPreferencesKey("specialty")
        val SEMESTER = stringPreferencesKey("semester")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_OPEN_CLASS = booleanPreferencesKey("auto_open_class")
        val CLASS_ALARM_ENABLED = booleanPreferencesKey("class_alarm_enabled")
        val LAST_BACKUP_MS = longPreferencesKey("last_backup_ms")
    }

    val preferencesFlow: Flow<AppPreferences> = context.konnachPrefs.data.map { p ->
        AppPreferences(
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            cycle = p[Keys.CYCLE] ?: "ليسانس",
            year = p[Keys.YEAR] ?: "L1",
            specialty = p[Keys.SPECIALTY] ?: "جذع مشترك",
            semester = p[Keys.SEMESTER] ?: "S1",
            themeMode = ThemeMode.fromKey(p[Keys.THEME_MODE]),
            autoOpenCurrentClass = p[Keys.AUTO_OPEN_CLASS] ?: true,
            classAlarmEnabled = p[Keys.CLASS_ALARM_ENABLED] ?: true,
            lastBackupEpochMs = p[Keys.LAST_BACKUP_MS] ?: 0L
        )
    }

    suspend fun current(): AppPreferences = preferencesFlow.first()

    suspend fun saveAcademicProfile(
        cycle: String,
        year: String,
        specialty: String,
        semester: String
    ) {
        context.konnachPrefs.edit {
            it[Keys.ONBOARDING_DONE] = true
            it[Keys.CYCLE] = cycle
            it[Keys.YEAR] = year
            it[Keys.SPECIALTY] = specialty
            it[Keys.SEMESTER] = semester
            if ((it[Keys.LAST_BACKUP_MS] ?: 0L) == 0L) {
                it[Keys.LAST_BACKUP_MS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.konnachPrefs.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setAutoOpenCurrentClass(enabled: Boolean) {
        context.konnachPrefs.edit { it[Keys.AUTO_OPEN_CLASS] = enabled }
    }

    suspend fun setClassAlarmEnabled(enabled: Boolean) {
        context.konnachPrefs.edit { it[Keys.CLASS_ALARM_ENABLED] = enabled }
    }

    suspend fun recordBackupNow() {
        context.konnachPrefs.edit { it[Keys.LAST_BACKUP_MS] = System.currentTimeMillis() }
    }
}
