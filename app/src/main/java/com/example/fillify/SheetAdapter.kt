package com.example.fillify

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** 학습지 카드 목록 어댑터 (홈·라이브러리 공용). item_sheet 를 바인딩한다. */
class SheetAdapter(
    private val sheets: List<StudySheet>,
    private val onClick: (StudySheet) -> Unit
) : RecyclerView.Adapter<SheetAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtSheetTitle)
        val meta: TextView = view.findViewById(R.id.txtSheetMeta)
        val preview: TextView = view.findViewById(R.id.txtSheetPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sheet, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sheet = sheets[position]
        holder.title.text = sheet.title
        val date = DateFormat.format("yyyy.MM.dd HH:mm", sheet.createdAt)
        holder.meta.text = "$date · 빈칸 ${sheet.blankCount}개"
        holder.preview.text = sheet.preview
        holder.itemView.setOnClickListener { onClick(sheet) }
    }

    override fun getItemCount(): Int = sheets.size
}
