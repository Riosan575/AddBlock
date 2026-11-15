package bendev.addblock.core.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bendev.addblock.AddBlockApp

/** Builds a ViewModel from the [AppContainer]'s use cases. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: AppContainer.() -> VM): VM {
    val container = (LocalContext.current.applicationContext as AddBlockApp).container
    return viewModel(factory = viewModelFactory { initializer { container.create() } })
}
