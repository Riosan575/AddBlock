package bendev.addblock.feature.allowlist.data

import android.content.Context
import bendev.addblock.feature.allowlist.domain.repository.AllowlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllowlistRepositoryImpl(context: Context) : AllowlistRepository {

    private val prefs = context.getSharedPreferences("allowlist", Context.MODE_PRIVATE)

    private val _domains = MutableStateFlow(prefs.getStringSet(KEY, null)?.toSet() ?: emptySet())
    override val domains: StateFlow<Set<String>> = _domains.asStateFlow()

    override fun add(domain: String) = save(_domains.value + domain)

    override fun remove(domain: String) = save(_domains.value - domain)

    private fun save(set: Set<String>) {
        _domains.value = set
        prefs.edit().putStringSet(KEY, set).apply()
    }

    private companion object {
        const val KEY = "domains"
    }
}
