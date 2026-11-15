package bendev.addblock.feature.allowlist.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AllowlistRepository {
    val domains: StateFlow<Set<String>>

    fun add(domain: String)

    fun remove(domain: String)
}
