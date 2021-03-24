package pl.sejmopendata.parser

import pl.sejmopendata.VoteReport

interface VoteResultsPdfParser {
    fun parse(pdfContent: ByteArray): VoteReport
}
