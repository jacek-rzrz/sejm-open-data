package pl.sejmopendata.parser

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

class PdfTextExtractor {

    fun extractLines(pdfContent: ByteArray): List<String> {
        val buffer = ByteArrayOutputStream()
        PDDocument.load(pdfContent).use { document ->
            OutputStreamWriter(buffer).use { writer ->
                PDFTextStripper().writeText(document, writer)
            }
        }

        return String(buffer.toByteArray())
            .lines()
            .dropLastWhile { it.isBlank() }
    }
}
