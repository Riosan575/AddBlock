package bendev.addblock.feature.allowlist.domain.usecase

import bendev.addblock.feature.allowlist.domain.repository.AllowlistRepository

class ObserveAllowlistUseCase(private val repository: AllowlistRepository) {
    operator fun invoke() = repository.domains
}

class AddToAllowlistUseCase(private val repository: AllowlistRepository) {
    /** Returns false when [input] doesn't look like a domain. */
    operator fun invoke(input: String): Boolean {
        val domain = input.trim().lowercase().removePrefix("*.").trimEnd('.')
        if (domain.isEmpty() || ' ' in domain || '.' !in domain) return false
        repository.add(domain)
        return true
    }
}

class RemoveFromAllowlistUseCase(private val repository: AllowlistRepository) {
    operator fun invoke(domain: String) = repository.remove(domain)
}
