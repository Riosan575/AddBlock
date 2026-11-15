package bendev.addblock.core.dns

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsPacketTest {

    /** Query for "ads.example.com", type A, with RD set. */
    private val query = byteArrayOf(
        0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        3, 'a'.code.toByte(), 'd'.code.toByte(), 's'.code.toByte(),
        7, *"example".toByteArray(),
        3, *"com".toByteArray(),
        0, 0x00, 0x01, 0x00, 0x01,
    )

    private val client = byteArrayOf(10, 111, 222.toByte(), 1)
    private val dns = byteArrayOf(10, 111, 222.toByte(), 2)

    @Test
    fun readsQueryName() {
        assertEquals("ads.example.com", DnsPacket.queryName(query))
    }

    @Test
    fun blockedResponseIsNxdomainWithSameIdAndQuestion() {
        val resp = DnsPacket.blockedResponse(query)!!
        assertEquals(query.size, resp.size)
        assertEquals(0x12, resp[0].toInt())
        assertEquals(0x34, resp[1].toInt())
        assertEquals(0x81, resp[2].toInt() and 0xFF) // QR + RD
        assertEquals(0x83, resp[3].toInt() and 0xFF) // RA + NXDOMAIN
        assertArrayEquals(query.copyOfRange(12, query.size), resp.copyOfRange(12, resp.size))
    }

    @Test
    fun ipv4UdpRoundTrip() {
        val packet = DnsPacket.buildIpv4Udp(client, dns, 40000, 53, query)
        val parsed = DnsPacket.parseIpv4Udp(packet)
        assertNotNull(parsed)
        parsed!!
        assertArrayEquals(client, parsed.srcAddr)
        assertArrayEquals(dns, parsed.dstAddr)
        assertEquals(40000, parsed.srcPort)
        assertEquals(53, parsed.dstPort)
        assertArrayEquals(query, parsed.payload)
        // A header with a valid checksum sums to zero
        assertEquals(0, DnsPacket.ipChecksum(packet, 20))
    }

    @Test
    fun replySwapsAddressesAndPorts() {
        val request = DnsPacket.parseIpv4Udp(DnsPacket.buildIpv4Udp(client, dns, 40000, 53, query))!!
        val reply = DnsPacket.parseIpv4Udp(DnsPacket.reply(request, query))!!
        assertArrayEquals(dns, reply.srcAddr)
        assertArrayEquals(client, reply.dstAddr)
        assertEquals(53, reply.srcPort)
        assertEquals(40000, reply.dstPort)
    }

    @Test
    fun ignoresTruncatedPackets() {
        assertNull(DnsPacket.parseIpv4Udp(ByteArray(10)))
        assertNull(DnsPacket.queryName(query.copyOf(20)))
    }
}
