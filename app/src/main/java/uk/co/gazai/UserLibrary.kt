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

    fun promptContext(context: Context, query: String, limit: Int = 4): String {
        val normalized = query.lowercase().trim()
        val terms = normalized.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }.toSet()
        if (terms.isEmpty()) return ""
        data class Match(val doc: ImportedDocument, val excerpt: String, val score: Int)
        val matches = mutableListOf<Match>()
        load(context).forEach { doc ->
            val paragraphs = doc.text.split(Regex("\\n\\s*\\n|(?<=[.!?])\\s+(?=[A-Z0-9])"))
                .filter { it.isNotBlank() }
            (if (paragraphs.isEmpty()) listOf(doc.text) else paragraphs).forEach { paragraph ->
                val lower = paragraph.lowercase()
                val hits = terms.count { lower.contains(it) }
                if (hits > 0) {
                    val phraseBonus = if (normalized.length > 5 && lower.contains(normalized)) 8 else 0
                    matches.add(Match(doc, paragraph.trim().take(1100), hits * 3 + phraseBonus))
                }
            }
        }
        return matches.sortedByDescending { it.score }.take(limit)
            .joinToString("\\n\\n") { match ->
                "[Imported library: ${match.doc.name}]\\n${match.excerpt}"
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
