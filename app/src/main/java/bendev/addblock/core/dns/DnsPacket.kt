package bendev.addblock.core.dns

/**
 * Minimal IPv4/UDP/DNS parsing and building, just enough to answer DNS
 * queries that arrive through the TUN interface.
 */
object DnsPacket {

    class UdpPacket(
        val srcAddr: ByteArray,
        val dstAddr: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val payload: ByteArray,
    )

    fun parseIpv4Udp(packet: ByteArray, length: Int = packet.size): UdpPacket? {
        if (length < 28) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return null
        val ihl = (packet[0].toInt() and 0xF) * 4
        if (packet[9].toInt() != 17) return null // not UDP
        if (u16(packet, 6) and 0x3FFF != 0) return null // fragmented
        val totalLen = minOf(u16(packet, 2), length)
        if (totalLen < ihl + 8) return null

        val udpLen = u16(packet, ihl + 4)
        val payloadStart = ihl + 8
        val payloadEnd = minOf(ihl + udpLen, totalLen)
        if (payloadEnd <= payloadStart) return null

        return UdpPacket(
            srcAddr = packet.copyOfRange(12, 16),
            dstAddr = packet.copyOfRange(16, 20),
            srcPort = u16(packet, ihl),
            dstPort = u16(packet, ihl + 2),
            payload = packet.copyOfRange(payloadStart, payloadEnd),
        )
    }

    /** Lowercase name of the first question, or null if it can't be read. */
    fun queryName(dns: ByteArray): String? = readQuestion(dns)?.first

    /** NXDOMAIN answer for [query], keeping only its header and question. */
    fun blockedResponse(query: ByteArray): ByteArray? {
        val questionEnd = readQuestion(query)?.second ?: return null
        val resp = query.copyOf(questionEnd)
        // QR=1, keep opcode and RD, clear AA/TC
        resp[2] = ((query[2].toInt() and 0x79) or 0x80).toByte()
        // RA=1, RCODE=3 (NXDOMAIN)
        resp[3] = 0x83.toByte()
        put16(resp, 4, 1)
        put16(resp, 6, 0)
        put16(resp, 8, 0)
        put16(resp, 10, 0)
        return resp
    }

    /** Builds the reply packet for [request], swapping addresses and ports. */
    fun reply(request: UdpPacket, payload: ByteArray): ByteArray =
        buildIpv4Udp(request.dstAddr, request.srcAddr, request.dstPort, request.srcPort, payload)

    fun buildIpv4Udp(
        srcAddr: ByteArray,
        dstAddr: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val p = ByteArray(totalLen)
        p[0] = 0x45
        put16(p, 2, totalLen)
        p[6] = 0x40 // don't fragment
        p[8] = 64 // TTL
        p[9] = 17 // UDP
        System.arraycopy(srcAddr, 0, p, 12, 4)
        System.arraycopy(dstAddr, 0, p, 16, 4)
        put16(p, 10, ipChecksum(p, 20))

        put16(p, 20, srcPort)
        put16(p, 22, dstPort)
        put16(p, 24, 8 + payload.size)
        // UDP checksum 0 = not computed, valid for IPv4
        System.arraycopy(payload, 0, p, 28, payload.size)
        return p
    }

    /** (name, offset just past the question) for the first question. */
    private fun readQuestion(dns: ByteArray): Pair<String, Int>? {
        if (dns.size < 12 || u16(dns, 4) < 1) return null
        val name = StringBuilder()
        var pos = 12
        while (true) {
            if (pos >= dns.size) return null
            val len = dns[pos].toInt() and 0xFF
            pos++
            if (len == 0) break
            if (len and 0xC0 != 0) return null // compression is not used in questions
            if (pos + len > dns.size) return null
            if (name.isNotEmpty()) name.append('.')
            for (i in pos until pos + len) name.append((dns[i].toInt() and 0xFF).toChar())
            pos += len
        }
        val end = pos + 4 // QTYPE + QCLASS
        if (end > dns.size) return null
        return name.toString().lowercase() to end
    }

    internal fun ipChecksum(p: ByteArray, len: Int): Int {
        var sum = 0
        for (i in 0 until len step 2) sum += u16(p, i)
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    private fun u16(b: ByteArray, off: Int) =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun put16(b: ByteArray, off: Int, v: Int) {
        b[off] = (v shr 8).toByte()
        b[off + 1] = v.toByte()
    }
}
