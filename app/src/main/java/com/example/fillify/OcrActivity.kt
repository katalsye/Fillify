package com.example.fillify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File

class OcrActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var txtImageHint: TextView
    private lateinit var txtStatus: TextView
    private lateinit var recyclerWords: RecyclerView
    private lateinit var layoutActions: View
    private lateinit var txtSelectedCount: TextView

    /** 줄바꿈 마커 포함 전체 단어 (PDF·퀴즈용) */
    private val words = mutableListOf<StudyWord>()

    /** 칩으로 표시하는 단어 (줄바꿈 제외, words와 같은 객체를 공유) */
    private val displayWords = mutableListOf<StudyWord>()

    private var cameraImageUri: Uri? = null

    private val recognizer by lazy {
        TextRecognition.getClient(
            KoreanTextRecognizerOptions.Builder().build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        imageView = findViewById(R.id.imageView)
        txtImageHint = findViewById(R.id.txtImageHint)
        txtStatus = findViewById(R.id.txtStatus)
        recyclerWords = findViewById(R.id.recyclerWords)
        layoutActions = findViewById(R.id.layoutActions)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)

        recyclerWords.layoutManager = FlexboxLayoutManager(this).apply {
            flexWrap = FlexWrap.WRAP
        }
        recyclerWords.adapter = WordAdapter()

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener { openGallery() }
        findViewById<Button>(R.id.btnCapture).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveSheet() }
        findViewById<Button>(R.id.btnStudy).setOnClickListener { startQuiz() }
        findViewById<Button>(R.id.btnHighlightPdf).setOnClickListener {
            exportPdf(PdfExporter.Type.HIGHLIGHT)
        }
        findViewById<Button>(R.id.btnBlankPdf).setOnClickListener {
            exportPdf(PdfExporter.Type.BLANK)
        }
    }

    // ---------- 이미지 입력 ----------

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                onImageReady(uri)
            }
        }

    private fun openGallery() {
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        )
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraImageUri
            if (success && uri != null) onImageReady(uri)
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }

    private fun openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun onImageReady(uri: Uri) {
        imageView.setImageURI(uri)
        txtImageHint.visibility = View.GONE
        txtStatus.text = "글자 인식 중..."
        runOcr(uri)
    }

    private fun runOcr(uri: Uri) {
        words.clear()
        displayWords.clear()
        layoutActions.visibility = View.GONE
        recyclerWords.adapter?.notifyDataSetChanged()

        val image = InputImage.fromFilePath(this, uri)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                visionText.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { element ->
                            words.add(StudyWord(element.text))
                        }
                        words.add(StudyWord("\n"))
                    }
                }
                displayWords.addAll(words.filterNot { it.isLineBreak })
                recyclerWords.adapter?.notifyDataSetChanged()
                txtStatus.text =
                    if (displayWords.isNotEmpty()) "단어를 탭해 빈칸으로 만들 부분을 선택하세요"
                    else "인식된 글자가 없습니다. 다른 사진을 시도해 보세요"
            }
            .addOnFailureListener {
                txtStatus.text = "인식에 실패했습니다: ${it.message}"
            }
    }

    // ---------- 액션 ----------

    private fun refreshActions() {
        val count = words.count { it.selected }
        layoutActions.visibility = if (count > 0) View.VISIBLE else View.GONE
        txtSelectedCount.text = "선택한 단어 ${count}개"
    }

    private fun saveSheet() {
        val input = EditText(this).apply { hint = "학습지 제목" }
        AlertDialog.Builder(this)
            .setTitle("학습지 저장")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                StudyRepository.create(this, input.text.toString(), words)
                Toast.makeText(this, "저장했습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun startQuiz() {
        val json = Gson().toJson(words)
        startActivity(
            Intent(this, QuizActivity::class.java)
                .putExtra(QuizActivity.EXTRA_WORDS_JSON, json)
                .putExtra(QuizActivity.EXTRA_TITLE, "학습")
        )
    }

    private fun exportPdf(type: PdfExporter.Type) {
        val uri = PdfExporter.export(this, words, type)
        if (uri == null) {
            Toast.makeText(this, "선택한 단어가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(PdfExporter.viewIntent(uri))
    }

    // ---------- 단어 리스트 ----------

    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.VH>() {

        inner class VH(val txt: TextView) : RecyclerView.ViewHolder(txt)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_word, parent, false) as TextView
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = displayWords[position]
            holder.txt.text = word.text
            holder.txt.isSelected = word.selected
            holder.txt.setOnClickListener {
                word.selected = !word.selected
                notifyItemChanged(position)
                refreshActions()
            }
        }

        override fun getItemCount(): Int = displayWords.size
    }
}
