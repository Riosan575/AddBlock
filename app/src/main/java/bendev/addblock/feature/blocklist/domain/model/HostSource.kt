package bendev.addblock.feature.blocklist.domain.model

data class HostSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean,
)

data class BlockListInfo(
    val sources: List<HostSource> = emptyList(),
    val domainCount: Int = 0,
    val lastUpdated: Long = 0L,
    val isUpdating: Boolean = false,
)
