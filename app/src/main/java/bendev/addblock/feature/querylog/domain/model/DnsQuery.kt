package bendev.addblock.feature.querylog.domain.model

data class DnsQuery(
    val domain: String,
    val blocked: Boolean,
    val timestamp: Long,
)

/** Query counts for one clock minute; [minute] is epoch milliseconds / 60 000. */
data class MinuteBucket(
    val minute: Long,
    val allowed: Int = 0,
    val blocked: Int = 0,
) {
    val total get() = allowed + blocked
}

data class DomainCount(val domain: String, val count: Int)

data class QueryStats(
    val total: Int = 0,
    val blocked: Int = 0,
    /** Oldest first, only minutes that had queries. */
    val buckets: List<MinuteBucket> = emptyList(),
    /** Most blocked domains, highest first. */
    val topBlocked: List<DomainCount> = emptyList(),
    /** Most queried allowed domains, highest first. */
    val topAllowed: List<DomainCount> = emptyList(),
) {
    val allowed: Int get() = total - blocked

    val blockedPercent: Int get() = if (total == 0) 0 else blocked * 100 / total

    /** The last [count] minutes up to [nowMillis], oldest first, with empty minutes filled in. */
    fun lastMinutes(nowMillis: Long, count: Int): List<MinuteBucket> {
        val now = nowMillis / 60_000
        val byMinute = buckets.associateBy { it.minute }
        return (now - count + 1..now).map { byMinute[it] ?: MinuteBucket(it) }
    }
}
