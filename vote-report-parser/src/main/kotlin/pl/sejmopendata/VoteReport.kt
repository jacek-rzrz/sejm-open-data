package pl.sejmopendata

import java.time.LocalDateTime

data class VoteReport(
    val id: VoteId,
    val timestamp: LocalDateTime,
    val description: String,
    val summary: VoteSummary,
    val partyReports: List<PartyVoteReport>
)

data class VoteId(
    val termNumber: Int,
    val sessionNumber: Int,
    val voteNumber: Int,
)

data class VoteSummary(
    val votedCount: Int,
    val forCount: Int,
    val againstCount: Int,
    val abstainedCount: Int,
    val absentCount: Int,
)

data class PartyVoteReport(
    val party: Party,
    val summary: VoteSummary,
    val mpVotes: List<MpVote>,
)

data class Party(
    val name: String,
    val headcount: Int,
)

data class MpVote(
    val name: String,
    val vote: Vote,
)

enum class Vote {
    FOR,
    AGAINST,
    ABSTAIN,
    ABSENT,
}
