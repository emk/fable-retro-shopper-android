package net.randomhacks.retroshopper.ui.home

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.randomhacks.retroshopper.data.ShoppingDatabase
import net.randomhacks.retroshopper.data.ShoppingRepository
import net.randomhacks.retroshopper.testutil.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the ViewModel against the real repository and database. */
@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var db: ShoppingDatabase
  private lateinit var repo: ShoppingRepository
  private lateinit var viewModel: HomeViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
        Room.inMemoryDatabaseBuilder(context, ShoppingDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
    repo = ShoppingRepository(db.shoppingDao())
    viewModel = HomeViewModel(repo)
  }

  @After
  fun tearDown() {
    db.close()
  }

  private suspend fun awaitItems(vararg names: String): HomeUiState =
      viewModel.uiState.first { state -> state.items.map { it.name } == names.toList() }

  @Test
  fun query_filtersItemsCaseInsensitively() = runTest {
    repo.addNeededItem("Milk")
    repo.addNeededItem("Almond milk")
    repo.addNeededItem("Bread")
    awaitItems("Almond milk", "Bread", "Milk")

    viewModel.setQuery("milk")

    awaitItems("Almond milk", "Milk")
  }

  @Test
  fun canAdd_onlyWhenQueryMatchesNoExistingItemExactly() = runTest {
    repo.addNeededItem("Milk")
    awaitItems("Milk")

    viewModel.setQuery("mil")
    assertTrue(viewModel.uiState.first { it.query == "mil" }.canAdd)

    // Exact (case-insensitive) match: nothing new to add.
    viewModel.setQuery("MILK")
    assertFalse(viewModel.uiState.first { it.query == "MILK" }.canAdd)

    viewModel.setQuery("")
    assertFalse(viewModel.uiState.first { it.query == "" }.canAdd)
  }

  @Test
  fun addItem_createsNeededItemAndClearsQuery() = runTest {
    viewModel.setQuery("  Cheese ")
    viewModel.addItem()

    val state = awaitItems("Cheese")
    assertEquals("", state.query)
    assertTrue(state.items.single().needed)
  }

  @Test
  fun setNeeded_togglesItem() = runTest {
    repo.addNeededItem("Milk")
    val milk = awaitItems("Milk").items.single()

    viewModel.setNeeded(milk.id, false)

    viewModel.uiState.first { it.items.singleOrNull()?.needed == false }
  }
}
