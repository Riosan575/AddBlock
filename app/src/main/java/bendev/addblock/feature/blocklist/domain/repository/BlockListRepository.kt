package bendev.addblock.feature.blocklist.domain.repository

import kotlinx.coroutines.flow.StateFlow
import bendev.addblock.feature.blocklist.domain.model.BlockListInfo

interface BlockListRepository {
    val info: StateFlow<BlockListInfo>

    /** Exact match against the loaded lists. Must be fast: called for every DNS query. */
    fun contains(domain: String): Boolean

    fun hasDownloadedLists(): Boolean

    /** Rebuilds the in-memory set from the lists already on disk. */
    suspend fun load()

    /** Downloads every enabled source and reloads. Returns the names of the sources that failed. */
    suspend fun update(): List<String>

    suspend fun setSourceEnabled(id: String, enabled: Boolean)
}
