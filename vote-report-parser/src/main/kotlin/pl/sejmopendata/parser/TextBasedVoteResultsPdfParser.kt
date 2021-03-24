package pl.sejmopendata.parser

import pl.sejmopendata.*
import java.time.LocalDateTime

class TextBasedVoteResultsPdfParser : VoteResultsPdfParser {

    override fun parse(pdfContent: ByteArray): VoteReport {
        return PdfTextExtractor()
            .extractLines(pdfContent)
            .filter(String::isNotBlank)
            .map(Line.Companion::parse)
            .fold(ParserState(), ParserState::readLine)
            .buildReport()
    }
}

sealed class ParserState {
    fun readLine(line: Line): ParserState {
        return doReadLine(line)
    }

    protected abstract fun doReadLine(line: Line): ParserState

    abstract fun buildReport(): VoteReport

    companion object {
        operator fun invoke(): ParserState = ExpectKadencja()
    }
}

class ExpectKadencja : ParserState() {

    override fun doReadLine(line: Line): ParserState {
        return when (line) {
            is TermLine -> ExpectVoteId(line.term)
            else -> this
        }
    }

    override fun buildReport(): VoteReport {
        throw Exception("Kadencja not found")
    }
}

class ExpectVoteId(private val kadencja: Int) : ParserState() {
    override fun doReadLine(line: Line): ParserState {
        return when (line) {
            is VoteIdLine -> ExpectSummary(
                id = VoteId(
                    termNumber = kadencja,
                    sessionNumber = line.sessionNumber,
                    voteNumber = line.voteNumber,
                ),
                timestamp = line.timestamp,
            )
            else -> this
        }
    }

    override fun buildReport(): VoteReport {
        throw Exception("Posiedzenie, glosowanie and timestamp not found")
    }
}

class ExpectSummary(
    private val id: VoteId,
    private val timestamp: LocalDateTime,
) : ParserState() {

    override fun doReadLine(line: Line): ParserState {
        return when (line) {
            is SummaryLine -> ExpectDescription(
                id = id,
                timestamp = timestamp,
                summary = line.toSummary(),
            )
            else -> throw Exception("Summary line expected, found: $line")
        }
    }

    override fun buildReport(): VoteReport {
        throw Exception("EOF when expecting summary")
    }
}

data class ExpectDescription(
    private val id: VoteId,
    private val timestamp: LocalDateTime,
    private val summary: VoteSummary,
    private val description: List<String> = emptyList(),
) : ParserState() {
    override fun doReadLine(line: Line): ParserState {
        return when (line) {
            is FreeTextLine -> copy(description = description + line.text)
            is SummaryLine -> ExpectDetailedVoteData(
                id = id,
                timestamp = timestamp,
                summary = summary,
                description = description.joinToString(separator = "\n"),
                parties = emptyList(),
                currentParty = line.toPartyReport(),
                currentPartyVotesBuilder = MpVotesBuilder()
            )
            else -> throw Exception("Description or summary line expected, found $line")
        }
    }

    override fun buildReport(): VoteReport {
        throw Exception("EOF while expecting vote data")
    }
}

data class ExpectDetailedVoteData(
    private val id: VoteId,
    private val timestamp: LocalDateTime,
    private val summary: VoteSummary,
    private val description: String,
    private val parties: List<PartyVoteReport>,
    private val currentParty: PartyVoteReport,
    private val currentPartyVotesBuilder: MpVotesBuilder,
) : ParserState() {

    override fun doReadLine(line: Line): ParserState {
        return when (line) {
            is SummaryLine -> copy(
                parties = finishBuildingCurrentParty(),
                currentParty = line.toPartyReport(),
                currentPartyVotesBuilder = MpVotesBuilder()
            )
            is MpsVotesLine -> copy(currentPartyVotesBuilder = currentPartyVotesBuilder.with(line.tokens))
            else -> this
        }
    }

    private fun finishBuildingCurrentParty(): List<PartyVoteReport> {
        return parties + currentParty.copy(mpVotes = currentPartyVotesBuilder.build())
    }

    override fun buildReport(): VoteReport {
        return VoteReport(
            id = id,
            timestamp = timestamp,
            summary = summary,
            description = description,
            partyReports = finishBuildingCurrentParty(),
        )
    }
}

fun SummaryLine.toSummary(): VoteSummary {
    return VoteSummary(
        votedCount = votedCount,
        forCount = forCount,
        againstCount = againstCount,
        abstainedCount = abstainedCount,
        absentCount = absentCount,
    )
}

fun SummaryLine.toPartyReport(): PartyVoteReport {
    return PartyVoteReport(
        party = Party(
            name = party!!.name,
            headcount = party.headcount,
        ),
        summary = toSummary(),
        mpVotes = emptyList(),
    )
}

data class MpVotesBuilder(val votes: List<MpVote> = emptyList(), val currentMpName: List<String> = emptyList()) {

    fun with(token: MpsVotesLineToken): MpVotesBuilder {
        return when (token) {
            is MpNameToken -> copy(currentMpName = currentMpName + token.word)
            is VoteToken -> MpVotesBuilder(
                votes = votes + MpVote(
                    name = currentMpName.joinToString(" "),
                    vote = token.vote,
                )
            )
        }
    }

    fun with(tokens: List<MpsVotesLineToken>): MpVotesBuilder {
        return tokens.fold(this, MpVotesBuilder::with)
    }

    fun build(): List<MpVote> {
        if (votes.any { it.name.isBlank() }) throw Exception("Blank MP name found: $votes")
        if (currentMpName.isNotEmpty()) throw Exception("Vote missing: ${currentMpName.joinToString(" ")}")
        return votes
    }
}
