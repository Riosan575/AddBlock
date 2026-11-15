package bendev.addblock.feature.querylog

import bendev.addblock.feature.querylog.data.InMemoryQueryLogRepository
import bendev.addblock.feature.querylog.domain.model.DnsQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryStatsTest {

    private val minute = 60_000L
    private val t0 = 1_000 * minute

    @Test
    fun bucketsQueriesPerMinuteAndFillsGaps() {
        val repo = InMemoryQueryLogRepository()
        repo.record(DnsQuery("a.com", blocked = true, timestamp = t0))
        repo.record(DnsQuery("b.com", blocked = false, timestamp = t0 + 10_000))
        repo.record(DnsQuery("c.com", blocked = true, timestamp = t0 + 2 * minute))

        val stats = repo.stats.value
        assertEquals(3, stats.total)
        assertEquals(2, stats.blocked)
        assertEquals(66, stats.blockedPercent)

        val timeline = stats.lastMinutes(nowMillis = t0 + 2 * minute, count = 4)
        assertEquals(listOf(0, 2, 0, 1), timeline.map { it.total })
        assertEquals(listOf(0, 1, 0, 1), timeline.map { it.blocked })
    }

    @Test
    fun clearingTheLogKeepsTotals() {
        val repo = InMemoryQueryLogRepository()
        repo.record(DnsQuery("a.com", blocked = true, timestamp = t0))
        repo.clear()
        assertEquals(0, repo.queries.value.size)
        assertEquals(1, repo.stats.value.blocked)
    }

    @Test
    fun keepsTopDomainsSorted() {
        val repo = InMemoryQueryLogRepository(topSize = 2)
        listOf("a.com", "b.com", "b.com", "c.com", "c.com", "c.com").forEach {
            repo.record(DnsQuery(it, blocked = true, timestamp = t0))
        }
        repo.record(DnsQuery("ok.com", blocked = false, timestamp = t0))

        val stats = repo.stats.value
        assertEquals(listOf("c.com" to 3, "b.com" to 2), stats.topBlocked.map { it.domain to it.count })
        assertEquals(listOf("ok.com" to 1), stats.topAllowed.map { it.domain to it.count })
    }
}
