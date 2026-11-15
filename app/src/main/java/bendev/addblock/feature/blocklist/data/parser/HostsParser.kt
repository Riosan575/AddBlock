package bendev.addblock.feature.blocklist.data.parser

/** Reads hosts-file lines ("0.0.0.0 ads.example.com") and plain domain lists. */
object HostsParser {

    private val IGNORED = setOf(
        "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "0.0.0.0",
    )

    fun parseLine(line: String, into: MutableSet<String>) {
        val content = line.substringBefore('#').trim()
        if (content.isEmpty()) return
        val tokens = content.split(' ', '\t').filter { it.isNotEmpty() }
        val domains = if (tokens.size == 1) tokens else tokens.drop(1)
        for (raw in domains) {
            val domain = raw.lowercase().trimEnd('.')
            if ('.' in domain && domain !in IGNORED) into += domain
        }
    }
}
