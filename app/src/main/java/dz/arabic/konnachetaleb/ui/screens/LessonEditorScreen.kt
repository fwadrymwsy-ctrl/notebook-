package dz.arabic.konnachetaleb.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import dz.arabic.konnachetaleb.data.BlockListStyle
import dz.arabic.konnachetaleb.data.LessonBlockEntity
import dz.arabic.konnachetaleb.data.PenColor
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.media.MediaStorageHelper
import dz.arabic.konnachetaleb.ui.KonnachViewModel
import dz.arabic.konnachetaleb.ui.theme.StarYellow
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditorScreen(
    subjectId: String,
    lessonId: String,
    viewModel: KonnachViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsState()
    val lessons by viewModel.observeLessons(subjectId).collectAsState(initial = emptyList())
    val blocks by viewModel.observeBlocks(lessonId).collectAsState(initial = emptyList())

    val subject = subjects.firstOrNull { it.id == subjectId } ?: return
    val lesson = lessons.firstOrNull { it.id == lessonId } ?: return

    var titleText by remember(lesson.id) { mutableStateOf(lesson.title) }
    var activePen by remember { mutableStateOf(PenColor.BLACK) }
    var activeBold by remember { mutableStateOf(false) }
    var activeUnderline by remember { mutableStateOf(false) }
    var activeListStyle by remember { mutableStateOf(BlockListStyle.NONE) }
    var fullScreenPhotoFile by remember { mutableStateOf<File?>(null) }

    // Camera capture via FileProvider
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingPhotoFile
        if (success && file != null && file.exists()) {
            viewModel.addImageBlock(lesson.id, file.name)
        }
        pendingPhotoFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = MediaStorageHelper.createPhotoFile(context)
            pendingPhotoFile = file
            val uri = MediaStorageHelper.getShareUri(context, file)
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "يرجى منح إذن الكاميرا لتصوير السبورة", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery image picker via PickVisualMedia
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedName = MediaStorageHelper.copyPickedImageToInternal(context, uri)
            if (savedName != null) {
                viewModel.addImageBlock(lesson.id, savedName)
            }
        }
    }

    // Audio Recorder & Player state
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioDurationMs by remember { mutableIntStateOf(0) }
    var audioPosMs by remember { mutableIntStateOf(0) }

    fun startRecordingAudio() {
        runCatching {
            val file = MediaStorageHelper.createAudioFile(context, lesson.id)
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            currentRecordingFile = file
            isRecording = true
        }.onFailure {
            Toast.makeText(context, "تعذر بدء التسجيل الصوتي", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingAudio() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        isRecording = false
        val file = currentRecordingFile
        if (file != null && file.exists() && file.length() > 0) {
            viewModel.updateLesson(lesson.copy(audioFileName = file.name))
            Toast.makeText(context, "تم حفظ تسجيل شرح الأستاذ في الدرس", Toast.LENGTH_SHORT).show()
        }
        currentRecordingFile = null
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecordingAudio()
        else Toast.makeText(context, "يرجى منح إذن الميكروفون للتسجيل الصوتي", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    audioPosMs = mp.currentPosition
                }
            }
            delay(300L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop(); recorder?.release() }
            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        }
    }

    val subjectColor = Color(subject.colorHex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(subject.name, fontSize = 15.sp, maxLines = 1)
                        Text(
                            text = "درس #${lesson.sequenceNumber} • ${SessionType.fromCode(lesson.sessionType).arabicLabel} • ${lesson.dateIso}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFEF3C7)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportLessonPdf(subject, lesson.copy(title = titleText), blocks) { pdfFile ->
                            val uri = MediaStorageHelper.getShareUri(context, pdfFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو حفظ ملف PDF للدرس"))
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = subjectColor,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Quick Pen, Formatting, Camera, Gallery, and Audio Recorder Bar
            Surface(shadowElevation = 8.dp, tonalElevation = 4.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Colored Pens
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            PenColor.entries.forEach { pen ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(pen.hexColor))
                                        .border(
                                            width = if (activePen == pen) 2.5.dp else 1.dp,
                                            color = if (activePen == pen) MaterialTheme.colorScheme.secondary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { activePen = pen }
                                )
                            }
                        }

                        // Bold, Underline, Bullet, Numbered
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeBold = !activeBold }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Default.FormatBold,
                                    contentDescription = "خط عريض",
                                    tint = if (activeBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { activeUnderline = !activeUnderline }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Default.FormatUnderlined,
                                    contentDescription = "تسطير",
                                    tint = if (activeUnderline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    activeListStyle = if (activeListStyle == BlockListStyle.BULLET) BlockListStyle.NONE else BlockListStyle.BULLET
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "قائمة نقطية",
                                    tint = if (activeListStyle == BlockListStyle.BULLET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    activeListStyle = if (activeListStyle == BlockListStyle.NUMBERED) BlockListStyle.NONE else BlockListStyle.NUMBERED
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatListNumbered,
                                    contentDescription = "قائمة مرقمة",
                                    tint = if (activeListStyle == BlockListStyle.NUMBERED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Add new styled paragraph
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.addTextBlock(lesson.id, activePen, activeBold, activeUnderline, activeListStyle)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فقرة جديدة", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Camera, Gallery, Mic
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    val file = MediaStorageHelper.createPhotoFile(context)
                                    pendingPhotoFile = file
                                    takePictureLauncher.launch(MediaStorageHelper.getShareUri(context, file))
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "تصوير السبورة", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "صورة من المعرض", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = {
                                if (isRecording) {
                                    stopRecordingAudio()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (granted) startRecordingAudio()
                                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "إيقاف التسجيل" else "تسجيل صوت الأستاذ",
                                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Lesson Title Input
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        viewModel.updateLesson(lesson.copy(title = it))
                    },
                    label = { Text("عنوان الدرس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Built-in Audio Player Bar if recording exists or recording is active
            if (isRecording || !lesson.audioFileName.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isRecording) {
                                Text(
                                    text = "جارٍ تسجيل صوت الأستاذ... اضغط زر الإيقاف لحفظه في الدرس.",
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val audioFile = MediaStorageHelper.audioFile(context, lesson.audioFileName!!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        if (isPlaying) {
                                            runCatching { mediaPlayer?.pause() }
                                            isPlaying = false
                                        } else {
                                            if (mediaPlayer == null && audioFile.exists()) {
                                                runCatching {
                                                    val mp = MediaPlayer().apply {
                                                        setDataSource(audioFile.absolutePath)
                                                        prepare()
                                                        setOnCompletionListener {
                                                            isPlaying = false
                                                            audioPosMs = 0
                                                        }
                                                    }
                                                    mediaPlayer = mp
                                                    audioDurationMs = mp.duration.coerceAtLeast(1)
                                                }
                                            }
                                            runCatching {
                                                mediaPlayer?.start()
                                                isPlaying = true
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل أو إيقاف"
                                        )
                                    }

                                    IconButton(onClick = {
                                        mediaPlayer?.let { mp ->
                                            val target = (mp.currentPosition + 10_000).coerceAtMost(mp.duration)
                                            mp.seekTo(target)
                                            audioPosMs = target
                                        }
                                    }) {
                                        Icon(Icons.Default.FastForward, contentDescription = "تقديم 10 ثوانٍ")
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تسجيل صوت الأستاذ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        if (audioDurationMs > 0) {
                                            Slider(
                                                value = audioPosMs.toFloat().coerceIn(0f, audioDurationMs.toFloat()),
                                                onValueChange = { newPos ->
                                                    audioPosMs = newPos.toInt()
                                                    mediaPlayer?.seekTo(newPos.toInt())
                                                },
                                                valueRange = 0f..audioDurationMs.toFloat()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lesson Paragraphs & Board Photos
            itemsIndexed(blocks, key = { _, b -> b.id }) { index, block ->
                if (block.blockType == "IMAGE" && !block.imageFileName.isNullOrBlank()) {
                    val imgFile = MediaStorageHelper.photoFile(context, block.imageFileName)
                    BoardPhotoBlockCard(
                        file = imgFile,
                        onOpenFullScreen = { fullScreenPhotoFile = imgFile },
                        onDelete = { viewModel.deleteBlock(block) }
                    )
                } else {
                    TextBlockCard(
                        block = block,
                        displayIndex = index + 1,
                        onUpdate = { updated -> viewModel.upsertBlock(updated) },
                        onDelete = { viewModel.deleteBlock(block) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Full-screen Board Photo Viewer
    fullScreenPhotoFile?.let { file ->
        Dialog(
            onDismissRequest = { fullScreenPhotoFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val bmp = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "صورة السبورة بملء الشاشة",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = { fullScreenPhotoFile = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TextBlockCard(
    block: LessonBlockEntity,
    displayIndex: Int,
    onUpdate: (LessonBlockEntity) -> Unit,
    onDelete: () -> Unit
) {
    var localText by remember(block.id) { mutableStateOf(block.content) }
    val pen = PenColor.fromCode(block.penColor)
    val listStyle = BlockListStyle.fromCode(block.listStyle)

    val prefixLabel = when (listStyle) {
        BlockListStyle.BULLET -> "•"
        BlockListStyle.NUMBERED -> "$displayIndex."
        BlockListStyle.NONE -> null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isImportantForExam) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (block.isImportantForExam) 1.5.dp else 0.7.dp,
                color = if (block.isImportantForExam) StarYellow else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (prefixLabel != null) {
                Text(
                    text = prefixLabel,
                    fontWeight = FontWeight.Bold,
                    color = Color(pen.hexColor),
                    modifier = Modifier.padding(top = 14.dp, end = 6.dp)
                )
            }

            OutlinedTextField(
                value = localText,
                onValueChange = {
                    localText = it
                    onUpdate(block.copy(content = it))
                },
                placeholder = { Text("اكتب ملاحظاتك هنا (${pen.arabicLabel})...") },
                textStyle = TextStyle(
                    color = if (pen == PenColor.BLACK) MaterialTheme.colorScheme.onSurface else Color(pen.hexColor),
                    fontSize = 15.5.sp,
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (block.isUnderline) TextDecoration.Underline else TextDecoration.None
                ),
                modifier = Modifier.weight(1f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onUpdate(block.copy(isImportantForExam = !block.isImportantForExam)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (block.isImportantForExam) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "مهم للامتحان",
                        tint = if (block.isImportantForExam) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف الفقرة",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardPhotoBlockCard(
    file: File,
    onOpenFullScreen: () -> Unit,
    onDelete: () -> Unit
) {
    val bmp = remember(file.absolutePath) {
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFullScreen() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "صورة السبورة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "صورة السبورة غير موجودة محلياً",
                    modifier = Modifier.padding(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
