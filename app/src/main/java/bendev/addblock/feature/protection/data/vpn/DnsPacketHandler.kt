package bendev.addblock.feature.protection.data.vpn

import bendev.addblock.core.dns.DnsPacket
import bendev.addblock.feature.protection.domain.usecase.ShouldBlockDomainUseCase
import bendev.addblock.feature.querylog.domain.usecase.RecordQueryUseCase
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Answers blocked DNS queries locally with NXDOMAIN and forwards the rest
 * to an upstream resolver over a socket that bypasses the VPN.
 */
class DnsPacketHandler(
    private val shouldBlock: ShouldBlockDomainUseCase,
    private val recordQuery: RecordQueryUseCase,
    private val protect: (DatagramSocket) -> Boolean,
    private val reply: (ByteArray) -> Unit,
) {
    private val executor: ExecutorService = Executors.newFixedThreadPool(8)

    fun handle(packet: ByteArray) {
        val request = DnsPacket.parseIpv4Udp(packet) ?: return
        if (request.dstPort != 53) return

        val name = DnsPacket.queryName(request.payload)
        if (name != null && shouldBlock(name)) {
            DnsPacket.blockedResponse(request.payload)?.let { reply(DnsPacket.reply(request, it)) }
            recordQuery(name, blocked = true)
            return
        }
        // Reverse lookups (1.2.3.4.in-addr.arpa) are resolved but mean nothing to the user
        if (name != null && !name.endsWith(".arpa")) recordQuery(name, blocked = false)

        executor.execute {
            forward(request.payload)?.let { reply(DnsPacket.reply(request, it)) }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun forward(query: ByteArray): ByteArray? {
        for (server in UPSTREAM_DNS) {
            try {
                DatagramSocket().use { socket ->
                    protect(socket)
                    socket.soTimeout = 4_000
                    socket.send(DatagramPacket(query, query.size, server, 53))
                    val buf = ByteArray(4096)
                    val response = DatagramPacket(buf, buf.size)
                    socket.receive(response)
                    return buf.copyOf(response.length)
                }
            } catch (_: SocketTimeoutException) {
                // try the next server
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    private companion object {
        val UPSTREAM_DNS: List<InetAddress> =
            listOf("1.1.1.1", "8.8.8.8").map { InetAddress.getByName(it) }
    }
}
