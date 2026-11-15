package bendev.addblock.feature.querylog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bendev.addblock.feature.allowlist.domain.usecase.AddToAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.ObserveAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.RemoveFromAllowlistUseCase
import bendev.addblock.feature.querylog.domain.model.DnsQuery
import bendev.addblock.feature.querylog.domain.model.MinuteBucket
import bendev.addblock.feature.querylog.domain.model.QueryStats
import bendev.addblock.feature.querylog.domain.usecase.ClearQueryLogUseCase
import bendev.addblock.feature.querylog.domain.usecase.ObserveQueryLogUseCase
import bendev.addblock.feature.querylog.domain.usecase.ObserveQueryStatsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

enum class QueryFilter(val label: String) {
    ALL("Todas"),
    BLOCKED("Bloqueadas"),
    ALLOWED("Permitidas"),
}

data class QueryLogUiState(
    val stats: QueryStats = QueryStats(),
    val queries: List<DnsQuery> = emptyList(),
    val allowlist: Set<String> = emptySet(),
    val filter: QueryFilter = QueryFilter.ALL,
    val search: String = "",
    val isLogEmpty: Boolean = true,
)

class QueryLogViewModel(
    observeQueryLog: ObserveQueryLogUseCase,
    observeQueryStats: ObserveQueryStatsUseCase,
    observeAllowlist: ObserveAllowlistUseCase,
    private val clearQueryLog: ClearQueryLogUseCase,
    private val addToAllowlist: AddToAllowlistUseCase,
    private val removeFromAllowlist: RemoveFromAllowlistUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(QueryFilter.ALL)
    private val search = MutableStateFlow("")

    val uiState: StateFlow<QueryLogUiState> = combine(
        observeQueryLog(), observeQueryStats(), observeAllowlist(), filter, search,
    ) { queries, stats, allowlist, filter, search ->
        val term = search.trim().lowercase()
        QueryLogUiState(
            stats = stats,
            queries = queries.filter { q ->
                when (filter) {
                    QueryFilter.ALL -> true
                    QueryFilter.BLOCKED -> q.blocked
                    QueryFilter.ALLOWED -> !q.blocked
                } && (term.isEmpty() || term in q.domain)
            },
            allowlist = allowlist,
            filter = filter,
            search = search,
            isLogEmpty = queries.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueryLogUiState())

    /** Per-minute chart data; the clock moves the window forward even when idle. */
    val timeline: StateFlow<List<MinuteBucket>> = combine(
        observeQueryStats(),
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(15_000)
            }
        },
    ) { stats, now -> stats.lastMinutes(now, 30) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: QueryFilter) {
        filter.value = value
    }

    fun setSearch(value: String) {
        search.value = value
    }

    fun clear() = clearQueryLog()

    fun allow(domain: String) {
        addToAllowlist(domain)
    }

    fun disallow(domain: String) = removeFromAllowlist(domain)
}
