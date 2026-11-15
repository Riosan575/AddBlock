package bendev.addblock.feature.blocklist.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import bendev.addblock.feature.blocklist.data.local.HostsLocalDataSource
import bendev.addblock.feature.blocklist.data.parser.HostsParser
import bendev.addblock.feature.blocklist.data.remote.HostsRemoteDataSource
import bendev.addblock.feature.blocklist.domain.model.BlockListInfo
import bendev.addblock.feature.blocklist.domain.model.HostSource
import bendev.addblock.feature.blocklist.domain.repository.BlockListRepository

class BlockListRepositoryImpl(
    private val local: HostsLocalDataSource,
    private val remote: HostsRemoteDataSource,
) : BlockListRepository {

    private val mutex = Mutex()

    @Volatile
    private var blocked: Set<String> = emptySet()

    private val _info = MutableStateFlow(
        BlockListInfo(sources = sources(), lastUpdated = local.lastUpdated),
    )
    override val info: StateFlow<BlockListInfo> = _info.asStateFlow()

    override fun contains(domain: String) = domain in blocked

    override fun hasDownloadedLists() = local.enabledIds().any(local::exists)

    override suspend fun load() = locked { reload() }

    override suspend fun update(): List<String> = locked {
        _info.update { it.copy(isUpdating = true) }
        try {
            val enabled = HostSourceCatalog.entries.filter { it.id in local.enabledIds() }
            val failed = enabled.filterNot { download(it) }.map { it.name }
            if (failed.size < enabled.size) local.lastUpdated = System.currentTimeMillis()
            reload()
            failed
        } finally {
            _info.update { it.copy(isUpdating = false, lastUpdated = local.lastUpdated) }
        }
    }

    override suspend fun setSourceEnabled(id: String, enabled: Boolean) = locked {
        local.setEnabled(id, enabled)
        if (enabled && !local.exists(id)) {
            HostSourceCatalog.entries.find { it.id == id }?.let(::download)
        }
        reload()
    }

    private suspend fun <T> locked(block: () -> T): T =
        mutex.withLock { withContext(Dispatchers.IO) { block() } }

    private fun reload() {
        val set = HashSet<String>(200_000)
        for (id in local.enabledIds()) local.forEachLine(id) { HostsParser.parseLine(it, set) }
        blocked = set
        _info.update { it.copy(sources = sources(), domainCount = set.size) }
    }

    private fun download(entry: HostSourceCatalog.Entry): Boolean = runCatching {
        remote.download(entry.url, local.tempFile(entry.id))
        local.commit(entry.id)
    }.isSuccess

    private fun sources(): List<HostSource> {
        val enabled = local.enabledIds()
        return HostSourceCatalog.entries.map { HostSource(it.id, it.name, it.url, it.id in enabled) }
    }
}
