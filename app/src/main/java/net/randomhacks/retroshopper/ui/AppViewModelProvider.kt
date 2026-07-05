package net.randomhacks.retroshopper.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.randomhacks.retroshopper.RetroShopperApplication
import net.randomhacks.retroshopper.ui.home.HomeViewModel
import net.randomhacks.retroshopper.ui.shop.ShopViewModel

/** Builds ViewModels with their repository dependency from the [AppContainer]. */
object AppViewModelProvider {
  val Factory = viewModelFactory {
    initializer { HomeViewModel(retroShopperApplication().container.repository) }
    initializer { ShopViewModel(retroShopperApplication().container.repository) }
  }
}

private fun CreationExtras.retroShopperApplication(): RetroShopperApplication =
    this[AndroidViewModelFactory.APPLICATION_KEY] as RetroShopperApplication
