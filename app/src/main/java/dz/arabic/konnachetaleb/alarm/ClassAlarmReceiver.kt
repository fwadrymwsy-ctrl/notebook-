package dz.arabic.konnachetaleb.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dz.arabic.konnachetaleb.MainActivity
import dz.arabic.konnachetaleb.R
import dz.arabic.konnachetaleb.data.AppDatabase
import dz.arabic.konnachetaleb.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ClassAlarmScheduler.ACTION_CLASS_REMINDER) return

        val subjectId = intent.getStringExtra(ClassAlarmScheduler.EXTRA_SUBJECT_ID) ?: ""
        val subjectName = intent.getStringExtra(ClassAlarmScheduler.EXTRA_SUBJECT_NAME) ?: "حصة جامعية"
        val sessionType = intent.getStringExtra(ClassAlarmScheduler.EXTRA_SESSION_TYPE) ?: "محاضرة"
        val room = intent.getStringExtra(ClassAlarmScheduler.EXTRA_ROOM)?.trim().orEmpty()
        val startTime = intent.getStringExtra(ClassAlarmScheduler.EXTRA_START_TIME) ?: ""

        ClassAlarmScheduler.createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_SUBJECT_ID, subjectId)
        }
        val openPending = PendingIntent.getActivity(
            context,
            subjectId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomPart = if (room.isNotBlank()) " — القاعة/المدرج: $room" else ""
        val body = "$sessionType تبدأ الساعة $startTime$roomPart"

        val notification = NotificationCompat.Builder(context, ClassAlarmScheduler.CHANNEL_CLASS_REMINDER)
            .setSmallIcon(R.drawable.ic_notification_book)
            .setContentTitle("بعد 5 دقائق: $subjectName")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(6000 + (subjectId.hashCode() and 0x3FF), notification)

        // Reschedule for next week automatically
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
