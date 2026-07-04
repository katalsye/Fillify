package com.example.fillify

/**
 * OCR로 추출된 단어 하나. 줄바꿈은 text == "\n" 으로 표현한다.
 * selected == true 이면 빈칸/형광펜 대상이다.
 */
data class StudyWord(
    val text: String,
    var selected: Boolean = false
) {
    val isLineBreak: Boolean get() = text == "\n"
}

/**
 * 저장되는 학습지 한 장. 라이브러리·퀴즈·PDF가 공유하는 단위.
 */
data class StudySheet(
    val id: String,
    val title: String,
    val createdAt: Long,
    val words: List<StudyWord>
) {
    /** 빈칸(정답) 개수 */
    val blankCount: Int get() = words.count { it.selected }

    /** 미리보기용 본문 요약 (줄바꿈 제거, 앞부분만) */
    val preview: String
        get() = words.filterNot { it.isLineBreak }
            .joinToString(" ") { it.text }
            .take(60)
}
