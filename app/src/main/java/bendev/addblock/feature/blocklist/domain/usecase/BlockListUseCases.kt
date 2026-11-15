package bendev.addblock.feature.blocklist.domain.usecase

import bendev.addblock.feature.blocklist.domain.repository.BlockListRepository

class ObserveBlockListUseCase(private val repository: BlockListRepository) {
    operator fun invoke() = repository.info
}

class LoadBlockListUseCase(private val repository: BlockListRepository) {
    suspend operator fun invoke() = repository.load()
}

class UpdateBlockListsUseCase(private val repository: BlockListRepository) {
    suspend operator fun invoke(): List<String> = repository.update()
}

class ToggleHostSourceUseCase(private val repository: BlockListRepository) {
    suspend operator fun invoke(id: String, enabled: Boolean) = repository.setSourceEnabled(id, enabled)
}

/** Makes sure there is something to block with: downloads on first run, otherwise loads from disk. */
class EnsureBlockListReadyUseCase(private val repository: BlockListRepository) {
    suspend operator fun invoke() {
        if (repository.hasDownloadedLists()) repository.load() else repository.update()
    }
}
