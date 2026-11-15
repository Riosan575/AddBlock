package bendev.addblock.feature.protection.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import bendev.addblock.feature.protection.data.vpn.AdBlockVpnService
import bendev.addblock.feature.protection.domain.model.ProtectionState
import bendev.addblock.feature.protection.domain.repository.ProtectionRepository
import kotlinx.coroutines.flow.StateFlow

class ProtectionRepositoryImpl(
    private val context: Context,
    private val holder: ProtectionStateHolder,
) : ProtectionRepository {

    override val state: StateFlow<ProtectionState> = holder.state

    override fun start() {
        context.startService(serviceIntent(AdBlockVpnService.ACTION_START))
    }

    override fun stop() {
        context.startService(serviceIntent(AdBlockVpnService.ACTION_STOP))
    }

    @Suppress("DEPRECATION") // allNetworks: we need the underlying network, not the VPN
    override fun isPrivateDnsStrict(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { network ->
            val caps = cm.getNetworkCapabilities(network)
            val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            !isVpn && cm.getLinkProperties(network)?.privateDnsServerName != null
        }
    }

    private fun serviceIntent(action: String) =
        Intent(context, AdBlockVpnService::class.java).setAction(action)
}
