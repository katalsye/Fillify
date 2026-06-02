package com.example.fillify

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

data class OcrWord(
    val text: String,
    var selected: Boolean = false
)

class OcrActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var txtStatus: TextView
    private lateinit var recyclerWords: RecyclerView
    private lateinit var layoutPdfButtons: LinearLayout
    private lateinit var btnHighlightPdf: Button
    private lateinit var btnBlankPdf: Button

    private val words = mutableListOf<OcrWord>()

    private val recognizer by lazy {
        TextRecognition.getClient(
            KoreanTextRecognizerOptions.Builder().build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        imageView = findViewById(R.id.imageView)
        txtStatus = findViewById(R.id.txtStatus)
        recyclerWords = findViewById(R.id.recyclerWords)
        layoutPdfButtons = findViewById(R.id.layoutPdfButtons)
        btnHighlightPdf = findViewById(R.id.btnHighlightPdf)
        btnBlankPdf = findViewById(R.id.btnBlankPdf)

        recyclerWords.layoutManager = GridLayoutManager(this, 4)
        recyclerWords.adapter = WordAdapter()

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            openGallery()
        }

        btnHighlightPdf.setOnClickListener {
            exportPdf("highlight")
        }

        btnBlankPdf.setOnClickListener {
            exportPdf("blank")
        }
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                imageView.setImageURI(uri)
                txtStatus.text = "사진 선택 완료"
                runOcr(uri)
            }
        }

    private fun openGallery() {
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        )
    }

    private fun runOcr(uri: Uri) {
        words.clear()
        layoutPdfButtons.visibility = View.GONE
        txtStatus.text = "OCR 처리 중..."

        val image = InputImage.fromFilePath(this, uri)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                visionText.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { element ->
                            words.add(OcrWord(element.text))
                        }
                        words.add(OcrWord("\n"))
                    }
                }
                txtStatus.text = "인식 완료: ${words.count { it.text != "\n" }}개 단어"
                recyclerWords.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                txtStatus.text = "OCR 실패: ${e.localizedMessage}"
            }
    }

    private fun exportPdf(type: String) {
        if (words.none { it.selected }) return

        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply { textSize = 14f }
        val highlightPaint = Paint().apply {
            color = 0xFFFFFF99.toInt()
            style = Paint.Style.FILL
        }

        val marginLeft = 40f
        val marginRight = 520f
        val marginTop = 40f
        val lineHeight = 30f
        val bottomLimit = 800f
        val space = textPaint.measureText(" ")

        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas: Canvas = page.canvas
        var x = marginLeft
        var y = marginTop

        fun newPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            x = marginLeft
            y = marginTop
        }

        for (word in words) {
            if (word.text == "\n") {
                x = marginLeft
                y += lineHeight
                if (y > bottomLimit) newPage()
                continue
            }

            val drawText = if (type == "blank" && word.selected) "_____" else word.text
            val width = textPaint.measureText(drawText)

            if (x + width > marginRight) {
                x = marginLeft
                y += lineHeight
                if (y > bottomLimit) newPage()
            }

            if (type == "highlight" && word.selected) {
                canvas.drawRect(x, y - textPaint.textSize, x + width, y + 5, highlightPaint)
            }

            canvas.drawText(drawText, x, y, textPaint)

            if (type == "blank" && word.selected) {
                canvas.drawLine(x, y + 4, x + width, y + 4, textPaint)
            }

            x += width + space
        }

        pdfDocument.finishPage(page)

        val fileName = "${type}_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            values
        ) ?: return

        contentResolver.openOutputStream(uri)?.use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "PDF 열기"
            )
        )
    }

    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.VH>() {

        inner class VH(val txt: TextView) : RecyclerView.ViewHolder(txt)

        private val displayWords get() = words.filter { it.text != "\n" }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_word, parent, false) as TextView
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = displayWords[position]
            holder.txt.text = word.text
            holder.txt.setBackgroundColor(
                if (word.selected) 0xFFFFFF99.toInt() else 0x00000000
            )
            holder.txt.setOnClickListener {
                word.selected = !word.selected
                notifyItemChanged(position)
                layoutPdfButtons.visibility =
                    if (words.any { it.selected }) View.VISIBLE else View.GONE
            }
        }

        override fun getItemCount(): Int = displayWords.size
    }
}
