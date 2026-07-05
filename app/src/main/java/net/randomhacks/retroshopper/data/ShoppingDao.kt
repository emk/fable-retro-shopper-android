package net.randomhacks.retroshopper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
  // --- Items ---

  @Query("SELECT * FROM Item ORDER BY name") fun observeItems(): Flow<List<Item>>

  /** Name comparison is case-insensitive (NOCASE collation on the column). */
  @Query("SELECT * FROM Item WHERE name = :name LIMIT 1")
  suspend fun findItemByName(name: String): Item?

  /** Returns -1 if an item with this name (case-insensitively) already exists. */
  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertItem(item: Item): Long

  @Query("UPDATE Item SET needed = :needed WHERE id = :id")
  suspend fun setNeeded(id: Long, needed: Boolean)

  @Query("UPDATE Item SET inCart = :inCart WHERE id = :id")
  suspend fun setInCart(id: Long, inCart: Boolean)

  @Query("UPDATE Item SET name = :name WHERE id = :id") suspend fun renameItem(id: Long, name: String)

  @Query("DELETE FROM Item WHERE id = :id") suspend fun deleteItem(id: Long)

  /** Ends a shopping trip: everything in the cart is bought, so no longer needed. */
  @Query("UPDATE Item SET needed = 0, inCart = 0 WHERE inCart != 0") suspend fun checkOut()

  // --- Stores ---

  @Query("SELECT * FROM Store ORDER BY name") fun observeStores(): Flow<List<Store>>

  @Query("SELECT * FROM Store WHERE name = :name LIMIT 1")
  suspend fun findStoreByName(name: String): Store?

  /** Returns -1 if a store with this name (case-insensitively) already exists. */
  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertStore(store: Store): Long

  @Query("UPDATE Store SET name = :name WHERE id = :id")
  suspend fun renameStore(id: Long, name: String)

  @Query("DELETE FROM Store WHERE id = :id") suspend fun deleteStore(id: Long)

  // --- Per-store availability ---

  @Upsert suspend fun upsertStoreItem(storeItem: StoreItem)

  @Query("DELETE FROM StoreItem WHERE itemId = :itemId AND storeId = :storeId")
  suspend fun deleteStoreItem(itemId: Long, storeId: Long)

  @Query(
      """
      SELECT Item.*, StoreItem.aisle AS aisle, StoreItem.itemId IS NOT NULL AS available
      FROM Item
      LEFT JOIN StoreItem ON StoreItem.itemId = Item.id AND StoreItem.storeId = :storeId
      WHERE Item.needed != 0
      ORDER BY Item.name
      """
  )
  fun observeShoppingList(storeId: Long): Flow<List<ShoppingListRow>>
}
