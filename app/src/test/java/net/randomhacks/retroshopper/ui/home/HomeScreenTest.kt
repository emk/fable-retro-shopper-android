package net.randomhacks.retroshopper.ui.home

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

  private fun waitForText(text: String) {
    waitFor(hasText(text))
  }

  private fun waitFor(matcher: SemanticsMatcher) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(matcher).fetchSemanticsNodes().size == 1
    }
  }
}
