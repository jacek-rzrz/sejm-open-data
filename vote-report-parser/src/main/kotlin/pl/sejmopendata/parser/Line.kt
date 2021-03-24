package pl.sejmopendata.parser

import mu.KLogging
import pl.sejmopendata.Vote
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class Line {
    companion object : KLogging() {
        fun parse(text: String): Line {
            return parseTrimmedLine(text.trim()).also { line ->
                logger.info { line.javaClass.simpleName.padEnd(20) + text }
            }
        }

        private fun parseTrimmedLine(text: String): Line {
            return sequenceOf(
                TermLine,
                VoteIdLine,
                SummaryLine,
                MpsVotesLine,
            )
                .mapNotNull { lineType -> lineType.tryParse(text) }
                .firstOrNull()
                ?: FreeTextLine(text)
        }
    }
}

interface LineCompanion {
    fun tryParse(text: String): Line?
}

data class FreeTextLine(val text: String) : Line()

data class TermLine(val term: Int) : Line() {

    companion object : LineCompanion {
        override fun tryParse(text: String): TermLine? {
            val match = "Sejm RP (\\d+) kadencji".toRegex()
                .matchEntire(text) ?: return null
            return TermLine(match.groups[1]!!.value.toInt())
        }
    }
}

data class VoteIdLine(
    val sessionNumber: Int,
    val voteNumber: Int,
    val timestamp: LocalDateTime,
) : Line() {
    companion object : LineCompanion {

        private val timestampFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

        override fun tryParse(text: String): VoteIdLine? {
            val match = "POSIEDZENIE\\s+(\\d+).*głosowanie\\s+nr\\s+(\\d+)\\s+\\((.*)\\)".toRegex()
                .matchEntire(text) ?: return null
            return VoteIdLine(
                sessionNumber = match.groups[1]!!.value.toInt(),
                voteNumber = match.groups[2]!!.value.toInt(),
                timestamp = LocalDateTime.parse(match.groups[3]!!.value, timestampFormat)
            )
        }
    }
}

data class SummaryLine(
    val party: Party?,
    val votedCount: Int,
    val forCount: Int,
    val againstCount: Int,
    val abstainedCount: Int,
    val absentCount: Int,
) : Line() {
    companion object : LineCompanion {
        override fun tryParse(text: String): SummaryLine? {
            val match = "(.*)?\\s*GŁOSOWAŁO - (\\d+) ZA - (\\d+) PRZECIW - (\\d+) WSTRZYM.* - (\\d+) NIE GŁOS.*- (\\d+)".toRegex()
                .matchEntire(text) ?: return null
            return SummaryLine(
                party = partyOrNull(match.groupValues[1]),
                votedCount = match.groupValues[2].toInt(),
                forCount = match.groupValues[3].toInt(),
                againstCount = match.groupValues[4].toInt(),
                abstainedCount = match.groupValues[5].toInt(),
                absentCount = match.groupValues[6].toInt(),
            )
        }

        private fun partyOrNull(text: String): Party? {
            val match = "(.*)\\s\\((\\d+)\\)\\s*".toRegex()
                .matchEntire(text) ?: return null
            val name = match.groupValues[1]
            val headcount = match.groupValues[2].toIntOrNull() ?: return null
            return Party(name = name, headcount = headcount)
        }
    }

    data class Party(
        val name: String,
        val headcount: Int,
    )
}

data class MpsVotesLine(val tokens: List<MpsVotesLineToken>) : Line() {

    companion object : LineCompanion {

        override fun tryParse(text: String): Line? {
            if (!".*[A-Z]+.*\\s(za|pr.|ng.|ws.)\\s.*".toRegex().matches(text)) return null

            val tokens = text.split(" ")
                .filter { it.isNotBlank() }
                .map { token ->
                    when (token) {
                        "za" -> VoteToken(Vote.FOR)
                        "pr." -> VoteToken(Vote.AGAINST)
                        "ng." -> VoteToken(Vote.ABSENT)
                        "ws." -> VoteToken(Vote.ABSTAIN)
                        else -> MpNameToken(token)
                    }
                }

            return MpsVotesLine(tokens)
        }
    }
}

sealed class MpsVotesLineToken
data class MpNameToken(val word: String) : MpsVotesLineToken()
data class VoteToken(val vote: Vote) : MpsVotesLineToken()
