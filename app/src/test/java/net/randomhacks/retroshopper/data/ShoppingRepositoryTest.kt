package net.randomhacks.retroshopper.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the repository against a real in-memory Room database (bundled
 * SQLite driver), per the project's mock-free testing strategy. Robolectric
 * supplies the Android Context; SQLite itself is the real engine.
 */
@RunWith(AndroidJUnit4::class)
class ShoppingRepositoryTest {
  private lateinit var db: ShoppingDatabase
  private lateinit var repo: ShoppingRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
        Room.inMemoryDatabaseBuilder(context, ShoppingDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
    repo = ShoppingRepository(db.shoppingDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  // --- Items ---

  @Test
  fun addNeededItem_createsNeededItem() = runTest {
    repo.addNeededItem("Milk")
    val items = repo.observeItems().first()
    assertEquals(listOf("Milk"), items.map { it.name })
    assertTrue(items.single().needed)
  }

  @Test
  fun addNeededItem_existingNameDifferentCase_marksNeededInsteadOfDuplicating() = runTest {
    val id = repo.addNeededItem("Milk")
    repo.setNeeded(id, false)

    val again = repo.addNeededItem("  milk ")

    assertEquals(id, again)
    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals("Milk", items.single().name) // original spelling kept
    assertTrue(items.single().needed)
  }

  @Test
  fun observeItems_sortsAlphabeticallyIgnoringCase() = runTest {
    repo.addNeededItem("bananas")
    repo.addNeededItem("Apples")
    repo.addNeededItem("Cheese")
    assertEquals(
        listOf("Apples", "bananas", "Cheese"),
        repo.observeItems().first().map { it.name },
    )
  }

  @Test
  fun renameItem_rejectsNameOfAnotherItem() = runTest {
    val milk = repo.addNeededItem("Milk")
    repo.addNeededItem("Bread")

    assertFalse(repo.renameItem(milk, "BREAD"))
    assertTrue(repo.renameItem(milk, "Whole milk"))
    assertTrue(repo.renameItem(milk, "WHOLE MILK")) // recasing itself is allowed

    assertEquals(
        listOf("Bread", "WHOLE MILK"),
        repo.observeItems().first().map { it.name },
    )
  }

  @Test
  fun deleteItem_cascadesItsStoreRecords() = runTest {
    val milk = repo.addNeededItem("Milk")
    val store = checkNotNull(repo.addStore("Co-op"))
    repo.setAvailable(milk, store, aisle = "dairy")

    repo.deleteItem(milk)

    assertTrue(repo.observeItems().first().isEmpty())
    assertTrue(repo.observeShoppingList(store).first().isEmpty())
  }

  // --- Shopping list rows ---

  @Test
  fun observeShoppingList_flagsAvailabilityAndAisle_forNeededItemsOnly() = runTest {
    val milk = repo.addNeededItem("Milk")
    val bread = repo.addNeededItem("Bread")
    val notNeeded = repo.addNeededItem("Caviar")
    repo.setNeeded(notNeeded, false)

    val store = checkNotNull(repo.addStore("Co-op"))
    repo.setAvailable(milk, store, aisle = "dairy")

    val rows = repo.observeShoppingList(store).first().associateBy { it.item.id }
    assertEquals(setOf(milk, bread), rows.keys) // Caviar not needed, not listed

    assertTrue(rows.getValue(milk).available)
    assertEquals("dairy", rows.getValue(milk).aisle)
    assertFalse(rows.getValue(bread).available)
    assertNull(rows.getValue(bread).aisle)
  }

  @Test
  fun setAvailable_updatesAisleOnSecondCall() = runTest {
    val milk = repo.addNeededItem("Milk")
    val store = checkNotNull(repo.addStore("Co-op"))

    repo.setAvailable(milk, store, aisle = "3")
    repo.setAvailable(milk, store, aisle = "7")

    assertEquals("7", repo.observeShoppingList(store).first().single().aisle)
  }

  @Test
  fun removeFromStore_dropsAvailabilityButKeepsItem() = runTest {
    val milk = repo.addNeededItem("Milk")
    val store = checkNotNull(repo.addStore("Co-op"))
    repo.setAvailable(milk, store, aisle = "dairy")

    repo.removeFromStore(milk, store)

    val row = repo.observeShoppingList(store).first().single()
    assertFalse(row.available)
    assertNull(row.aisle)
    assertEquals(1, repo.observeItems().first().size)
  }

  // --- Cart and checkout ---

  @Test
  fun checkOut_clearsOnlyCartedItems() = runTest {
    val milk = repo.addNeededItem("Milk")
    val bread = repo.addNeededItem("Bread")
    repo.setInCart(milk, true)

    repo.checkOut()

    val items = repo.observeItems().first().associateBy { it.id }
    assertFalse(items.getValue(milk).needed)
    assertFalse(items.getValue(milk).inCart)
    assertTrue(items.getValue(bread).needed) // untouched
  }

  @Test
  fun setInCart_isReversibleWithoutAffectingNeeded() = runTest {
    val milk = repo.addNeededItem("Milk")
    repo.setInCart(milk, true)
    repo.setInCart(milk, false)

    val item = repo.observeItems().first().single()
    assertTrue(item.needed)
    assertFalse(item.inCart)
  }

  // --- Stores ---

  @Test
  fun addStore_rejectsDuplicateNamesCaseInsensitively() = runTest {
    assertTrue(repo.addStore("Co-op") != null)
    assertNull(repo.addStore("co-OP"))
    assertNull(repo.addStore("   "))
    assertEquals(1, repo.observeStores().first().size)
  }

  @Test
  fun renameStore_rejectsNameOfAnotherStore() = runTest {
    val coop = checkNotNull(repo.addStore("Co-op"))
    repo.addStore("Hannaford")

    assertFalse(repo.renameStore(coop, "hannaford"))
    assertTrue(repo.renameStore(coop, "City Market"))

    assertEquals(
        listOf("City Market", "Hannaford"),
        repo.observeStores().first().map { it.name },
    )
  }

  @Test
  fun deleteStore_cascadesItsRecordsButKeepsItems() = runTest {
    val milk = repo.addNeededItem("Milk")
    val coop = checkNotNull(repo.addStore("Co-op"))
    val hannaford = checkNotNull(repo.addStore("Hannaford"))
    repo.setAvailable(milk, coop, aisle = "dairy")
    repo.setAvailable(milk, hannaford, aisle = "12")

    repo.deleteStore(coop)

    assertEquals(listOf("Hannaford"), repo.observeStores().first().map { it.name })
    assertEquals(1, repo.observeItems().first().size)
    // The other store's aisle info survives.
    assertEquals("12", repo.observeShoppingList(hannaford).first().single().aisle)
  }
}
