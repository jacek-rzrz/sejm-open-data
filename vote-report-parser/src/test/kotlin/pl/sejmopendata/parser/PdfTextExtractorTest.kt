package pl.sejmopendata.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.sejmopendata.testutils.loadBytes

class PdfTextExtractorTest {

    @Test
    fun `reads all text content`() {
        val pdfFile = "kadencja9_posiedzenie27_glosowanie15.pdf".loadBytes()
        val lines = PdfTextExtractor().extractLines(pdfFile)

        assertThat(lines).isNotEmpty
        assertThat(lines[0]).startsWith(" 1")
        assertThat(lines[1]).startsWith("Sejm RP 9 kadencji ")
        assertThat(lines.last()).startsWith("GALLA RYSZARD za MEJZA ŁUKASZ pr. KOŁAKOWSKI LECH ws. ŚCIGAJ AGNIESZKA ws. ")
    }
}
