package com.ai.assist.documents

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PptxDocumentRenderer {
    fun render(outline: DocumentOutline, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypes(outline.slides.size))
            zip.writeEntry("_rels/.rels", rootRelationships())
            zip.writeEntry("ppt/presentation.xml", presentation(outline.slides.size))
            zip.writeEntry("ppt/_rels/presentation.xml.rels", presentationRelationships(outline.slides.size))
            outline.slides.forEachIndexed { index, slide ->
                zip.writeEntry("ppt/slides/slide${index + 1}.xml", slideXml(outline, slide, index))
            }
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(slideCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>""")
        for (index in 1..slideCount) {
            append("""<Override PartName="/ppt/slides/slide$index.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
        }
        append("</Types>")
    }

    private fun rootRelationships(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
        </Relationships>
    """.trimIndent()

    private fun presentation(slideCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">""")
        append("<p:sldIdLst>")
        for (index in 1..slideCount) {
            append("""<p:sldId id="${255 + index}" r:id="rId$index"/>""")
        }
        append("""</p:sldIdLst><p:sldSz cx="12192000" cy="6858000" type="wide"/><p:notesSz cx="6858000" cy="9144000"/></p:presentation>""")
    }

    private fun presentationRelationships(slideCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (index in 1..slideCount) {
            append("""<Relationship Id="rId$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$index.xml"/>""")
        }
        append("</Relationships>")
    }

    private fun slideXml(outline: DocumentOutline, slide: SlideOutline, index: Int): String {
        val title = if (index == 0) outline.title else slide.title
        val bullets = if (index == 0 && outline.subtitle.isNotBlank()) {
            listOf(outline.subtitle) + slide.bullets
        } else {
            slide.bullets
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld>
                <p:spTree>
                  <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
                  <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
                  ${textShape(2, "Title", 600000, 650000, 11000000, 900000, title, 3600, false)}
                  ${textShape(3, "Bullets", 850000, 1900000, 10500000, 3900000, bullets.joinToString("\n"), 2200, true)}
                </p:spTree>
              </p:cSld>
              <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
            </p:sld>
        """.trimIndent()
    }

    private fun textShape(
        id: Int,
        name: String,
        x: Int,
        y: Int,
        cx: Int,
        cy: Int,
        text: String,
        size: Int,
        bullets: Boolean,
    ): String {
        val paragraphs = text.split('\n').filter { it.isNotBlank() }.joinToString("") { line ->
            val prefix = if (bullets) """<a:buChar char="•"/>""" else "<a:buNone/>"
            """<a:p><a:pPr>$prefix</a:pPr><a:r><a:rPr lang="ko-KR" sz="$size"/><a:t>${xml(line)}</a:t></a:r></a:p>"""
        }
        return """
            <p:sp>
              <p:nvSpPr><p:cNvPr id="$id" name="$name"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
              <p:spPr><a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
              <p:txBody><a:bodyPr wrap="square"/><a:lstStyle/>$paragraphs</p:txBody>
            </p:sp>
        """.trimIndent()
    }

    private fun xml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
