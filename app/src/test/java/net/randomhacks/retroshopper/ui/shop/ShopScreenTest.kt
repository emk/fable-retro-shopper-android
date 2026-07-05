package net.randomhacks.retroshopper.ui.shop

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.randomhacks.retroshopper.data.ShoppingDatabase
import net.randomhacks.retroshopper.data.ShoppingRepository
import net.randomhacks.retroshopper.theme.RetroShopperTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Headless (Robolectric) Compose test of the Shop screen wired to the real
 * ViewModel, repository, and database.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShopScreenTest {
  @get:Rule val composeRule = createComposeRule()

  private lateinit var db: ShoppingDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
        Room.inMemoryDatabaseBuilder(context, ShoppingDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
    val repo = ShoppingRepository(db.shoppingDao())
    runBlocking {
      val store = checkNotNull(repo.addStore("Co-op"))
      val milk = repo.addNeededItem("Milk")
      repo.addNeededItem("Batteries") // stays "Not at this store?"
      repo.setAvailable(milk, store, aisle = "2")
    }
    val viewModel = ShopViewModel(repo)
    composeRule.setContent { RetroShopperTheme { ShopScreen(viewModel = viewModel) } }
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun cartAndCheckoutFlow() {
    // Milk is buyable with its aisle shown; Batteries sit in the unknown section.
    waitFor(hasText("Milk"))
    composeRule.onNodeWithText("Aisle 2").assertExists()
    composeRule.onNodeWithText("Not at this store?").assertExists()
    composeRule.onNodeWithText("Batteries").assertExists()

    // Tapping Milk puts it in the cart.
    composeRule.onNodeWithText("Milk").performClick()
    waitFor(isToggleable() and isOn())
    waitFor(hasText("In cart"))

    // Checkout asks for confirmation, then clears the bought item.
    composeRule.onNodeWithText("Check out").performClick()
    waitFor(hasText("Finish shopping?"))
    composeRule.onNodeWithText("1 item in the cart will be marked as bought.").assertExists()
    // The dialog's confirm button is the second "Check out" node.
    composeRule
        .onNode(hasText("Check out") and hasAnyAncestor(isDialog()))
        .performClick()

    waitUntilGone(hasText("Milk"))
    composeRule.onNodeWithText("Batteries").assertExists() // still needed, still unknown
  }

  private fun waitFor(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().size == 1
    }
  }

  private fun waitUntilGone(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }
  }
}
