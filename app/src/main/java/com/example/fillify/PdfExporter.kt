package com.example.fillify

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

/**
 * 학습지 단어 목록을 PDF로 렌더링해 Downloads에 저장한다.
 * OCR 화면과 라이브러리에서 공유한다.
 */
object PdfExporter {

    enum class Type(val label: String) {
        HIGHLIGHT("형광펜"),
        BLANK("빈칸")
    }

    /**
     * PDF를 생성해 저장한 뒤 뷰어로 여는 Intent를 반환한다.
     * 선택된 단어가 없으면 null.
     */
    fun export(context: Context, words: List<StudyWord>, type: Type): Uri? {
        if (words.none { it.selected }) return null

        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply { textSize = 14f }
        val highlightPaint = Paint().apply {
            color = 0xFFFFFF99.toInt()
            style = Paint.Style.FILL
        }

        val marginLeft = 40f
        val marginRight = 520f
        val marginTop = 40f
        val lineHeight = 30f
        val bottomLimit = 800f
        val space = textPaint.measureText(" ")

        var pageNumber = 1
        var page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        )
        var canvas: Canvas = page.canvas
        var x = marginLeft
        var y = marginTop

        fun newPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            )
            canvas = page.canvas
            x = marginLeft
            y = marginTop
        }

        for (word in words) {
            if (word.isLineBreak) {
                x = marginLeft
                y += lineHeight
                if (y > bottomLimit) newPage()
                continue
            }

            val drawText = if (type == Type.BLANK && word.selected) "_____" else word.text
            val width = textPaint.measureText(drawText)

            // 그리기 전에 줄바꿈해 우측 경계 초과 방지
            if (x + width > marginRight) {
                x = marginLeft
                y += lineHeight
                if (y > bottomLimit) newPage()
            }

            if (type == Type.HIGHLIGHT && word.selected) {
                canvas.drawRect(
                    x,
                    y - textPaint.textSize,
                    x + width,
                    y + 5,
                    highlightPaint
                )
            }

            canvas.drawText(drawText, x, y, textPaint)

            if (type == Type.BLANK && word.selected) {
                canvas.drawLine(x, y + 4, x + width, y + 4, textPaint)
            }

            x += width + space
        }

        pdfDocument.finishPage(page)

        val fileName = "${type.name.lowercase()}_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            values
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()
        return uri
    }

    /** PDF 뷰어 선택 Intent */
    fun viewIntent(uri: Uri): Intent = Intent.createChooser(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        "PDF 열기"
    )
}
