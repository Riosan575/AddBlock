package bendev.addblock.feature.querylog.domain.usecase

import bendev.addblock.feature.querylog.domain.model.DnsQuery
import bendev.addblock.feature.querylog.domain.repository.QueryLogRepository

class ObserveQueryLogUseCase(private val repository: QueryLogRepository) {
    operator fun invoke() = repository.queries
}

class ObserveQueryStatsUseCase(private val repository: QueryLogRepository) {
    operator fun invoke() = repository.stats
}

class RecordQueryUseCase(private val repository: QueryLogRepository) {
    operator fun invoke(domain: String, blocked: Boolean) =
        repository.record(DnsQuery(domain, blocked, System.currentTimeMillis()))
}

class ClearQueryLogUseCase(private val repository: QueryLogRepository) {
    operator fun invoke() = repository.clear()
}
