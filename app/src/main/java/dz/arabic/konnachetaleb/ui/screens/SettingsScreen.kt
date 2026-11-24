package dz.arabic.konnachetaleb.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dz.arabic.konnachetaleb.alarm.ClassAlarmScheduler
import dz.arabic.konnachetaleb.backup.BackupManager
import dz.arabic.konnachetaleb.data.ThemeMode
import dz.arabic.konnachetaleb.media.MediaStorageHelper
import dz.arabic.konnachetaleb.ui.KonnachViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: KonnachViewModel,
    onOpenTrackSelector: () -> Unit
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsState()

    var pendingBackupIncludeMedia by remember { mutableStateOf(true) }
    var pendingRestoreReplaceAll by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri, pendingBackupIncludeMedia)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(uri, pendingRestoreReplaceAll)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "الإعدادات والنسخ الاحتياطي",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 1. Academic Track
        item {
            SettingsGroupCard("المسار الأكاديمي (لغة وأدب عربي LMD)") {
                Text(
                    text = "${prefs.cycle} ${prefs.year} — ${prefs.specialty} (${prefs.semester})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenTrackSelector,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تغيير الطور أو التخصص أو تحميل سداسي جديد")
                }
            }
        }

        // 2. Theme Mode
        item {
            SettingsGroupCard("المظهر") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.arabicLabel) }
                        )
                    }
                }
            }
        }

        // 3. Smart Auto-Open & 5-min Pre-Class Alarm
        item {
            SettingsGroupCard("الفتح التلقائي ومنبه الحصص") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الفتح التلقائي لكراس الحصة الجارية", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "عند فتح التطبيق أثناء حصة أو قبلها بـ 10 دقائق، يفتح كراس المقياس مباشرة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.autoOpenCurrentClass,
                        onCheckedChange = { viewModel.setAutoOpenCurrentClass(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("منبه الحصص قبل 5 دقائق", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "إشعار أسبوعي دقيق باسم المقياس والقاعة قبل 5 دقائق من بداية كل حصة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.classAlarmEnabled,
                        onCheckedChange = { viewModel.setClassAlarmEnabled(it) }
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !ClassAlarmScheduler.canScheduleExactAlarms(context)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("منح صلاحية المنبهات الدقيقة (لضمان التنبيه قبل 5 دقائق)")
                    }
                }
            }
        }

        // 4. Offline ZIP Backup & Restore (SAF)
        item {
            SettingsGroupCard("النسخ الاحتياطي والاسترجاع (ملف ZIP بدون إنترنت)") {
                Text(
                    text = "إنشاء نسخة احتياطية محفوظة في هاتفك:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            pendingBackupIncludeMedia = false
                            createBackupLauncher.launch(
                                BackupManager.suggestedFileName(prefs.specialty, includeMedia = false)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("النصوص والبيانات فقط")
                    }
                    Button(
                        onClick = {
                            pendingBackupIncludeMedia = true
                            createBackupLauncher.launch(
                                BackupManager.suggestedFileName(prefs.specialty, includeMedia = true)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نسخة شاملة (مع الوسائط)")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.createShareableBackup(includeMedia = true) { zipFile ->
                            val uri = MediaStorageHelper.getShareUri(context, zipFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مشاركة نسخة شاملة فوراً (واتساب / تيليغرام / بريد)")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = "استرجاع نسخة احتياطية (مع فحص تلقائي للسلامة):",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pendingRestoreReplaceAll = false
                            openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("دمج ذكي (Smart Merge)")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingRestoreReplaceAll = true
                            openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("استرجاع كامل")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
