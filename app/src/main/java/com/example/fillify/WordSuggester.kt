package com.example.fillify

/**
 * 조사 기반 휴리스틱으로 빈칸 후보(핵심어)를 추천한다.
 *
 * 한국어는 교착어라 명사 뒤에 조사가 붙는다(세포막은, 에너지를). 형태소 분석기 없이,
 * 닫힌 집합인 조사 목록과 서술어 어미 배제로 명사(체언)를 근사 추정한다.
 * 완벽하지 않으므로(예: 많이/같이 같은 부사 오탐) 사용자가 탭으로 조정하는 것을 전제로 한다.
 */
object WordSuggester {

    /** 명사 뒤에 붙는 대표 조사 (주격·목적격·부사격 등). 정밀도를 위해 보조사 일부는 제외. */
    private val particles = listOf(
        "에서", "에게", "께서", "한테", "부터", "까지", "보다", "처럼", "마다", "으로",
        "은", "는", "이", "가", "을", "를", "의", "와", "과", "로", "에"
    )

    /** 서술어(동사·형용사) 어미 — 이 어미로 끝나면 명사에서 제외 */
    private val predicateEndings = listOf("다", "요", "죠", "까")

    /** 해당 단어가 빈칸으로 삼을 만한 핵심어(명사 후보)인지 */
    fun isKeyword(text: String): Boolean {
        if (text.length < 2) return false
        if (predicateEndings.any { text.endsWith(it) }) return false
        // 조사보다 앞선 어간이 최소 한 글자 이상 남아야 함
        return particles.any { text.length > it.length && text.endsWith(it) }
    }

    /** OCR 결과에서 핵심어로 보이는 단어를 selected=true 로 표시하고, 추천 개수를 반환 */
    fun apply(words: List<StudyWord>): Int {
        var count = 0
        words.forEach { w ->
            if (!w.isLineBreak) {
                w.selected = isKeyword(w.text)
                if (w.selected) count++
            }
        }
        return count
    }
}
