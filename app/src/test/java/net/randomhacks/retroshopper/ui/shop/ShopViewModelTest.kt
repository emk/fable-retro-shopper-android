package net.randomhacks.retroshopper.ui.shop

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the ViewModel against the real repository and database. */
@RunWith(AndroidJUnit4::class)
class ShopViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var db: ShoppingDatabase
  private lateinit var repo: ShoppingRepository
  private lateinit var viewModel: ShopViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
        Room.inMemoryDatabaseBuilder(context, ShoppingDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
    repo = ShoppingRepository(db.shoppingDao())
    viewModel = ShopViewModel(repo)
  }

  @After
  fun tearDown() {
    db.close()
  }

  private suspend fun awaitState(predicate: (ShopUiState) -> Boolean): ShopUiState =
      viewModel.uiState.first(predicate)

  @Test
  fun noStores_yieldsEmptyState() = runTest {
    val state = awaitState { true }
    assertTrue(state.stores.isEmpty())
    assertNull(state.selectedStore)
  }

  @Test
  fun firstStoreIsSelectedByDefault_andSelectStoreSwitches() = runTest {
    val milk = repo.addNeededItem("Milk")
    val coop = checkNotNull(repo.addStore("Co-op"))
    val hannaford = checkNotNull(repo.addStore("Hannaford"))
    repo.setAvailable(milk, hannaford, aisle = "12")

    // Alphabetically first store ("Co-op") wins by default; Milk is unknown there.
    var state = awaitState { it.selectedStore?.id == coop }
    assertEquals(listOf("Milk"), state.unknown.map { it.item.name })
    assertTrue(state.toBuy.isEmpty())

    viewModel.selectStore(hannaford)
    state = awaitState { it.selectedStore?.id == hannaford }
    assertEquals(listOf("Milk"), state.toBuy.map { it.item.name })
    assertEquals("12", state.toBuy.single().aisle)
  }

  @Test
  fun sectionsPartitionByCartAndAvailability() = runTest {
    val store = checkNotNull(repo.addStore("Co-op"))
    val milk = repo.addNeededItem("Milk")
    val bread = repo.addNeededItem("Bread")
    repo.addNeededItem("Batteries") // no record at this store
    repo.setAvailable(milk, store)
    repo.setAvailable(bread, store)
    repo.setInCart(bread, true)

    val state = awaitState { it.inCart.isNotEmpty() }
    assertEquals(listOf("Milk"), state.toBuy.map { it.item.name })
    assertEquals(listOf("Bread"), state.inCart.map { it.item.name })
    assertEquals(listOf("Batteries"), state.unknown.map { it.item.name })
  }

  @Test
  fun toBuy_sortsNumericAislesNaturallyAndBlankLast() = runTest {
    val store = checkNotNull(repo.addStore("Co-op"))
    repo.setAvailable(repo.addNeededItem("Cereal"), store, aisle = "10")
    repo.setAvailable(repo.addNeededItem("Milk"), store, aisle = "2")
    repo.setAvailable(repo.addNeededItem("Apples"), store, aisle = "produce")
    repo.setAvailable(repo.addNeededItem("Bags"), store) // no aisle

    val state = awaitState { it.toBuy.size == 4 }
    assertEquals(listOf("Milk", "Cereal", "Apples", "Bags"), state.toBuy.map { it.item.name })
  }

  @Test
  fun addStore_createsAndSelectsIt() = runTest {
    checkNotNull(repo.addStore("Co-op"))
    viewModel.addStore("Hannaford")

    val state = awaitState { it.selectedStore?.name == "Hannaford" }
    assertEquals(listOf("Co-op", "Hannaford"), state.stores.map { it.name })
  }

  @Test
  fun checkOut_clearsCartAndRemovesBoughtItemsFromList() = runTest {
    val store = checkNotNull(repo.addStore("Co-op"))
    val milk = repo.addNeededItem("Milk")
    val bread = repo.addNeededItem("Bread")
    repo.setAvailable(milk, store)
    repo.setAvailable(bread, store)
    viewModel.setInCart(bread, true)
    awaitState { it.inCart.isNotEmpty() }

    viewModel.checkOut()

    val state = awaitState { it.inCart.isEmpty() }
    // Bread was bought: no longer needed, gone from every section.
    assertEquals(listOf("Milk"), state.toBuy.map { it.item.name })
    assertTrue(state.unknown.isEmpty())
  }
}
