package com.example.fillify

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

/**
 * 학습지를 앱 내부 저장소(JSON 파일)에 보관하는 단순 저장소.
 * 외부 DB 없이 filesDir/study_sheets.json 하나에 전체 목록을 직렬화한다.
 */
object StudyRepository {

    private const val FILE_NAME = "study_sheets.json"
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<StudySheet>>() {}.type

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** 최신순으로 정렬된 전체 학습지 */
    fun loadAll(context: Context): MutableList<StudySheet> {
        val f = file(context)
        if (!f.exists()) return mutableListOf()
        return runCatching {
            gson.fromJson<MutableList<StudySheet>>(f.readText(), listType) ?: mutableListOf()
        }.getOrDefault(mutableListOf())
            .sortedByDescending { it.createdAt }
            .toMutableList()
    }

    fun get(context: Context, id: String): StudySheet? =
        loadAll(context).firstOrNull { it.id == id }

    /** 새 학습지 생성 후 저장하고 반환 */
    fun create(context: Context, title: String, words: List<StudyWord>): StudySheet {
        val sheet = StudySheet(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "제목 없음" },
            createdAt = System.currentTimeMillis(),
            words = words.map { StudyWord(it.text, it.selected) }
        )
        val all = loadAll(context)
        all.add(sheet)
        persist(context, all)
        return sheet
    }

    /** 기존 학습지의 제목·단어를 갱신한다. id·생성시각은 유지. */
    fun update(context: Context, id: String, title: String, words: List<StudyWord>) {
        val all = loadAll(context)
        val idx = all.indexOfFirst { it.id == id }
        if (idx == -1) return
        val old = all[idx]
        all[idx] = old.copy(
            title = title.ifBlank { old.title },
            words = words.map { StudyWord(it.text, it.selected) }
        )
        persist(context, all)
    }

    fun delete(context: Context, id: String) {
        val all = loadAll(context).filterNot { it.id == id }.toMutableList()
        persist(context, all)
    }

    private fun persist(context: Context, sheets: List<StudySheet>) {
        file(context).writeText(gson.toJson(sheets, listType))
    }
}
