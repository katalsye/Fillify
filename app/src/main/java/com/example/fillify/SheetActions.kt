package com.example.fillify

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson

/**
 * 저장된 학습지에 대한 공용 액션(학습·PDF·삭제) 다이얼로그.
 * 홈과 라이브러리가 함께 사용한다.
 */
object SheetActions {

    /** @param onChanged 삭제 등으로 목록이 바뀌었을 때 호출 */
    fun showOptions(context: Context, sheet: StudySheet, onChanged: () -> Unit) {
        val options = arrayOf("학습하기", "편집", "형광펜 PDF", "빈칸 PDF", "삭제")
        AlertDialog.Builder(context)
            .setTitle(sheet.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> context.startActivity(
                        Intent(context, QuizActivity::class.java)
                            .putExtra(QuizActivity.EXTRA_WORDS_JSON, Gson().toJson(sheet.words))
                            .putExtra(QuizActivity.EXTRA_TITLE, sheet.title)
                    )
                    1 -> context.startActivity(
                        Intent(context, OcrActivity::class.java)
                            .putExtra(OcrActivity.EXTRA_SHEET_ID, sheet.id)
                    )
                    2 -> exportPdf(context, sheet, PdfExporter.Type.HIGHLIGHT)
                    3 -> exportPdf(context, sheet, PdfExporter.Type.BLANK)
                    4 -> confirmDelete(context, sheet, onChanged)
                }
            }
            .show()
    }

    private fun exportPdf(context: Context, sheet: StudySheet, type: PdfExporter.Type) {
        val uri = PdfExporter.export(context, sheet.words, type)
        if (uri == null) {
            Toast.makeText(context, "선택된 단어가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(PdfExporter.viewIntent(uri))
    }

    private fun confirmDelete(context: Context, sheet: StudySheet, onChanged: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("삭제")
            .setMessage("'${sheet.title}' 학습지를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                StudyRepository.delete(context, sheet.id)
                onChanged()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
