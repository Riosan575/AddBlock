package bendev.addblock.feature.blocklist.data.remote

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HostsRemoteDataSource {

    fun download(url: String, target: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
    }
}
