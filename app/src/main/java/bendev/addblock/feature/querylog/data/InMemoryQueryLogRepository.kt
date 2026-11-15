package bendev.addblock.feature.querylog.data

import bendev.addblock.feature.querylog.domain.model.DnsQuery
import bendev.addblock.feature.querylog.domain.model.DomainCount
import bendev.addblock.feature.querylog.domain.model.MinuteBucket
import bendev.addblock.feature.querylog.domain.model.QueryStats
import bendev.addblock.feature.querylog.domain.repository.QueryLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryQueryLogRepository(
    private val maxEntries: Int = 300,
    private val keepMinutes: Int = 60,
    private val topSize: Int = 5,
) : QueryLogRepository {

    private val _queries = MutableStateFlow<List<DnsQuery>>(emptyList())
    override val queries: StateFlow<List<DnsQuery>> = _queries.asStateFlow()

    private val _stats = MutableStateFlow(QueryStats())
    override val stats: StateFlow<QueryStats> = _stats.asStateFlow()

    private val blockedCounts = HashMap<String, Int>()
    private val allowedCounts = HashMap<String, Int>()

    @Synchronized
    override fun record(query: DnsQuery) {
        _queries.update { (listOf(query) + it).take(maxEntries) }

        val counts = if (query.blocked) blockedCounts else allowedCounts
        val count = (counts[query.domain] ?: 0) + 1
        counts[query.domain] = count

        _stats.update { stats ->
            val withQuery = stats.plus(query)
            if (query.blocked) {
                withQuery.copy(topBlocked = withQuery.topBlocked.bump(query.domain, count))
            } else {
                withQuery.copy(topAllowed = withQuery.topAllowed.bump(query.domain, count))
            }
        }
    }

    override fun clear() {
        _queries.value = emptyList()
    }

    /** Counts only grow, so the top list can be maintained without rescanning every domain. */
    private fun List<DomainCount>.bump(domain: String, count: Int): List<DomainCount> {
        val inTop = any { it.domain == domain }
        if (!inTop && size >= topSize && count <= last().count) return this
        return (filterNot { it.domain == domain } + DomainCount(domain, count))
            .sortedByDescending { it.count }
            .take(topSize)
    }

    private fun QueryStats.plus(query: DnsQuery): QueryStats {
        val minute = query.timestamp / 60_000
        val last = buckets.lastOrNull()
        val current = if (last?.minute == minute) last else MinuteBucket(minute)
        val updated = if (query.blocked) {
            current.copy(blocked = current.blocked + 1)
        } else {
            current.copy(allowed = current.allowed + 1)
        }
        val previous = if (last?.minute == minute) buckets.dropLast(1) else buckets
        return copy(
            total = total + 1,
            blocked = blocked + if (query.blocked) 1 else 0,
            buckets = (previous + updated).filter { it.minute > minute - keepMinutes },
        )
    }
}
