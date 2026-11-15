package bendev.addblock.feature.blocklist.data

object HostSourceCatalog {

    data class Entry(val id: String, val name: String, val url: String, val defaultEnabled: Boolean)

    val entries = listOf(
        Entry("adaway", "AdAway", "https://adaway.org/hosts.txt", true),
        Entry(
            "stevenblack", "StevenBlack (unificada)",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", true,
        ),
        Entry(
            "pgl", "Peter Lowe",
            "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            false,
        ),
    )

    val defaultIds = entries.filter { it.defaultEnabled }.map { it.id }.toSet()
}
