package bendev.addblock.feature.protection.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bendev.addblock.feature.allowlist.domain.usecase.ObserveAllowlistUseCase
import bendev.addblock.feature.blocklist.domain.usecase.LoadBlockListUseCase
import bendev.addblock.feature.blocklist.domain.usecase.ObserveBlockListUseCase
import bendev.addblock.feature.protection.domain.model.ProtectionStatus
import bendev.addblock.feature.protection.domain.usecase.CheckPrivateDnsUseCase
import bendev.addblock.feature.protection.domain.usecase.ObserveProtectionUseCase
import bendev.addblock.feature.protection.domain.usecase.StartProtectionUseCase
import bendev.addblock.feature.protection.domain.usecase.StopProtectionUseCase
import bendev.addblock.feature.querylog.domain.model.MinuteBucket
import bendev.addblock.feature.querylog.domain.usecase.ObserveQueryStatsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProtectionUiState(
    val status: ProtectionStatus = ProtectionStatus.STOPPED,
    val error: String? = null,
    val domainCount: Int = 0,
    val allowlistCount: Int = 0,
    val totalQueries: Int = 0,
    val blockedCount: Int = 0,
    val blockedPercent: Int = 0,
    val timeline: List<MinuteBucket> = emptyList(),
    val privateDnsStrict: Boolean = false,
)

class ProtectionViewModel(
    observeProtection: ObserveProtectionUseCase,
    observeBlockList: ObserveBlockListUseCase,
    observeQueryStats: ObserveQueryStatsUseCase,
    observeAllowlist: ObserveAllowlistUseCase,
    loadBlockList: LoadBlockListUseCase,
    private val startProtection: StartProtectionUseCase,
    private val stopProtection: StopProtectionUseCase,
    private val checkPrivateDns: CheckPrivateDnsUseCase,
) : ViewModel() {

    private val privateDnsStrict = MutableStateFlow(false)

    /** Moves the chart window forward even when no queries arrive. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000)
        }
    }

    private val timeline = combine(observeQueryStats(), clock) { stats, now ->
        stats.lastMinutes(now, TIMELINE_MINUTES)
    }

    val uiState: StateFlow<ProtectionUiState> = combine(
        observeProtection(),
        observeBlockList(),
        observeQueryStats(),
        observeAllowlist(),
        combine(timeline, privateDnsStrict, ::Pair),
    ) { protection, blockList, stats, allowlist, (timeline, privateDns) ->
        ProtectionUiState(
            status = protection.status,
            error = protection.error,
            domainCount = blockList.domainCount,
            allowlistCount = allowlist.size,
            totalQueries = stats.total,
            blockedCount = stats.blocked,
            blockedPercent = stats.blockedPercent,
            timeline = timeline,
            privateDnsStrict = privateDns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProtectionUiState())

    init {
        viewModelScope.launch { if (observeBlockList().value.domainCount == 0) loadBlockList() }
    }

    fun start() = startProtection()

    fun stop() = stopProtection()

    fun refreshPrivateDns() {
        privateDnsStrict.value = checkPrivateDns()
    }

    private companion object {
        const val TIMELINE_MINUTES = 30
    }
}
