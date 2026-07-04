package com.example.fillify

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import java.text.Collator
import java.util.Locale

/** 저장된 학습지 목록 — 제목 검색과 정렬 지원. 탭하면 학습/편집/PDF/삭제 옵션. */
class LibraryActivity : AppCompatActivity() {

    private enum class Sort { NEWEST, OLDEST, TITLE, BLANKS }

    private lateinit var recycler: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var editSearch: EditText
    private lateinit var chipSort: ChipGroup

    private val allSheets = mutableListOf<StudySheet>()
    private val shown = mutableListOf<StudySheet>()

    private var query: String = ""
    private var sort: Sort = Sort.NEWEST

    private val korCollator = Collator.getInstance(Locale.KOREAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recycler = findViewById(R.id.recyclerSheets)
        txtEmpty = findViewById(R.id.txtEmpty)
        editSearch = findViewById(R.id.editSearch)
        chipSort = findViewById(R.id.chipSort)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = SheetAdapter(shown) { sheet ->
            SheetActions.showOptions(this, sheet) { reload() }
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                query = s?.toString()?.trim().orEmpty()
                applyFilterSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipSort.setOnCheckedStateChangeListener { _, checkedIds ->
            sort = when (checkedIds.firstOrNull()) {
                R.id.chipOldest -> Sort.OLDEST
                R.id.chipTitle -> Sort.TITLE
                R.id.chipBlanks -> Sort.BLANKS
                else -> Sort.NEWEST
            }
            applyFilterSort()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        allSheets.clear()
        allSheets.addAll(StudyRepository.loadAll(this))
        applyFilterSort()
    }

    private fun applyFilterSort() {
        val filtered =
            if (query.isEmpty()) allSheets
            else allSheets.filter { it.title.contains(query, ignoreCase = true) }

        val sorted = when (sort) {
            Sort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            Sort.OLDEST -> filtered.sortedBy { it.createdAt }
            Sort.TITLE -> filtered.sortedWith(compareBy(korCollator) { it.title })
            Sort.BLANKS -> filtered.sortedByDescending { it.blankCount }
        }

        shown.clear()
        shown.addAll(sorted)
        recycler.adapter?.notifyDataSetChanged()

        when {
            allSheets.isEmpty() -> {
                txtEmpty.text = "저장된 학습지가 없습니다.\n새 학습지를 만들어 보세요."
                txtEmpty.visibility = View.VISIBLE
            }
            shown.isEmpty() -> {
                txtEmpty.text = "'${query}' 검색 결과가 없습니다."
                txtEmpty.visibility = View.VISIBLE
            }
            else -> txtEmpty.visibility = View.GONE
        }
    }
}
