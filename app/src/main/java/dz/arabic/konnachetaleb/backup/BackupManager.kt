package dz.arabic.konnachetaleb.backup

import android.content.Context
import android.net.Uri
import dz.arabic.konnachetaleb.data.AppDatabase
import dz.arabic.konnachetaleb.data.LessonBlockEntity
import dz.arabic.konnachetaleb.data.LessonEntity
import dz.arabic.konnachetaleb.data.SubjectEntity
import dz.arabic.konnachetaleb.data.TaskEntity
import dz.arabic.konnachetaleb.data.TimetableSlotEntity
import dz.arabic.konnachetaleb.media.MediaStorageHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupManifest(
    val appId: String = "dz.arabic.konnachetaleb",
    val schemaVersion: Int = 1,
    val createdAtIso: String,
    val includeMedia: Boolean,
    val specialty: String
)

@Serializable
data class BackupPayload(
    val subjects: List<SubjectEntity>,
    val lessons: List<LessonEntity>,
    val blocks: List<LessonBlockEntity>,
    val timetableSlots: List<TimetableSlotEntity>,
    val tasks: List<TaskEntity>
)

data class BackupRestoreResult(
    val success: Boolean,
    val message: String
)

object BackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun suggestedFileName(specialty: String, includeMedia: Boolean): String {
        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cleanSpec = specialty.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(24)
        val suffix = if (includeMedia) "Full" else "TextOnly"
        return "KonnachEtaleb_${cleanSpec}_${suffix}_${dateStr}.zip"
    }

    suspend fun exportToUri(
        context: Context,
        db: AppDatabase,
        destinationUri: Uri,
        specialty: String,
        includeMedia: Boolean
    ): BackupRestoreResult {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { os ->
                writeZipToStream(context, db, os, specialty, includeMedia)
            } ?: return BackupRestoreResult(false, "تعذر فتح مسار الحفظ المختار.")
            BackupRestoreResult(
                true,
                if (includeMedia) "تم حفظ النسخة الشاملة (مع الصور والتسجيلات) بنجاح."
                else "تم حفظ نسخة النصوص والبيانات بنجاح."
            )
        }.getOrElse {
            BackupRestoreResult(false, "فشل التصدير: ${it.localizedMessage ?: "خطأ غير متوقع"}")
        }
    }

    suspend fun exportToShareableFile(
        context: Context,
        db: AppDatabase,
        specialty: String,
        includeMedia: Boolean
    ): File {
        val file = File(
            MediaStorageHelper.backupsDir(context),
            suggestedFileName(specialty, includeMedia)
        )
        FileOutputStream(file).use { fos ->
            writeZipToStream(context, db, fos, specialty, includeMedia)
        }
        return file
    }

    private suspend fun writeZipToStream(
        context: Context,
        db: AppDatabase,
        outputStream: OutputStream,
        specialty: String,
        includeMedia: Boolean
    ) {
        val manifest = BackupManifest(
            createdAtIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            includeMedia = includeMedia,
            specialty = specialty
        )
        val payload = BackupPayload(
            subjects = db.subjectDao().getAll(),
            lessons = db.lessonDao().getAll(),
            blocks = db.lessonBlockDao().getAll(),
            timetableSlots = db.timetableDao().getAll(),
            tasks = db.taskDao().getAll()
        )

        ZipOutputStream(outputStream).use { zos ->
            // 1. manifest.json
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. database.json
            zos.putNextEntry(ZipEntry("database.json"))
            zos.write(json.encodeToString(BackupPayload.serializer(), payload).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. Optional media files (board photos + audio recordings)
            if (includeMedia) {
                MediaStorageHelper.photosDir(context).listFiles()?.forEach { f ->
                    if (f.isFile) {
                        zos.putNextEntry(ZipEntry("board_photos/${f.name}"))
                        f.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                MediaStorageHelper.audioDir(context).listFiles()?.forEach { f ->
                    if (f.isFile) {
                        zos.putNextEntry(ZipEntry("audio_notes/${f.name}"))
                        f.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    /**
     * Validates ZIP structure and schema BEFORE touching any local database tables.
     * Supports Full Restore (replaceAll = true) or Smart Merge (replaceAll = false).
     */
    suspend fun restoreFromUri(
        context: Context,
        db: AppDatabase,
        sourceUri: Uri,
        replaceAll: Boolean
    ): BackupRestoreResult {
        var manifestJson: String? = null
        var databaseJson: String? = null
        val tempDir = File(context.cacheDir, "restore_stage_${System.currentTimeMillis()}").apply { mkdirs() }

        return try {
            // Pass 1: Read and stage entries safely, blocking Zip Slip paths
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name.contains("..")) {
                            return BackupRestoreResult(false, "ملف النسخة الاحتياطية غير آمن أو تالف.")
                        }
                        when {
                            name == "manifest.json" -> manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                            name == "database.json" -> databaseJson = zis.readBytes().toString(Charsets.UTF_8)
                            name.startsWith("board_photos/") && !entry.isDirectory -> {
                                val fileName = File(name).name
                                val out = File(File(tempDir, "board_photos").apply { mkdirs() }, fileName)
                                out.outputStream().use { zis.copyTo(it) }
                            }
                            name.startsWith("audio_notes/") && !entry.isDirectory -> {
                                val fileName = File(name).name
                                val out = File(File(tempDir, "audio_notes").apply { mkdirs() }, fileName)
                                out.outputStream().use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return BackupRestoreResult(false, "تعذر قراءة ملف النسخة الاحتياطية.")

            if (manifestJson == null || databaseJson == null) {
                return BackupRestoreResult(false, "ملف ZIP غير صالح: لا يحتوي على بيانات تطبيق كناش الطالب.")
            }

            val manifest = runCatching {
                json.decodeFromString(BackupManifest.serializer(), manifestJson!!)
            }.getOrNull() ?: return BackupRestoreResult(false, "فشل التحقق من ترويسة النسخة الاحتياطية.")

            if (manifest.appId != "dz.arabic.konnachetaleb") {
                return BackupRestoreResult(false, "هذا الملف لا يخص تطبيق كناش الطالب.")
            }

            val payload = runCatching {
                json.decodeFromString(BackupPayload.serializer(), databaseJson!!)
            }.getOrNull() ?: return BackupRestoreResult(false, "بنية قاعدة البيانات داخل الملف تالفة.")

            // Copy staged media files into internal storage
            File(tempDir, "board_photos").listFiles()?.forEach { src ->
                val dest = MediaStorageHelper.photoFile(context, src.name)
                if (replaceAll || !dest.exists()) src.copyTo(dest, overwrite = true)
            }
            File(tempDir, "audio_notes").listFiles()?.forEach { src ->
                val dest = MediaStorageHelper.audioFile(context, src.name)
                if (replaceAll || !dest.exists()) src.copyTo(dest, overwrite = true)
            }

            if (replaceAll) {
                // Full Restore
                db.lessonBlockDao().deleteAll()
                db.lessonDao().deleteAll()
                db.timetableDao().deleteAll()
                db.taskDao().deleteAll()
                db.subjectDao().deleteAll()

                db.subjectDao().upsertAll(payload.subjects)
                db.lessonDao().upsertAll(payload.lessons)
                db.lessonBlockDao().upsertAll(payload.blocks)
                db.timetableDao().upsertAll(payload.timetableSlots)
                db.taskDao().upsertAll(payload.tasks)

                BackupRestoreResult(
                    true,
                    "تم الاسترجاع الكامل بنجاح (${payload.subjects.size} مقياس، ${payload.lessons.size} درس)."
                )
            } else {
                // Smart Merge: insert missing records and only overwrite when backup record is newer
                val existingSubjects = db.subjectDao().getAll().associateBy { it.id }
                val subjectsToSave = payload.subjects.filter { incoming ->
                    val local = existingSubjects[incoming.id]
                    local == null || incoming.updatedAt > local.updatedAt
                }
                db.subjectDao().upsertAll(subjectsToSave)

                val existingLessons = db.lessonDao().getAll().associateBy { it.id }
                val lessonsToSave = payload.lessons.filter { incoming ->
                    val local = existingLessons[incoming.id]
                    local == null || incoming.updatedAt > local.updatedAt
                }
                db.lessonDao().upsertAll(lessonsToSave)

                val existingBlocks = db.lessonBlockDao().getAll().associateBy { it.id }
                val blocksToSave = payload.blocks.filter { incoming ->
                    val local = existingBlocks[incoming.id]
                    local == null || incoming.updatedAt > local.updatedAt
                }
                db.lessonBlockDao().upsertAll(blocksToSave)

                val existingSlots = db.timetableDao().getAll().associateBy { it.id }
                val slotsToSave = payload.timetableSlots.filter { incoming ->
                    val local = existingSlots[incoming.id]
                    local == null || incoming.updatedAt > local.updatedAt
                }
                db.timetableDao().upsertAll(slotsToSave)

                val existingTasks = db.taskDao().getAll().associateBy { it.id }
                val tasksToSave = payload.tasks.filter { incoming ->
                    val local = existingTasks[incoming.id]
                    local == null || incoming.updatedAt > local.updatedAt
                }
                db.taskDao().upsertAll(tasksToSave)

                BackupRestoreResult(
                    true,
                    "تم الدمج الذكي بنجاح: أُضيف/حُدّث ${lessonsToSave.size} درس دون مسح دروسك الأحدث."
                )
            }
        } catch (e: Exception) {
            BackupRestoreResult(false, "تعذر استرجاع الملف: ${e.localizedMessage ?: "خطأ في القراءة"}")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
