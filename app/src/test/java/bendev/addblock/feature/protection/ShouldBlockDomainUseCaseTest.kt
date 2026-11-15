package bendev.addblock.feature.protection

import bendev.addblock.feature.allowlist.domain.repository.AllowlistRepository
import bendev.addblock.feature.blocklist.domain.model.BlockListInfo
import bendev.addblock.feature.blocklist.domain.repository.BlockListRepository
import bendev.addblock.feature.protection.domain.usecase.ShouldBlockDomainUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldBlockDomainUseCaseTest {

    private class FakeBlockList(private val domains: Set<String>) : BlockListRepository {
        override val info = MutableStateFlow(BlockListInfo())
        override fun contains(domain: String) = domain in domains
        override fun hasDownloadedLists() = true
        override suspend fun load() = Unit
        override suspend fun update() = emptyList<String>()
        override suspend fun setSourceEnabled(id: String, enabled: Boolean) = Unit
    }

    private class FakeAllowlist(initial: Set<String>) : AllowlistRepository {
        override val domains = MutableStateFlow(initial)
        override fun add(domain: String) { domains.value += domain }
        override fun remove(domain: String) { domains.value -= domain }
    }

    private val shouldBlock = ShouldBlockDomainUseCase(
        FakeBlockList(setOf("ads.example.com", "cdn.allowed.org")),
        FakeAllowlist(setOf("allowed.org")),
    )

    @Test
    fun blocksListedDomains() = assertTrue(shouldBlock("ads.example.com"))

    @Test
    fun listMatchIsExact() = assertFalse(shouldBlock("example.com"))

    @Test
    fun allowlistCoversSubdomains() = assertFalse(shouldBlock("cdn.allowed.org"))
}
