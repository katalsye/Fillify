package com.example.fillify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var layoutRecent: View
    private lateinit var recyclerRecent: RecyclerView
    private val recentSheets = mutableListOf<StudySheet>()

    private companion object {
        const val RECENT_LIMIT = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutRecent = findViewById(R.id.layoutRecent)
        recyclerRecent = findViewById(R.id.recyclerRecent)
        recyclerRecent.layoutManager = LinearLayoutManager(this)
        recyclerRecent.adapter = SheetAdapter(recentSheets) { sheet ->
            SheetActions.showOptions(this, sheet) { loadRecent() }
        }

        findViewById<Button>(R.id.btnStartOcr).setOnClickListener {
            startActivity(Intent(this, OcrActivity::class.java))
        }
        findViewById<Button>(R.id.btnLibrary).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        findViewById<Button>(R.id.btnSeeAll).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecent()
    }

    private fun loadRecent() {
        val all = StudyRepository.loadAll(this)
        recentSheets.clear()
        recentSheets.addAll(all.take(RECENT_LIMIT))
        recyclerRecent.adapter?.notifyDataSetChanged()
        layoutRecent.visibility = if (recentSheets.isEmpty()) View.GONE else View.VISIBLE
    }
}
