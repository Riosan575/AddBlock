package bendev.addblock

import android.app.Application
import bendev.addblock.core.di.AppContainer

class AddBlockApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
