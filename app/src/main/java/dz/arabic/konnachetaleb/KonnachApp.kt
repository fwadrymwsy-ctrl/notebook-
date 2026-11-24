package dz.arabic.konnachetaleb

import android.app.Application
import dz.arabic.konnachetaleb.alarm.ClassAlarmScheduler
import dz.arabic.konnachetaleb.data.AppDatabase
import dz.arabic.konnachetaleb.data.KonnachRepository
import dz.arabic.konnachetaleb.data.SettingsStore

class KonnachApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: KonnachRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = KonnachRepository(database)
        settingsStore = SettingsStore(this)
        ClassAlarmScheduler.createNotificationChannel(this)
    }
}
