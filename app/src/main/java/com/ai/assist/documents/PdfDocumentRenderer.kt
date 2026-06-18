package com.ai.assist.documents

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

class PdfDocumentRenderer {
    fun render(outline: DocumentOutline, outputFile: File) {
        val document = PdfDocument()
        try {
            outline.slides.forEachIndexed { index, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(1280, 720, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 44f
                    isFakeBoldText = true
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 28f
                }
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 18f
                    alpha = 160
                }
                canvas.drawText(if (index == 0) outline.title else slide.title, 72f, 110f, titlePaint)
                if (index == 0 && outline.subtitle.isNotBlank()) {
                    canvas.drawText(outline.subtitle, 72f, 160f, bodyPaint)
                }
                var y = if (index == 0) 250f else 180f
                slide.bullets.take(6).forEach { bullet ->
                    canvas.drawText("- ${bullet.take(82)}", 96f, y, bodyPaint)
                    y += 54f
                }
                canvas.drawText("AI Assist Lab", 72f, 670f, footerPaint)
                canvas.drawText("${index + 1} / ${outline.slides.size}", 1140f, 670f, footerPaint)
                document.finishPage(page)
            }
            FileOutputStream(outputFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }
}
