package bendev.addblock.feature.protection.domain.repository

import bendev.addblock.feature.protection.domain.model.ProtectionState
import kotlinx.coroutines.flow.StateFlow

interface ProtectionRepository {
    val state: StateFlow<ProtectionState>

    /** Requires the VPN permission to have been granted already. */
    fun start()

    fun stop()

    /** True when Android "Private DNS" is set to a hostname, which bypasses the local DNS filter. */
    fun isPrivateDnsStrict(): Boolean
}
