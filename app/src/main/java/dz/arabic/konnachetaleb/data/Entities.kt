package dz.arabic.konnachetaleb.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class SessionType(val code: String, val arabicLabel: String) {
    COURS("COURS", "محاضرة (Cours)"),
    TD("TD", "أعمال موجهة (TD)"),
    TP("TP", "أعمال تطبيقية (TP)");

    companion object {
        fun fromCode(code: String?): SessionType =
            entries.firstOrNull { it.code == code } ?: COURS
    }
}

enum class PenColor(val code: String, val arabicLabel: String, val hexColor: Long) {
    BLACK("BLACK", "أسود (نص عادي)", 0xFF111827L),
    RED("RED", "أحمر (عناوين)", 0xFFB91C1CL),
    GREEN("GREEN", "أخضر (تعاريف)", 0xFF15803DL),
    BLUE("BLUE", "أزرق (شواهد)", 0xFF1D4ED8L);

    companion object {
        fun fromCode(code: String?): PenColor =
            entries.firstOrNull { it.code == code } ?: BLACK
    }
}

enum class BlockListStyle(val code: String, val arabicLabel: String) {
    NONE("NONE", "عادي"),
    BULLET("BULLET", "قائمة نقطية"),
    NUMBERED("NUMBERED", "قائمة مرقمة");

    companion object {
        fun fromCode(code: String?): BlockListStyle =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class ThemeMode(val key: String, val arabicLabel: String) {
    SYSTEM("system", "تلقائي (النظام)"),
    LIGHT("light", "الوضع النهاري"),
    DARK("dark", "الوضع الداكن");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

@Serializable
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val semester: String,
    val cycle: String,
    val year: String,
    val specialty: String,
    val nature: String = ArabicLmdCatalog.NATURE_BOTH,
    val professorName: String = "",
    val coefficient: String = "2",
    val credits: String = "4",
    val colorHex: Long = 0xFF1E3A5FL,
    val iconKey: String = "book",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val sequenceNumber: Int,
    val title: String,
    val sessionType: String = SessionType.COURS.code,
    val dateIso: String,
    val audioFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "lesson_blocks",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class LessonBlockEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val sortOrder: Int,
    val blockType: String = "TEXT", // "TEXT" or "IMAGE"
    val content: String = "",
    val imageFileName: String? = null,
    val penColor: String = PenColor.BLACK.code,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false,
    val listStyle: String = BlockListStyle.NONE.code,
    val isImportantForExam: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TimetableSlotEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val dayOfWeek: Int, // 7 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday (ISO DayOfWeek)
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val sessionType: String = SessionType.COURS.code,
    val room: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val dueDateIso: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class StarredHighlightItem(
    val blockId: String,
    val lessonId: String,
    val subjectId: String,
    val subjectName: String,
    val lessonTitle: String,
    val lessonDate: String,
    val content: String,
    val penColor: String
)

data class SearchHitItem(
    val lessonId: String,
    val subjectId: String,
    val subjectName: String,
    val lessonTitle: String,
    val lessonDate: String,
    val matchedSnippet: String
)
