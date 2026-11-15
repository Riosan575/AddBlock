package bendev.addblock.feature.blocklist.data.local

import android.content.Context
import bendev.addblock.feature.blocklist.data.HostSourceCatalog
import java.io.File
import java.io.IOException

/** Downloaded hosts files on disk, plus which sources are enabled. */
class HostsLocalDataSource(context: Context) {

    private val prefs = context.getSharedPreferences("blocklist", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "hosts").apply { mkdirs() }

    fun enabledIds(): Set<String> =
        prefs.getStringSet(KEY_SOURCES, null)?.toSet() ?: HostSourceCatalog.defaultIds

    fun setEnabled(id: String, enabled: Boolean) {
        val ids = if (enabled) enabledIds() + id else enabledIds() - id
        prefs.edit().putStringSet(KEY_SOURCES, ids).apply()
    }

    var lastUpdated: Long
        get() = prefs.getLong(KEY_UPDATED, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATED, value).apply()

    fun exists(id: String) = file(id).exists()

    fun tempFile(id: String) = File(dir, "$id.tmp")

    fun commit(id: String) {
        if (!tempFile(id).renameTo(file(id))) throw IOException("No se pudo guardar $id")
    }

    fun forEachLine(id: String, action: (String) -> Unit) {
        val f = file(id)
        if (f.exists()) f.bufferedReader().useLines { lines -> lines.forEach(action) }
    }

    private fun file(id: String) = File(dir, "$id.txt")

    private companion object {
        const val KEY_SOURCES = "sources"
        const val KEY_UPDATED = "last_updated"
    }
}
