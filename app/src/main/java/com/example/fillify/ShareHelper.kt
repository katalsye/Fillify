package com.example.fillify

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 학습지를 안드로이드 공유 시트로 내보낸다. 모두 로컬 동작이라 로그인·네트워크가 필요 없다.
 */
object ShareHelper {

    /** 선택 단어를 빈칸(＿＿)으로 만든 텍스트 학습지를 생성 */
    fun buildText(sheet: StudySheet): String {
        val sb = StringBuilder()
        var lineStart = true
        for (w in sheet.words) {
            if (w.isLineBreak) {
                sb.append("\n")
                lineStart = true
                continue
            }
            if (!lineStart) sb.append(" ")
            sb.append(if (w.selected) "＿＿" else w.text)
            lineStart = false
        }
        return sb.toString().trim()
    }

    /** 텍스트로 공유 (카톡·메모 등에 바로 붙여넣기 좋음) */
    fun shareText(context: Context, sheet: StudySheet) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, sheet.title)
            putExtra(Intent.EXTRA_TEXT, buildText(sheet))
        }
        context.startActivity(Intent.createChooser(intent, "학습지 공유"))
    }

    /** PDF 파일로 공유 */
    fun sharePdf(context: Context, sheet: StudySheet, type: PdfExporter.Type) {
        val uri = PdfExporter.export(context, sheet.words, type)
        if (uri == null) {
            Toast.makeText(context, "선택된 단어가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, sheet.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF 공유"))
    }
}
