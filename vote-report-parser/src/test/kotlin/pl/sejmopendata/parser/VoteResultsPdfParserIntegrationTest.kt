package pl.sejmopendata.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.sejmopendata.MpVote
import pl.sejmopendata.Vote
import pl.sejmopendata.VoteReport
import pl.sejmopendata.testutils.loadBytes

class VoteResultsPdfParserIntegrationTest {

    lateinit var report: VoteReport

    @BeforeEach
    fun setUp() {
        val parser = TextBasedVoteResultsPdfParser()
        val pdfFile = "kadencja9_posiedzenie27_glosowanie15.pdf".loadBytes()
        report = parser.parse(pdfFile)
    }

    @Test
    fun `extracts vote id`() {
        assertThat(report.id.termNumber).isEqualTo(9)
        assertThat(report.id.sessionNumber).isEqualTo(27)
        assertThat(report.id.voteNumber).isEqualTo(15)
    }

    @Test
    fun `extracts time`() {
        assertThat(report.timestamp).isEqualTo("2021-03-17T16:27:21")
    }

    @Test
    fun `extracts overall vote summary`() {
        assertThat(report.summary.votedCount).isEqualTo(446)
        assertThat(report.summary.forCount).isEqualTo(213)
        assertThat(report.summary.againstCount).isEqualTo(231)
        assertThat(report.summary.abstainedCount).isEqualTo(2)
        assertThat(report.summary.absentCount).isEqualTo(14)
    }

    @Test
    fun `extracts description`() {
        assertThat(report.description).startsWith("Pkt 6. porz. dzien. Sprawozdanie Komisji")
        assertThat(report.description).endsWith("Głosowanie nad przyjęciem 1. poprawki")
    }

    @Test
    fun `extracts party vote summary`() {
        assertThat(report.partyReports).hasSize(8)
        assertThat(report.partyReports[1].party.name).isEqualTo("KO")
        assertThat(report.partyReports[1].party.headcount).isEqualTo(131)
        assertThat(report.partyReports[1].summary.votedCount).isEqualTo(128)
        assertThat(report.partyReports[1].summary.forCount).isEqualTo(127)
        assertThat(report.partyReports[1].summary.againstCount).isEqualTo(1)
        assertThat(report.partyReports[1].summary.abstainedCount).isEqualTo(0)
        assertThat(report.partyReports[1].summary.absentCount).isEqualTo(3)
    }

    @Test
    fun `extracts individual votes`() {
        val voteAgainst = report.partyReports[0].mpVotes.find { it.name == "KACZYŃSKI JAROSŁAW" }
        val voteFor = report.partyReports[1].mpVotes.find { it.name == "CHMIEL MAŁGORZATA" }
        val abstain = report.partyReports[7].mpVotes.find { it.name == "KOŁAKOWSKI LECH" }
        val absent = report.partyReports[2].mpVotes.find { it.name == "PROKOP-PACZKOWSKA MAŁGORZATA" }

        assertThat(report.partyReports[1].mpVotes).hasSize(131)
        assertThat(voteAgainst).isEqualTo(MpVote(name = "KACZYŃSKI JAROSŁAW", vote = Vote.AGAINST))
        assertThat(voteFor).isEqualTo(MpVote(name = "CHMIEL MAŁGORZATA", vote = Vote.FOR))
        assertThat(abstain).isEqualTo(MpVote(name = "KOŁAKOWSKI LECH", vote = Vote.ABSTAIN))
        assertThat(absent).isEqualTo(MpVote(name = "PROKOP-PACZKOWSKA MAŁGORZATA", vote = Vote.ABSENT))
    }
}
