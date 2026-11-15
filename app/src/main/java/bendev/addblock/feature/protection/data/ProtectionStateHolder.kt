package bendev.addblock.feature.protection.data

import bendev.addblock.feature.protection.domain.model.ProtectionState
import bendev.addblock.feature.protection.domain.model.ProtectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Written by the VPN service, read by the repository. */
class ProtectionStateHolder {

    private val _state = MutableStateFlow(ProtectionState())
    val state: StateFlow<ProtectionState> = _state.asStateFlow()

    fun set(status: ProtectionStatus) {
        _state.value = ProtectionState(status)
    }

    fun fail(message: String?) {
        _state.value = ProtectionState(ProtectionStatus.STOPPED, message ?: "Error desconocido")
    }
}
