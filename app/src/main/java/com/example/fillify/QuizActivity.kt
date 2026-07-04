package com.example.fillify

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 앱 내 빈칸 채우기 학습 모드.
 * 선택된 단어는 빈칸으로 가려지고, 탭하면 정답이 공개된다.
 */
class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORDS_JSON = "extra_words_json"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var txtTitle: TextView
    private lateinit var txtProgress: TextView
    private lateinit var txtPassage: TextView
    private lateinit var btnToggleAll: Button

    private val words = mutableListOf<StudyWord>()

    /** 빈칸(선택된 단어)의 words 인덱스 목록과 공개 여부 */
    private val blankIndices = mutableListOf<Int>()
    private val revealed = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        txtTitle = findViewById(R.id.txtQuizTitle)
        txtProgress = findViewById(R.id.txtQuizProgress)
        txtPassage = findViewById(R.id.txtPassage)
        btnToggleAll = findViewById(R.id.btnToggleAll)

        txtPassage.movementMethod = LinkMovementMethod.getInstance()

        val json = intent.getStringExtra(EXTRA_WORDS_JSON)
        val type = object : TypeToken<List<StudyWord>>() {}.type
        val loaded: List<StudyWord> = runCatching {
            Gson().fromJson<List<StudyWord>>(json, type)
        }.getOrNull() ?: emptyList()
        words.addAll(loaded)

        words.forEachIndexed { i, w -> if (w.selected && !w.isLineBreak) blankIndices.add(i) }
        blankIndices.forEach { revealed[it] = false }

        txtTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "학습"

        btnToggleAll.setOnClickListener { toggleAll() }

        render()
    }

    private fun toggleAll() {
        val allShown = blankIndices.all { revealed[it] == true }
        blankIndices.forEach { revealed[it] = !allShown }
        render()
    }

    private fun render() {
        val builder = SpannableStringBuilder()
        var firstLineEnd = -1

        for ((index, word) in words.withIndex()) {
            if (word.isLineBreak) {
                if (firstLineEnd == -1) firstLineEnd = builder.length
                builder.append("\n")
                continue
            }

            if (word.selected) {
                val isRevealed = revealed[index] == true
                val display = if (isRevealed) word.text else "＿".repeat(word.text.length.coerceAtLeast(2))
                val start = builder.length
                builder.append(display)
                val end = builder.length

                // 빈칸 강조 (형광 배경) — 밑줄은 ＿ 글자 한 줄로만 표시
                builder.setSpan(
                    BackgroundColorSpan(ContextCompat.getColor(this, R.color.highlight)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isRevealed) {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                builder.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        revealed[index] = !(revealed[index] ?: false)
                        render()
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        ds.color = Color.BLACK
                        ds.isUnderlineText = false
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                builder.append(word.text)
            }
            builder.append(" ")
        }

        // 첫 줄(제목) 볼드
        if (firstLineEnd == -1) firstLineEnd = builder.length
        if (firstLineEnd > 0) {
            builder.setSpan(
                StyleSpan(Typeface.BOLD), 0, firstLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        txtPassage.text = builder

        val shown = blankIndices.count { revealed[it] == true }
        val total = blankIndices.size
        txtProgress.text = "정답 ${total}개 중 ${shown}개 확인"
        btnToggleAll.text =
            if (total > 0 && blankIndices.all { revealed[it] == true }) "모두 가리기" else "모두 보기"
    }
}
