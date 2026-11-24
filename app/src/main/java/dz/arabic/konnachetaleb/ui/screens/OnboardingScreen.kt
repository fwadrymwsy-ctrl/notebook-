package dz.arabic.konnachetaleb.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dz.arabic.konnachetaleb.data.AppPreferences
import dz.arabic.konnachetaleb.data.ArabicLmdCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialPrefs: AppPreferences,
    isFirstLaunch: Boolean,
    onConfirm: (cycle: String, year: String, specialty: String, semester: String, replace: Boolean) -> Unit,
    onCancel: (() -> Unit)?
) {
    var cycle by remember { mutableStateOf(initialPrefs.cycle) }
    var year by remember { mutableStateOf(initialPrefs.year) }
    var specialty by remember { mutableStateOf(initialPrefs.specialty) }
    var semester by remember { mutableStateOf(initialPrefs.semester) }
    var replaceExisting by remember { mutableStateOf(false) }

    val years = ArabicLmdCatalog.yearsForCycle(cycle)
    if (year !in years) year = years.first()

    val specialties = ArabicLmdCatalog.specialtiesFor(cycle, year)
    if (specialty !in specialties) specialty = specialties.first()

    val semesters = ArabicLmdCatalog.semestersFor(cycle, year, specialty)
    if (semester !in semesters) semester = semesters.first()

    val previewSubjects = ArabicLmdCatalog.subjectsFor(cycle, year, specialty, semester)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isFirstLaunch) "مرحباً بك في كناش الطالب" else "تغيير المسار الأكاديمي وتحميل المقاييس",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "قسم اللغة والأدب العربي — كلية الآداب واللغات والفنون — جامعة الجلفة (2026–2027)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionLabel("1. اختر الطور:")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArabicLmdCatalog.cycles().forEach { c ->
                    FilterChip(
                        selected = cycle == c,
                        onClick = { cycle = c },
                        label = { Text(c) }
                    )
                }
            }

            SectionLabel("2. اختر السنة:")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                years.forEach { y ->
                    FilterChip(
                        selected = year == y,
                        onClick = { year = y },
                        label = { Text(y) }
                    )
                }
            }

            SectionLabel("3. اختر التخصص / الشعبة:")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                specialties.forEach { sp ->
                    FilterChip(
                        selected = specialty == sp,
                        onClick = { specialty = sp },
                        label = { Text(sp) }
                    )
                }
            }

            SectionLabel("4. اختر السداسي:")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                semesters.forEachIndexed { idx, sem ->
                    val semLabel = if (idx == 0) "السداسي الأول ($sem)" else "السداسي الثاني ($sem)"
                    FilterChip(
                        selected = semester == sem,
                        onClick = { semester = sem },
                        label = { Text(semLabel) }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (previewSubjects.isEmpty()) {
                        Text(
                            text = "السداسي الزوجي ($semester) جاهز وفارغ لإضافة مقاييسك يدوياً متى شئت.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "المقاييس التي ستُحمّل تلقائياً (${previewSubjects.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        previewSubjects.forEachIndexed { i, item ->
                            Text(
                                text = "${i + 1}. ${item.name} — (${item.nature})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (!isFirstLaunch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = replaceExisting,
                        onCheckedChange = { replaceExisting = it }
                    )
                    Text(
                        text = "استبدال مقاييس السداسي $semester الحالية (احذر: يحذف دروسها)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = { onConfirm(cycle, year, specialty, semester, replaceExisting) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isFirstLaunch) "تحميل المقاييس وفتح الكراريس" else "حفظ وتحميل المقاييس",
                    fontWeight = FontWeight.Bold
                )
            }

            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
