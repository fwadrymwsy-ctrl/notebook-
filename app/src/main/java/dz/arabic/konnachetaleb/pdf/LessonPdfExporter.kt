package dz.arabic.konnachetaleb.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import dz.arabic.konnachetaleb.data.BlockListStyle
import dz.arabic.konnachetaleb.data.LessonBlockEntity
import dz.arabic.konnachetaleb.data.LessonEntity
import dz.arabic.konnachetaleb.data.PenColor
import dz.arabic.konnachetaleb.data.SessionType
import dz.arabic.konnachetaleb.data.SubjectEntity
import dz.arabic.konnachetaleb.media.MediaStorageHelper
import java.io.File
import java.io.FileOutputStream

object LessonPdfExporter {

    private const val PAGE_WIDTH = 595   // A4 width in points (72 DPI)
    private const val PAGE_HEIGHT = 842  // A4 height in points
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = (PAGE_WIDTH - 2 * MARGIN).toInt()

    /**
     * Generates a clean RTL Arabic PDF using android.graphics.pdf.PdfDocument + StaticLayout.
     * Supports Arabic shaping, pen colors, bold/underline, bullet/numbered lists, exam stars, and board photos.
     */
    fun exportLessonToPdf(
        context: Context,
        subject: SubjectEntity,
        lesson: LessonEntity,
        blocks: List<LessonBlockEntity>
    ): File {
        val doc = PdfDocument()
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun nextPage() {
            drawPageFooter(canvas, pageNumber, subject.name)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        // 1. Header Banner
        val headerBg = Paint().apply { color = subject.colorHex.toInt() }
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 74f),
            10f, 10f, headerBg
        )

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 11f
        }

        val headerTitle = "${subject.name} — ${lesson.title}"
        val headerSub = "الدرس رقم ${lesson.sequenceNumber} | ${SessionType.fromCode(lesson.sessionType).arabicLabel} | التاريخ: ${lesson.dateIso}"
        drawRtlLayout(canvas, headerTitle, titlePaint, (CONTENT_WIDTH - 24), MARGIN + 12f, y + 12f)
        drawRtlLayout(canvas, headerSub, subtitlePaint, (CONTENT_WIDTH - 24), MARGIN + 12f, y + 46f)
        y += 88f

        // 2. Render Blocks Sequentially
        var numberCounter = 0
        blocks.forEach { block ->
            if (block.blockType == "IMAGE" && !block.imageFileName.isNullOrBlank()) {
                numberCounter = 0
                val imgFile = MediaStorageHelper.photoFile(context, block.imageFileName)
                if (imgFile.exists()) {
                    val bitmap = decodeScaledBitmap(imgFile, CONTENT_WIDTH, 320)
                    if (bitmap != null) {
                        val drawW = bitmap.width.toFloat().coerceAtMost(CONTENT_WIDTH.toFloat())
                        val scale = drawW / bitmap.width.toFloat()
                        val drawH = bitmap.height * scale

                        if (y + drawH + 24f > PAGE_HEIGHT - MARGIN - 24f) {
                            nextPage()
                        }

                        val left = MARGIN + (CONTENT_WIDTH - drawW) / 2f
                        canvas.drawBitmap(bitmap, null, RectF(left, y, left + drawW, y + drawH), null)
                        val borderPaint = Paint().apply {
                            style = Paint.Style.STROKE
                            color = Color.parseColor("#CBD5E1")
                            strokeWidth = 1f
                        }
                        canvas.drawRect(left, y, left + drawW, y + drawH, borderPaint)
                        bitmap.recycle()
                        y += drawH + 14f
                    }
                }
            } else {
                val rawText = block.content.trim()
                if (rawText.isEmpty()) return@forEach

                val listStyle = BlockListStyle.fromCode(block.listStyle)
                val prefix = when (listStyle) {
                    BlockListStyle.BULLET -> {
                        numberCounter = 0
                        "• "
                    }
                    BlockListStyle.NUMBERED -> {
                        numberCounter++
                        "$numberCounter. "
                    }
                    BlockListStyle.NONE -> {
                        numberCounter = 0
                        ""
                    }
                }
                val starPrefix = if (block.isImportantForExam) "★ [مهم للامتحان] " else ""
                val fullText = "$starPrefix$prefix$rawText"

                val pen = PenColor.fromCode(block.penColor)
                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pen.hexColor.toInt()
                    textSize = 13f
                    typeface = Typeface.create(
                        Typeface.DEFAULT,
                        if (block.isBold) Typeface.BOLD else Typeface.NORMAL
                    )
                    isUnderlineText = block.isUnderline
                }

                val layout = buildRtlStaticLayout(fullText, textPaint, CONTENT_WIDTH - 16)
                val blockHeight = layout.height + 14f

                if (y + blockHeight > PAGE_HEIGHT - MARGIN - 24f) {
                    nextPage()
                }

                if (block.isImportantForExam) {
                    val highlightBg = Paint().apply { color = Color.parseColor("#FEF9C3") }
                    val highlightBar = Paint().apply { color = Color.parseColor("#D97706") }
                    canvas.drawRoundRect(
                        RectF(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y + layout.height + 6f),
                        6f, 6f, highlightBg
                    )
                    canvas.drawRect(
                        PAGE_WIDTH - MARGIN - 4f, y - 4f,
                        PAGE_WIDTH - MARGIN, y + layout.height + 6f,
                        highlightBar
                    )
                }

                canvas.save()
                canvas.translate(MARGIN + 8f, y)
                layout.draw(canvas)
                canvas.restore()

                y += blockHeight
            }
        }

        drawPageFooter(canvas, pageNumber, subject.name)
        doc.finishPage(page)

        val safeTitle = lesson.title.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(30)
        val outFile = File(
            MediaStorageHelper.pdfsDir(context),
            "Konnach_${safeTitle}_${lesson.dateIso}.pdf"
        )
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun buildRtlStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(4f, 1.15f)
            .setIncludePad(true)
            .build()
    }

    private fun drawRtlLayout(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        width: Int,
        x: Float,
        y: Float
    ) {
        val layout = buildRtlStaticLayout(text, paint, width)
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, subjectName: String) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
        }
        val footerText = "كناش الطالب — $subjectName | صفحة $pageNumber"
        drawRtlLayout(canvas, footerText, paint, CONTENT_WIDTH, MARGIN, PAGE_HEIGHT - 28f)
    }

    private fun decodeScaledBitmap(file: File, maxW: Int, maxH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        var sample = 1
        while (opts.outWidth / sample > maxW * 2 || opts.outHeight / sample > maxH * 2) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
        val ratio = minOf(maxW.toFloat() / bmp.width, maxH.toFloat() / bmp.height, 1f)
        if (ratio >= 1f) return bmp
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * ratio).toInt().coerceAtLeast(1),
            (bmp.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled != bmp) bmp.recycle()
        return scaled
    }
}
