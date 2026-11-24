package dz.arabic.konnachetaleb.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY semester ASC, sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY semester ASC, sortOrder ASC, name ASC")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId ORDER BY sequenceNumber DESC, createdAt DESC")
    fun observeBySubject(subjectId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY updatedAt DESC")
    suspend fun getAll(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getById(lessonId: String): LessonEntity?

    @Query("SELECT COALESCE(MAX(sequenceNumber), 0) FROM lessons WHERE subjectId = :subjectId")
    suspend fun getMaxSequence(subjectId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lesson: LessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lessons: List<LessonEntity>)

    @Update
    suspend fun update(lesson: LessonEntity)

    @Delete
    suspend fun delete(lesson: LessonEntity)

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()
}

@Dao
interface LessonBlockDao {
    @Query("SELECT * FROM lesson_blocks WHERE lessonId = :lessonId ORDER BY sortOrder ASC")
    fun observeByLesson(lessonId: String): Flow<List<LessonBlockEntity>>

    @Query("SELECT * FROM lesson_blocks WHERE lessonId = :lessonId ORDER BY sortOrder ASC")
    suspend fun getByLesson(lessonId: String): List<LessonBlockEntity>

    @Query("SELECT * FROM lesson_blocks ORDER BY sortOrder ASC")
    suspend fun getAll(): List<LessonBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: LessonBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<LessonBlockEntity>)

    @Delete
    suspend fun delete(block: LessonBlockEntity)

    @Query("DELETE FROM lesson_blocks")
    suspend fun deleteAll()

    @Query("""
        SELECT b.id AS blockId,
               l.id AS lessonId,
               s.id AS subjectId,
               s.name AS subjectName,
               l.title AS lessonTitle,
               l.dateIso AS lessonDate,
               b.content AS content,
               b.penColor AS penColor
        FROM lesson_blocks b
        INNER JOIN lessons l ON b.lessonId = l.id
        INNER JOIN subjects s ON l.subjectId = s.id
        WHERE b.isImportantForExam = 1 AND b.blockType = 'TEXT' AND LENGTH(TRIM(b.content)) > 0
        ORDER BY l.dateIso DESC, b.sortOrder ASC
    """)
    fun observeAllStarredHighlights(): Flow<List<StarredHighlightItem>>

    @Query("""
        SELECT b.id AS blockId,
               l.id AS lessonId,
               s.id AS subjectId,
               s.name AS subjectName,
               l.title AS lessonTitle,
               l.dateIso AS lessonDate,
               b.content AS content,
               b.penColor AS penColor
        FROM lesson_blocks b
        INNER JOIN lessons l ON b.lessonId = l.id
        INNER JOIN subjects s ON l.subjectId = s.id
        WHERE s.id = :subjectId AND b.isImportantForExam = 1 AND b.blockType = 'TEXT' AND LENGTH(TRIM(b.content)) > 0
        ORDER BY l.dateIso DESC, b.sortOrder ASC
    """)
    fun observeStarredHighlightsBySubject(subjectId: String): Flow<List<StarredHighlightItem>>

    @Query("""
        SELECT l.id AS lessonId,
               s.id AS subjectId,
               s.name AS subjectName,
               l.title AS lessonTitle,
               l.dateIso AS lessonDate,
               COALESCE(b.content, l.title) AS matchedSnippet
        FROM lessons l
        INNER JOIN subjects s ON l.subjectId = s.id
        LEFT JOIN lesson_blocks b ON b.lessonId = l.id
        WHERE s.name LIKE '%' || :query || '%'
           OR l.title LIKE '%' || :query || '%'
           OR b.content LIKE '%' || :query || '%'
        GROUP BY l.id
        ORDER BY l.updatedAt DESC
        LIMIT 80
    """)
    suspend fun searchAcrossNotebooks(query: String): List<SearchHitItem>
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeAll(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAll(): List<TimetableSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: TimetableSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(slots: List<TimetableSlotEntity>)

    @Delete
    suspend fun delete(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY isCompleted ASC, createdAt DESC")
    fun observeBySubject(subjectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAll(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
