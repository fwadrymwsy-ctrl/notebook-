package dz.arabic.konnachetaleb.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dz.arabic.konnachetaleb.data.AppDatabase
import dz.arabic.konnachetaleb.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val allowed = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        if (action !in allowed) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val prefs = SettingsStore(context).current()
                ClassAlarmScheduler.rescheduleAllSlots(
                    context = context,
                    slots = db.timetableDao().getAll(),
                    subjects = db.subjectDao().getAll(),
                    alarmsEnabled = prefs.classAlarmEnabled
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
