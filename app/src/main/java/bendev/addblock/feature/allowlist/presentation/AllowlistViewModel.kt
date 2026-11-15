package bendev.addblock.feature.allowlist.presentation

import androidx.lifecycle.ViewModel
import bendev.addblock.feature.allowlist.domain.usecase.AddToAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.ObserveAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.RemoveFromAllowlistUseCase
import kotlinx.coroutines.flow.StateFlow

class AllowlistViewModel(
    observeAllowlist: ObserveAllowlistUseCase,
    private val addToAllowlist: AddToAllowlistUseCase,
    private val removeFromAllowlist: RemoveFromAllowlistUseCase,
) : ViewModel() {

    val domains: StateFlow<Set<String>> = observeAllowlist()

    /** Returns false when the input isn't a valid domain. */
    fun add(input: String): Boolean = addToAllowlist(input)

    fun remove(domain: String) = removeFromAllowlist(domain)
}
