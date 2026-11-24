package dz.arabic.konnachetaleb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dz.arabic.konnachetaleb.data.LessonEntity
import dz.arabic.konnachetaleb.data.PenColor
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.data.TaskEntity
import dz.arabic.konnachetaleb.ui.KonnachViewModel
import dz.arabic.konnachetaleb.ui.theme.StarYellow
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    viewModel: KonnachViewModel,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val subjects by viewModel.subjects.collectAsState()
    val subject = subjects.firstOrNull { it.id == subjectId } ?: return
    val lessons by viewModel.observeLessons(subjectId).collectAsState(initial = emptyList())
    val highlights by viewModel.observeSubjectHighlights(subjectId).collectAsState(initial = emptyList())
    val tasks by viewModel.observeTasks(subjectId).collectAsState(initial = emptyList())

    val tabTitles = listOf("محاضرة (Cours)", "أعمال موجهة (TD)", "أعمال تطبيقية (TP)", "للامتحان ⭐", "المهام")
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showNewLessonDialog by remember { mutableStateOf(false) }
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var deletingLesson by remember { mutableStateOf<LessonEntity?>(null) }

    val subjectColor = Color(subject.colorHex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditInfoDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل بطاقة المقياس", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = subjectColor,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab in 0..2 || selectedTab == 4) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 4) showNewTaskDialog = true
                        else showNewLessonDialog = true
                    },
                    containerColor = subjectColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Editable Subject Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditInfoDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأستاذ: ${subject.professorName.ifBlank { "غير محدد (اضغط للتعديل)" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${subject.nature} • معامل: ${subject.coefficient} | رصيد: ${subject.credits}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0, 1, 2 -> {
                    val targetType = when (selectedTab) {
                        0 -> SessionType.COURS
                        1 -> SessionType.TD
                        else -> SessionType.TP
                    }
                    val filtered = lessons.filter { it.sessionType == targetType.code }

                    if (filtered.isEmpty()) {
                        EmptyHint("لا توجد دروس في قسم ${targetType.arabicLabel} بعد.\nاضغط زر (+) لبدء تدوين أول درس.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { lesson ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .clickable { onOpenLesson(lesson.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "درس #${lesson.sequenceNumber} — ${lesson.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "التاريخ: ${lesson.dateIso}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (!lesson.audioFileName.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.Mic,
                                                        contentDescription = "يحتوي على تسجيل صوتي",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(onClick = { deletingLesson = lesson }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الدرس", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Exam Highlights Tab
                    if (highlights.isEmpty()) {
                        EmptyHint("لا توجد نقاط موسومة بنجمة (⭐) في هذا المقياس بعد.\nفعّل زر النجمة بجانب أي فقرة داخل الدرس لتظهر هنا تلقائياً.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(highlights, key = { it.blockId }) { item ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenLesson(item.lessonId) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${item.lessonTitle} (${item.lessonDate})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(PenColor.fromCode(item.penColor).hexColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // To-Do Tasks Tab
                    if (tasks.isEmpty()) {
                        EmptyHint("لا توجد واجبات أو بحوث مسجلة لهذا المقياس.\nاضغط زر (+) لإضافة بحث أو تمرين.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { checked ->
                                                viewModel.saveTask(task.copy(isCompleted = checked))
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            if (task.dueDateIso.isNotBlank()) {
                                                Text(
                                                    text = "تاريخ التسليم: ${task.dueDateIso}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditInfoDialog) {
        SubjectEditDialog(
            initial = subject,
            defaultSemester = subject.semester,
            onDismiss = { showEditInfoDialog = false },
            onSave = { name, sem, nature, prof, coeff, cred, color, icon ->
                viewModel.saveSubject(
                    subject.copy(
                        name = name,
                        semester = sem,
                        nature = nature,
                        professorName = prof,
                        coefficient = coeff,
                        credits = cred,
                        colorHex = color,
                        iconKey = icon
                    )
                )
                showEditInfoDialog = false
            }
        )
    }

    if (showNewLessonDialog) {
        val defaultType = when (selectedTab) {
            1 -> SessionType.TD
            2 -> SessionType.TP
            else -> SessionType.COURS
        }
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewLessonDialog = false },
            title = { Text("درس جديد (${defaultType.arabicLabel})") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الدرس") },
                    placeholder = { Text("مثلاً: الشعر الجاهلي وخصائصه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNewLessonDialog = false
                    viewModel.createLesson(subject.id, defaultType, title) { created ->
                        onOpenLesson(created.id)
                    }
                }) { Text("فتح الكراس") }
            },
            dismissButton = {
                TextButton(onClick = { showNewLessonDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showNewTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTaskDialog = false },
            title = { Text("مهمة / بحث جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("عنوان البحث أو التمرين") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("تاريخ التسليم (اختياري، مثلاً 2026-12-10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (taskTitle.isNotBlank()) {
                        viewModel.saveTask(
                            TaskEntity(
                                id = UUID.randomUUID().toString(),
                                subjectId = subject.id,
                                title = taskTitle.trim(),
                                dueDateIso = dueDate.trim()
                            )
                        )
                        showNewTaskDialog = false
                    }
                }) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTaskDialog = false }) { Text("إلغاء") }
            }
        )
    }

    deletingLesson?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingLesson = null },
            title = { Text("حذف الدرس") },
            text = { Text("هل تريد حذف «${target.title}» نهائياً؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLesson(target)
                    deletingLesson = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingLesson = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun EmptyHint(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
