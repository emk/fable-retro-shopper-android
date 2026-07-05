package net.randomhacks.retroshopper.ui.home

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Headless (Robolectric) Compose test of the Home screen wired to the real
 * ViewModel, repository, and database.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {
  @get:Rule val composeRule = createComposeRule()

  private lateinit var db: ShoppingDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
        Room.inMemoryDatabaseBuilder(context, ShoppingDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
    val viewModel = HomeViewModel(ShoppingRepository(db.shoppingDao()))
    composeRule.setContent { RetroShopperTheme { HomeScreen(viewModel = viewModel) } }
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun addFlow_typeSearchAddAndToggle() {
    // Typing a new name offers an Add row.
    composeRule.onNode(hasSetTextAction()).performTextInput("Milk")
    waitForText("Add “Milk”")

    // Adding creates a needed item and clears the search box.
    composeRule.onNodeWithText("Add “Milk”").performClick()
    waitForText("Milk")
    waitFor(isToggleable() and isOn())

    // Tapping the row unchecks "needed"; tapping again restores it.
    // (Each tap round-trips through the database, so wait rather than assert.)
    composeRule.onNode(isToggleable()).performClick()
    waitFor(isToggleable() and isOff())
    composeRule.onNode(isToggleable()).performClick()
    waitFor(isToggleable() and isOn())
  }

  @Test
  fun detailsSheet_renameWithValidationAndDelete() {
    composeRule.onNode(hasSetTextAction()).performTextInput("Milk")
    waitForText("Add “Milk”")
    composeRule.onNodeWithText("Add “Milk”").performClick()
    composeRule.onNode(hasSetTextAction()).performTextInput("Bread")
    waitForText("Add “Bread”")
    composeRule.onNodeWithText("Add “Bread”").performClick()
    composeRule.onNode(hasSetTextAction()).performTextClearance()
    waitForText("Milk")

    // Long-pressing an item opens its details sheet.
    composeRule.onNodeWithText("Milk").performTouchInput { longClick() }
    waitForText("Delete item")

    // Renaming to another item's name is blocked with an inline error.
    composeRule.onNodeWithContentDescription("Rename item").performClick()
    waitFor(hasSetTextAction() and hasAnyAncestor(isDialog()))
    val dialogField = composeRule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
    dialogField.performTextClearance()
    dialogField.performTextInput("BREAD")
    waitForText("This name is already in use")
    composeRule.onNodeWithText("Rename").assertIsNotEnabled()

    // A fresh name goes through.
    dialogField.performTextClearance()
    dialogField.performTextInput("Oat milk")
    composeRule.onNodeWithText("Rename").performClick()
    waitForText("Oat milk")

    // Delete (with confirmation) removes the item entirely.
    composeRule.onNodeWithText("Delete item").performClick()
    waitForText("Delete “Oat milk” everywhere? Its aisles at every store will be forgotten.")
    composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    waitUntilGone(hasText("Oat milk"))
    composeRule.onNodeWithText("Bread").assertExists()
  }

  private fun waitForText(text: String) {
    waitFor(hasText(text))
  }

  private fun waitUntilGone(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }
  }

  private fun waitFor(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
