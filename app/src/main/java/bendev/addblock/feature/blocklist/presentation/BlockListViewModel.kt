package bendev.addblock.feature.blocklist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bendev.addblock.feature.blocklist.domain.model.BlockListInfo
import bendev.addblock.feature.blocklist.domain.usecase.LoadBlockListUseCase
import bendev.addblock.feature.blocklist.domain.usecase.ObserveBlockListUseCase
import bendev.addblock.feature.blocklist.domain.usecase.ToggleHostSourceUseCase
import bendev.addblock.feature.blocklist.domain.usecase.UpdateBlockListsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlockListViewModel(
    observeBlockList: ObserveBlockListUseCase,
    loadBlockList: LoadBlockListUseCase,
    private val updateBlockLists: UpdateBlockListsUseCase,
    private val toggleHostSource: ToggleHostSourceUseCase,
) : ViewModel() {

    val info: StateFlow<BlockListInfo> = observeBlockList()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { if (info.value.domainCount == 0) loadBlockList() }
    }

    fun update() {
        viewModelScope.launch {
            val failed = updateBlockLists()
            _message.value = if (failed.isEmpty()) {
                "Listas actualizadas"
            } else {
                "No se pudo descargar: ${failed.joinToString()}"
            }
        }
    }

    fun toggle(id: String, enabled: Boolean) {
        viewModelScope.launch { toggleHostSource(id, enabled) }
    }
}
