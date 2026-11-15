package bendev.addblock.feature.protection.data.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import bendev.addblock.AddBlockApp
import bendev.addblock.MainActivity
import bendev.addblock.R
import bendev.addblock.feature.protection.domain.model.ProtectionStatus
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * Local VPN that only routes a fake DNS server address through the TUN
 * interface, so the rest of the traffic never touches this app.
 */
class AdBlockVpnService : VpnService() {

    private val container get() = (application as AddBlockApp).container

    private var worker: Thread? = null

    /** Closing this end of the pipe wakes up the poll loop so it can exit. */
    @Volatile
    private var wakeWrite: FileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        // A null intent means a restart by the system or always-on VPN
        startVpn()
        return START_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (worker?.isAlive == true) return
        val (wakeRead, wakeW) = Os.pipe().let { it[0] to it[1] }
        wakeWrite = wakeW
        container.protectionStateHolder.set(ProtectionStatus.STARTING)
        worker = thread(name = "addblock-vpn") { run(wakeRead) }
    }

    private fun stopVpn() {
        wakeWrite?.let { runCatching { Os.close(it) } }
        wakeWrite = null
        worker?.join(2_000)
        worker = null
    }

    private fun run(wakeRead: FileDescriptor) {
        val holder = container.protectionStateHolder
        var tun: ParcelFileDescriptor? = null
        try {
            runBlocking { container.ensureBlockListReady() }
            tun = buildTun() ?: error("Permiso de VPN no concedido")
            holder.set(ProtectionStatus.RUNNING)
            loop(tun, wakeRead)
            holder.set(ProtectionStatus.STOPPED)
        } catch (e: Exception) {
            holder.fail(e.message)
        } finally {
            runCatching { tun?.close() }
            runCatching { Os.close(wakeRead) }
        }
    }

    private fun buildTun(): ParcelFileDescriptor? {
        val configure = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(VPN_ADDRESS, 24)
            .addDnsServer(VPN_DNS)
            .addRoute(VPN_DNS, 32)
            // Without this Android blackholes all IPv6 traffic, since the VPN has no IPv6 route
            .allowFamily(OsConstants.AF_INET6)
            .setConfigureIntent(configure)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setMetered(false) }
            .establish()
    }

    private fun loop(tun: ParcelFileDescriptor, wakeRead: FileDescriptor) {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val handler = DnsPacketHandler(
            shouldBlock = container.shouldBlockDomain,
            recordQuery = container.recordQuery,
            protect = { protect(it) },
            reply = { packet -> synchronized(output) { runCatching { output.write(packet) } } },
        )
        val tunPoll = StructPollfd().apply {
            fd = tun.fileDescriptor
            events = OsConstants.POLLIN.toShort()
        }
        val wakePoll = StructPollfd().apply { fd = wakeRead } // POLLHUP is always reported
        val buf = ByteArray(Short.MAX_VALUE.toInt())

        try {
            while (true) {
                try {
                    Os.poll(arrayOf(tunPoll, wakePoll), -1)
                } catch (e: ErrnoException) {
                    if (e.errno == OsConstants.EINTR) continue
                    throw e
                }
                if (wakePoll.revents.toInt() != 0) break
                if (tunPoll.revents.toInt() and OsConstants.POLLIN != 0) {
                    val n = input.read(buf)
                    if (n > 0) handler.handle(buf.copyOf(n))
                }
            }
        } finally {
            handler.shutdown()
        }
    }

    companion object {
        const val ACTION_START = "bendev.addblock.action.START"
        const val ACTION_STOP = "bendev.addblock.action.STOP"

        private const val VPN_ADDRESS = "10.111.222.1"
        private const val VPN_DNS = "10.111.222.2"
    }
}
