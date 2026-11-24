package dz.arabic.konnachetaleb.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.data.SubjectEntity
import dz.arabic.konnachetaleb.data.TimetableSlotEntity
import dz.arabic.konnachetaleb.domain.TimetableHelper
import java.time.LocalDateTime
import java.time.ZoneId

object ClassAlarmScheduler {

    const val CHANNEL_CLASS_REMINDER = "konnach_class_reminder_channel"
    const val ACTION_CLASS_REMINDER = "dz.arabic.konnachetaleb.ACTION_CLASS_REMINDER"

    const val EXTRA_SLOT_ID = "extra_slot_id"
    const val EXTRA_SUBJECT_ID = "extra_subject_id"
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_SESSION_TYPE = "extra_session_type"
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_START_TIME = "extra_start_time"

    fun createNotificationChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_CLASS_REMINDER,
            "منبه الحصص الجامعية (قبل 5 دقائق)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيه محلي أسبوعي قبل بداية كل حصة بـ 5 دقائق"
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun rescheduleAllSlots(
        context: Context,
        slots: List<TimetableSlotEntity>,
        subjects: List<SubjectEntity>,
        alarmsEnabled: Boolean
    ) {
        createNotificationChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val subjectMap = subjects.associateBy { it.id }

        // Cancel existing alarms for all known slots first
        slots.forEach { slot ->
            cancelSlotAlarm(context, am, slot.id)
        }
        if (!alarmsEnabled) return

        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()

        slots.forEach { slot ->
            val subject = subjectMap[slot.subjectId] ?: return@forEach
            val triggerDt = nextWeeklyReminderTime(now, slot) ?: return@forEach
            val triggerMillis = triggerDt.atZone(zone).toInstant().toEpochMilli()

            val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
                action = ACTION_CLASS_REMINDER
                putExtra(EXTRA_SLOT_ID, slot.id)
                putExtra(EXTRA_SUBJECT_ID, subject.id)
                putExtra(EXTRA_SUBJECT_NAME, subject.name)
                putExtra(EXTRA_SESSION_TYPE, SessionType.fromCode(slot.sessionType).arabicLabel)
                putExtra(EXTRA_ROOM, slot.room)
                putExtra(EXTRA_START_TIME, slot.startTime)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                requestCodeFor(slot.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canScheduleExactAlarms(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            }
        }
    }

    fun cancelSlot(context: Context, slotId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelSlotAlarm(context, am, slotId)
    }

    private fun cancelSlotAlarm(context: Context, am: AlarmManager, slotId: String) {
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_CLASS_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCodeFor(slotId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            am.cancel(pending)
            pending.cancel()
        }
    }

    /**
     * Computes the next exact weekly trigger time: 5 minutes before slot.startTime on slot.dayOfWeek.
     */
    private fun nextWeeklyReminderTime(now: LocalDateTime, slot: TimetableSlotEntity): LocalDateTime? {
        val start = TimetableHelper.parseTimeOrNull(slot.startTime) ?: return null
        val reminderTime = start.minusMinutes(5)

        for (dayOffset in 0..7) {
            val date = now.toLocalDate().plusDays(dayOffset.toLong())
            if (date.dayOfWeek.value == slot.dayOfWeek) {
                val candidate = LocalDateTime.of(date, reminderTime)
                if (candidate.isAfter(now)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun requestCodeFor(slotId: String): Int = (slotId.hashCode() and 0x7FFFFFFF)
}
