package uk.co.gazai

import android.content.Context
import android.text.Html
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.zip.ZipInputStream

data class ImportedDocument(val name: String, val text: String, val fileName: String)

object UserLibrary {
    private fun folder(context: Context) = File(context.filesDir, "knowledge-library").apply { mkdirs() }

    fun saveImported(context: Context, displayName: String, mimeType: String?, bytes: ByteArray): ImportedDocument {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val text = when (extension) {
            "txt", "md", "markdown", "csv", "json", "xml" -> bytes.toString(Charsets.UTF_8)
            "html", "htm" -> Html.fromHtml(bytes.toString(Charsets.UTF_8), Html.FROM_HTML_MODE_COMPACT).toString()
            "epub" -> extractEpub(bytes)
            "pdf" -> extractPdf(context, bytes)
            else -> throw IllegalArgumentException("Supported files: PDF, EPUB, TXT, MD, HTML, CSV and JSON.")
        }.trim()
        if (text.isBlank()) throw IllegalArgumentException("No readable text was found in this file.")
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "document" }
        val target = File(folder(context), "${System.currentTimeMillis()}_$safeName.txt")
        target.writeText(text, Charsets.UTF_8)
        return ImportedDocument(displayName, text, target.name)
    }

    fun load(context: Context): List<ImportedDocument> =
        folder(context).listFiles()?.filter { it.isFile && it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching { ImportedDocument(file.name.substringAfter('_'), file.readText(), file.name) }.getOrNull()
            } ?: emptyList()

    fun promptContext(context: Context, query: String, limit: Int = 3): String {
        val terms = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 2 }.toSet()
        if (terms.isEmpty()) return ""
        return load(context).map { doc ->
            val lower = doc.text.lowercase()
            val score = terms.sumOf { term -> lower.windowed(term.length).count { it == term }.coerceAtMost(8) }
            doc to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit)
            .joinToString("\n\n") { (doc, _) ->
                "[Imported library: ${doc.name}]\n${doc.text.take(5000)}"
            }
    }

    private fun extractPdf(context: Context, bytes: ByteArray): String {
        PDFBoxResourceLoader.init(context)
        PDDocument.load(bytes).use { document -> return PDFTextStripper().getText(document) }
    }

    private fun extractEpub(bytes: ByteArray): String {
        val output = StringBuilder()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.lowercase().let {
                    it.endsWith(".xhtml") || it.endsWith(".html") || it.endsWith(".htm")
                }) {
                    output.append(Html.fromHtml(zip.readBytes().toString(Charsets.UTF_8), Html.FROM_HTML_MODE_COMPACT))
                        .append("\n\n")
                }
                zip.closeEntry()
            }
        }
        return output.toString()
    }
}
