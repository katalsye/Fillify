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
 * 빈칸을 탭하면 정답 확인 → O(맞음) → X(틀림) 순으로 순환하며 자가 채점한다.
 */
class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORDS_JSON = "extra_words_json"
        const val EXTRA_TITLE = "extra_title"
    }

    /** 빈칸 상태: 가림 → 정답확인 → 맞음 → 틀림 (탭할 때마다 순환) */
    private enum class BlankState { HIDDEN, REVEALED, CORRECT, WRONG }

    private lateinit var txtTitle: TextView
    private lateinit var txtProgress: TextView
    private lateinit var txtPassage: TextView
    private lateinit var btnRevealAll: Button
    private lateinit var btnReset: Button
    private lateinit var btnRetryWrong: Button

    private val words = mutableListOf<StudyWord>()

    /** 빈칸(선택 단어)의 words 인덱스별 상태 */
    private val blankIndices = mutableListOf<Int>()
    private val states = mutableMapOf<Int, BlankState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        txtTitle = findViewById(R.id.txtQuizTitle)
        txtProgress = findViewById(R.id.txtQuizProgress)
        txtPassage = findViewById(R.id.txtPassage)
        btnRevealAll = findViewById(R.id.btnRevealAll)
        btnReset = findViewById(R.id.btnReset)
        btnRetryWrong = findViewById(R.id.btnRetryWrong)

        txtPassage.movementMethod = LinkMovementMethod.getInstance()

        val json = intent.getStringExtra(EXTRA_WORDS_JSON)
        val type = object : TypeToken<List<StudyWord>>() {}.type
        val loaded: List<StudyWord> = runCatching {
            Gson().fromJson<List<StudyWord>>(json, type)
        }.getOrNull() ?: emptyList()
        words.addAll(loaded)

        words.forEachIndexed { i, w -> if (w.selected && !w.isLineBreak) blankIndices.add(i) }
        blankIndices.forEach { states[it] = BlankState.HIDDEN }

        txtTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "학습"

        btnRevealAll.setOnClickListener { revealAll() }
        btnReset.setOnClickListener { setAll(BlankState.HIDDEN) }
        btnRetryWrong.setOnClickListener { retryWrong() }

        render()
    }

    /** 탭 한 번: 다음 상태로 순환 */
    private fun cycle(index: Int) {
        states[index] = when (states[index]) {
            BlankState.HIDDEN -> BlankState.REVEALED
            BlankState.REVEALED -> BlankState.CORRECT
            BlankState.CORRECT -> BlankState.WRONG
            else -> BlankState.HIDDEN
        }
        render()
    }

    /** 아직 가려진 빈칸만 정답 공개 (채점 결과는 유지) */
    private fun revealAll() {
        blankIndices.forEach { if (states[it] == BlankState.HIDDEN) states[it] = BlankState.REVEALED }
        render()
    }

    private fun setAll(state: BlankState) {
        blankIndices.forEach { states[it] = state }
        render()
    }

    /** 틀린 빈칸만 다시 가려서 재도전 */
    private fun retryWrong() {
        blankIndices.forEach { if (states[it] == BlankState.WRONG) states[it] = BlankState.HIDDEN }
        render()
    }

    private fun bgColorFor(state: BlankState): Int = ContextCompat.getColor(
        this,
        when (state) {
            BlankState.CORRECT -> R.color.grade_correct
            BlankState.WRONG -> R.color.grade_wrong
            else -> R.color.highlight
        }
    )

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
                val state = states[index] ?: BlankState.HIDDEN
                val shown = state != BlankState.HIDDEN
                val display =
                    if (shown) word.text else "＿".repeat(word.text.length.coerceAtLeast(2))
                val start = builder.length
                builder.append(display)
                val end = builder.length

                // 상태별 배경색 (가림/확인=노랑, 맞음=초록, 틀림=빨강)
                builder.setSpan(
                    BackgroundColorSpan(bgColorFor(state)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (shown) {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                builder.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = cycle(index)

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

        val total = blankIndices.size
        val correct = states.values.count { it == BlankState.CORRECT }
        val wrong = states.values.count { it == BlankState.WRONG }
        txtProgress.text = "맞음 ${correct} · 틀림 ${wrong} · 전체 ${total}"

        val allShown = total > 0 && blankIndices.none { states[it] == BlankState.HIDDEN }
        btnRevealAll.isEnabled = !allShown
        btnRetryWrong.visibility = if (wrong > 0) View.VISIBLE else View.GONE
    }
}
