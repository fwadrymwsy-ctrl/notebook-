package dz.arabic.konnachetaleb.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class KonnachRepository(private val db: AppDatabase) {

    fun observeSubjects() = db.subjectDao().observeAll()
    fun observeLessons(subjectId: String) = db.lessonDao().observeBySubject(subjectId)
    fun observeBlocks(lessonId: String) = db.lessonBlockDao().observeByLesson(lessonId)
    fun observeTimetable() = db.timetableDao().observeAll()
    fun observeTasks(subjectId: String) = db.taskDao().observeBySubject(subjectId)
    fun observeAllHighlights() = db.lessonBlockDao().observeAllStarredHighlights()
    fun observeSubjectHighlights(subjectId: String) = db.lessonBlockDao().observeStarredHighlightsBySubject(subjectId)

    suspend fun getAllSubjects() = db.subjectDao().getAll()
    suspend fun getSubjectById(id: String) = db.subjectDao().getById(id)
    suspend fun getLessonById(id: String) = db.lessonDao().getById(id)
    suspend fun getBlocksByLesson(lessonId: String) = db.lessonBlockDao().getByLesson(lessonId)
    suspend fun getAllTimetableSlots() = db.timetableDao().getAll()
    suspend fun search(query: String) = db.lessonBlockDao().searchAcrossNotebooks(query)

    suspend fun loadCatalogSubjects(
        cycle: String,
        year: String,
        specialty: String,
        semester: String,
        replaceExistingForSemester: Boolean
    ) {
        val track = ArabicLmdCatalog.trackFor(cycle, year, specialty)
        val semestersToLoad = if (track != null && !replaceExistingForSemester) {
            listOf(track.oddSemesterCode, track.evenSemesterCode)
        } else {
            listOf(semester)
        }

        for (sem in semestersToLoad) {
            val catalogItems = ArabicLmdCatalog.subjectsFor(cycle, year, specialty, sem)
            if (replaceExistingForSemester && sem == semester) {
                val existing = db.subjectDao().getAll().filter { it.semester == sem }
                existing.forEach { db.subjectDao().delete(it) }
            }
            val existingNames = db.subjectDao().getAll()
                .filter { it.semester == sem }
                .map { it.name }
                .toSet()

            val entities = catalogItems.mapIndexedNotNull { idx, item ->
                if (item.name in existingNames) return@mapIndexedNotNull null
                SubjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = item.name,
                    semester = sem,
                    cycle = cycle,
                    year = year,
                    specialty = specialty,
                    nature = item.nature,
                    colorHex = ArabicLmdCatalog.NOTEBOOK_COLORS[idx % ArabicLmdCatalog.NOTEBOOK_COLORS.size],
                    iconKey = ArabicLmdCatalog.NOTEBOOK_ICONS[idx % ArabicLmdCatalog.NOTEBOOK_ICONS.size],
                    sortOrder = idx
                )
            }
            if (entities.isNotEmpty()) {
                db.subjectDao().upsertAll(entities)
            }
        }
    }

    suspend fun saveSubject(subject: SubjectEntity) {
        db.subjectDao().upsert(subject.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        db.subjectDao().delete(subject)
    }

    suspend fun createLesson(subjectId: String, sessionType: SessionType, title: String): LessonEntity {
        val nextSeq = db.lessonDao().getMaxSequence(subjectId) + 1
        val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val finalTitle = title.trim().ifBlank { "الدرس رقم $nextSeq" }
        val lesson = LessonEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            sequenceNumber = nextSeq,
            title = finalTitle,
            sessionType = sessionType.code,
            dateIso = todayIso
        )
        db.lessonDao().upsert(lesson)
        // Create an initial empty text paragraph so the student can write immediately
        db.lessonBlockDao().upsert(
            LessonBlockEntity(
                id = UUID.randomUUID().toString(),
                lessonId = lesson.id,
                sortOrder = 0,
                blockType = "TEXT",
                content = ""
            )
        )
        return lesson
    }

    suspend fun updateLesson(lesson: LessonEntity) {
        db.lessonDao().upsert(lesson.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteLesson(lesson: LessonEntity) {
        db.lessonDao().delete(lesson)
    }

    suspend fun upsertBlock(block: LessonBlockEntity) {
        db.lessonBlockDao().upsert(block.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteBlock(block: LessonBlockEntity) {
        db.lessonBlockDao().delete(block)
    }

    suspend fun addTextBlock(
        lessonId: String,
        penColor: PenColor = PenColor.BLACK,
        isBold: Boolean = false,
        isUnderline: Boolean = false,
        listStyle: BlockListStyle = BlockListStyle.NONE
    ): LessonBlockEntity {
        val existing = db.lessonBlockDao().getByLesson(lessonId)
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val block = LessonBlockEntity(
            id = UUID.randomUUID().toString(),
            lessonId = lessonId,
            sortOrder = nextOrder,
            blockType = "TEXT",
            content = "",
            penColor = penColor.code,
            isBold = isBold,
            isUnderline = isUnderline,
            listStyle = listStyle.code
        )
        db.lessonBlockDao().upsert(block)
        return block
    }

    suspend fun addImageBlock(lessonId: String, imageFileName: String): LessonBlockEntity {
        val existing = db.lessonBlockDao().getByLesson(lessonId)
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val block = LessonBlockEntity(
            id = UUID.randomUUID().toString(),
            lessonId = lessonId,
            sortOrder = nextOrder,
            blockType = "IMAGE",
            content = "صورة السبورة",
            imageFileName = imageFileName
        )
        db.lessonBlockDao().upsert(block)
        return block
    }

    suspend fun saveTimetableSlot(slot: TimetableSlotEntity) {
        db.timetableDao().upsert(slot.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity) {
        db.timetableDao().delete(slot)
    }

    suspend fun saveTask(task: TaskEntity) {
        db.taskDao().upsert(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskEntity) {
        db.taskDao().delete(task)
    }
}
