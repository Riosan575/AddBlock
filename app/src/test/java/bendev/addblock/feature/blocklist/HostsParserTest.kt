package bendev.addblock.feature.blocklist

import bendev.addblock.feature.blocklist.data.parser.HostsParser
import org.junit.Assert.assertEquals
import org.junit.Test

class HostsParserTest {

    private fun parse(vararg lines: String) =
        HashSet<String>().also { set -> lines.forEach { HostsParser.parseLine(it, set) } }

    @Test
    fun parsesHostsAndPlainFormats() {
        val result = parse(
            "# comment",
            "",
            "127.0.0.1 localhost",
            "0.0.0.0 0.0.0.0",
            "0.0.0.0\tAds.Example.com  # trailing",
            "127.0.0.1 a.com b.com",
            "tracker.net.",
        )
        assertEquals(setOf("ads.example.com", "a.com", "b.com", "tracker.net"), result)
    }
}
