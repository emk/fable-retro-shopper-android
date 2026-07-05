package net.randomhacks.retroshopper

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import net.randomhacks.retroshopper.ui.home.HomeScreen
import net.randomhacks.retroshopper.ui.shop.ShopScreen

private data class Tab(val key: NavKey, val labelRes: Int, val icon: ImageVector)

private val TABS =
    listOf(
        Tab(HomeTab, R.string.tab_home, Icons.Default.Home),
        Tab(ShopTab, R.string.tab_shop, Icons.Default.ShoppingCart),
    )

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(HomeTab)
  val currentKey = backStack.lastOrNull()

  Scaffold(
      bottomBar = {
        NavigationBar {
          TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentKey == tab.key,
                onClick = {
                  if (currentKey != tab.key) {
                    backStack.clear()
                    backStack.add(tab.key)
                  }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
          }
        }
      }
  ) { innerPadding ->
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
              entry<HomeTab> { HomeScreen(Modifier.padding(innerPadding)) }
              entry<ShopTab> { ShopScreen(Modifier.padding(innerPadding)) }
            },
    )
  }
}
