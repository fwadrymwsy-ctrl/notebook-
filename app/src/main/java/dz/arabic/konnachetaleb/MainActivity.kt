package dz.arabic.konnachetaleb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.arabic.konnachetaleb.data.ThemeMode
import dz.arabic.konnachetaleb.ui.KonnachNavHost
import dz.arabic.konnachetaleb.ui.KonnachViewModel
import dz.arabic.konnachetaleb.ui.theme.KonnachTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SUBJECT_ID = "extra_open_subject_id"
    }

    private var pendingSubjectFromIntent: String? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* User choice respected */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingSubjectFromIntent = intent?.getStringExtra(EXTRA_OPEN_SUBJECT_ID)
        requestNotificationPermissionIfNeeded()

        setContent {
            val vm: KonnachViewModel = viewModel()
            val prefs by vm.preferences.collectAsState()

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KonnachTheme(darkTheme = darkTheme) {
                    KonnachNavHost(
                        viewModel = vm,
                        initialSubjectIdFromNotification = pendingSubjectFromIntent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingSubjectFromIntent = intent.getStringExtra(EXTRA_OPEN_SUBJECT_ID)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
