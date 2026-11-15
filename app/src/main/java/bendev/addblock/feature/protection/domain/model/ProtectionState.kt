package bendev.addblock.feature.protection.domain.model

enum class ProtectionStatus { STOPPED, STARTING, RUNNING }

data class ProtectionState(
    val status: ProtectionStatus = ProtectionStatus.STOPPED,
    val error: String? = null,
)
