package bendev.addblock.feature.querylog.domain.repository

import bendev.addblock.feature.querylog.domain.model.DnsQuery
import bendev.addblock.feature.querylog.domain.model.QueryStats
import kotlinx.coroutines.flow.StateFlow

interface QueryLogRepository {
    /** Most recent first. */
    val queries: StateFlow<List<DnsQuery>>

    /** Totals since the app process started, plus a per-minute timeline. */
    val stats: StateFlow<QueryStats>

    fun record(query: DnsQuery)

    fun clear()
}
