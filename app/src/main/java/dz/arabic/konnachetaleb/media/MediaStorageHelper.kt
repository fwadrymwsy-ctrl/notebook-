package dz.arabic.konnachetaleb.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object MediaStorageHelper {

    fun photosDir(context: Context): File =
        File(context.filesDir, "board_photos").apply { if (!exists()) mkdirs() }

    fun audioDir(context: Context): File =
        File(context.filesDir, "audio_notes").apply { if (!exists()) mkdirs() }

    fun pdfsDir(context: Context): File =
        File(context.filesDir, "exported_pdfs").apply { if (!exists()) mkdirs() }

    fun backupsDir(context: Context): File =
        File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }

    fun createPhotoFile(context: Context): File {
        val name = "board_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        return File(photosDir(context), name)
    }

    fun createAudioFile(context: Context, lessonId: String): File {
        val name = "audio_${lessonId.take(8)}_${System.currentTimeMillis()}.m4a"
        return File(audioDir(context), name)
    }

    fun photoFile(context: Context, fileName: String): File = File(photosDir(context), fileName)

    fun audioFile(context: Context, fileName: String): File = File(audioDir(context), fileName)

    fun getShareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyPickedImageToInternal(context: Context, sourceUri: Uri): String? {
        return runCatching {
            val dest = createPhotoFile(context)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0) dest.name else null
        }.getOrNull()
    }
}
