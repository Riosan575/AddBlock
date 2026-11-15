package bendev.addblock.feature.protection.domain.usecase

import bendev.addblock.feature.allowlist.domain.repository.AllowlistRepository
import bendev.addblock.feature.blocklist.domain.repository.BlockListRepository
import bendev.addblock.feature.protection.domain.repository.ProtectionRepository

class ObserveProtectionUseCase(private val repository: ProtectionRepository) {
    operator fun invoke() = repository.state
}

class StartProtectionUseCase(private val repository: ProtectionRepository) {
    operator fun invoke() = repository.start()
}

class StopProtectionUseCase(private val repository: ProtectionRepository) {
    operator fun invoke() = repository.stop()
}

class CheckPrivateDnsUseCase(private val repository: ProtectionRepository) {
    operator fun invoke() = repository.isPrivateDnsStrict()
}

/** Blocked when it's in the lists and neither it nor a parent domain is allowlisted. */
class ShouldBlockDomainUseCase(
    private val blockList: BlockListRepository,
    private val allowlist: AllowlistRepository,
) {
    operator fun invoke(domain: String): Boolean {
        val allowed = allowlist.domains.value
        var d = domain
        while (true) {
            if (d in allowed) return false
            val dot = d.indexOf('.')
            if (dot < 0) break
            d = d.substring(dot + 1)
        }
        return blockList.contains(domain)
    }
}
