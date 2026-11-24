package dz.arabic.konnachetaleb.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.data.SubjectEntity
import dz.arabic.konnachetaleb.data.TimetableSlotEntity
import dz.arabic.konnachetaleb.domain.TimetableHelper
import dz.arabic.konnachetaleb.ui.KonnachViewModel
import java.util.UUID

@Composable
fun TimetableScreen(
    viewModel: KonnachViewModel,
    onOpenSubject: (String) -> Unit
) {
    val slots by viewModel.timetableSlots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlotEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة حصة")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "استعمال الزمن الأسبوعي",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "يُنبهك التطبيق قبل كل حصة بـ 5 دقائق ويفتح كراسها تلقائياً عند دخول وقتها.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TimetableHelper.universityDays.forEach { (dayIso, dayLabel) ->
                val daySlots = slots.filter { it.dayOfWeek == dayIso }.sortedBy { it.startTime }
                item(key = "header_$dayIso") {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (daySlots.isEmpty()) {
                    item(key = "empty_$dayIso") {
                        Text(
                            text = "لا توجد حصص مبرمجة يوم $dayLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                } else {
                    items(daySlots, key = { it.id }) { slot ->
                        val subject = subjectsById[slot.subjectId]
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { subject?.let { onOpenSubject(it.id) } }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject?.name ?: "مقياس محذوف",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val roomStr = if (slot.room.isNotBlank()) " • القاعة: ${slot.room}" else ""
                                    Text(
                                        text = "${slot.startTime} – ${slot.endTime} | ${SessionType.fromCode(slot.sessionType).arabicLabel}$roomStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingSlot = slot }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                    }
                                    IconButton(onClick = { viewModel.deleteTimetableSlot(slot) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }

    if (showAddDialog && subjects.isNotEmpty()) {
        TimetableSlotDialog(
            initial = null,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { slot ->
                viewModel.saveTimetableSlot(slot)
                showAddDialog = false
            }
        )
    }

    editingSlot?.let { slot ->
        TimetableSlotDialog(
            initial = slot,
            subjects = subjects,
            onDismiss = { editingSlot = null },
            onSave = { updated ->
                viewModel.saveTimetableSlot(updated)
                editingSlot = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableSlotDialog(
    initial: TimetableSlotEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (TimetableSlotEntity) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(initial?.dayOfWeek ?: TimetableHelper.universityDays.first().first) }
    var selectedSubjectId by remember { mutableStateOf(initial?.subjectId ?: subjects.first().id) }
    var selectedType by remember { mutableStateOf(SessionType.fromCode(initial?.sessionType)) }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "08:30") }
    var endTime by remember { mutableStateOf(initial?.endTime ?: "10:00") }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    val selectedSubjectName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: subjects.first().name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة حصة في استعمال الزمن" else "تعديل الحصة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اليوم:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimetableHelper.universityDays.forEach { (iso, label) ->
                        FilterChip(
                            selected = selectedDay == iso,
                            onClick = { selectedDay = iso },
                            label = { Text(label) }
                        )
                    }
                }

                Text("المقياس:", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(
                    onClick = { subjectMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedSubjectName, maxLines = 1)
                }
                DropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    subjects.forEach { subj ->
                        DropdownMenuItem(
                            text = { Text("${subj.name} (${subj.semester})") },
                            onClick = {
                                selectedSubjectId = subj.id
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }

                Text("نوع الحصة:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SessionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.code) }
                        )
                    }
                }

                Text("فترات التوقيت الجامعية الرسمية (اختيار سريع):", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimetableHelper.officialTimePresets.forEach { preset ->
                        FilterChip(
                            selected = startTime == preset.start && endTime == preset.end,
                            onClick = {
                                startTime = preset.start
                                endTime = preset.end
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("البداية (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("النهاية (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("رقم المدرج / القاعة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (TimetableHelper.parseTimeOrNull(startTime) != null &&
                    TimetableHelper.parseTimeOrNull(endTime) != null
                ) {
                    onSave(
                        TimetableSlotEntity(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            subjectId = selectedSubjectId,
                            dayOfWeek = selectedDay,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            sessionType = selectedType.code,
                            room = room.trim()
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
