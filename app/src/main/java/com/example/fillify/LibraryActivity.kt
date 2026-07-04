package com.example.fillify

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        recycler.adapter = SheetAdapter(sheets) { sheet ->
            SheetActions.showOptions(this, sheet) { reload() }
        }
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
}
