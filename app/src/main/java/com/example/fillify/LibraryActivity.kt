package com.example.fillify

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

/** 저장된 학습지 목록. 탭하면 학습/PDF/삭제 옵션을 제공한다. */
class LibraryActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtEmpty: TextView
    private val sheets = mutableListOf<StudySheet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recycler = findViewById(R.id.recyclerSheets)
        txtEmpty = findViewById(R.id.txtEmpty)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = SheetAdapter()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        sheets.clear()
        sheets.addAll(StudyRepository.loadAll(this))
        recycler.adapter?.notifyDataSetChanged()
        txtEmpty.visibility = if (sheets.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showOptions(sheet: StudySheet) {
        val options = arrayOf("학습하기", "형광펜 PDF", "빈칸 PDF", "삭제")
        AlertDialog.Builder(this)
            .setTitle(sheet.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(
                        Intent(this, QuizActivity::class.java)
                            .putExtra(QuizActivity.EXTRA_WORDS_JSON, Gson().toJson(sheet.words))
                            .putExtra(QuizActivity.EXTRA_TITLE, sheet.title)
                    )
                    1 -> exportPdf(sheet, PdfExporter.Type.HIGHLIGHT)
                    2 -> exportPdf(sheet, PdfExporter.Type.BLANK)
                    3 -> confirmDelete(sheet)
                }
            }
            .show()
    }

    private fun exportPdf(sheet: StudySheet, type: PdfExporter.Type) {
        val uri = PdfExporter.export(this, sheet.words, type)
        if (uri == null) {
            Toast.makeText(this, "선택된 단어가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(PdfExporter.viewIntent(uri))
    }

    private fun confirmDelete(sheet: StudySheet) {
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("'${sheet.title}' 학습지를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                StudyRepository.delete(this, sheet.id)
                reload()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    inner class SheetAdapter : RecyclerView.Adapter<SheetAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.txtSheetTitle)
            val meta: TextView = view.findViewById(R.id.txtSheetMeta)
            val preview: TextView = view.findViewById(R.id.txtSheetPreview)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_sheet, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sheet = sheets[position]
            holder.title.text = sheet.title
            val date = DateFormat.format("yyyy.MM.dd HH:mm", sheet.createdAt)
            holder.meta.text = "$date · 빈칸 ${sheet.blankCount}개"
            holder.preview.text = sheet.preview
            holder.itemView.setOnClickListener { showOptions(sheet) }
        }

        override fun getItemCount(): Int = sheets.size
    }
}
