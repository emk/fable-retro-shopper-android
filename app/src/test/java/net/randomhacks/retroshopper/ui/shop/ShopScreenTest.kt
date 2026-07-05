package net.randomhacks.retroshopper.ui.shop

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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

  @Test
  fun unknownItem_canBeMarkedAvailableWithAisleFromDetailsSheet() {
    // Tapping a dimmed row opens the details sheet for this store.
    waitFor(hasText("Batteries"))
    composeRule.onNodeWithText("Batteries").performClick()
    waitFor(hasText("Available at Co-op"))

    // Availability is off (nothing pre-flipped); switching it on reveals the aisle field.
    composeRule.onNodeWithText("Available at Co-op").performClick()
    waitFor(hasSetTextAction())
    composeRule.onNode(hasSetTextAction()).performTextInput("9")

    // The list behind the sheet updates live: Batteries moved out of the unknown section.
    waitFor(hasText("Aisle 9"))
    waitUntilGone(hasText("Not at this store?"))

    // Switching availability back off returns it to the unknown section.
    composeRule.onNodeWithText("Available at Co-op").performClick()
    waitFor(hasText("Not at this store?"))
  }

  @Test
  fun storeMenu_renameAndDelete() {
    waitFor(hasText("Co-op"))
    composeRule.onNodeWithContentDescription("Store options").performClick()
    composeRule.onNodeWithText("Rename store").performClick()

    val dialogField = composeRule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
    dialogField.performTextClearance()
    dialogField.performTextInput("City Market")
    composeRule.onNodeWithText("Rename").performClick()
    waitFor(hasText("City Market"))

    // Deleting the only store lands on the empty state; items survive elsewhere.
    composeRule.onNodeWithContentDescription("Store options").performClick()
    composeRule.onNodeWithText("Delete store").performClick()
    waitFor(hasAnyAncestor(isDialog()) and hasText("Delete"))
    composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    waitFor(hasText("No stores yet. Add the store you shop at to start a list."))
  }

  private fun waitFor(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun waitUntilGone(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }
  }
}
