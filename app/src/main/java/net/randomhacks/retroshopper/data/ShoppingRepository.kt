package net.randomhacks.retroshopper.data

import kotlinx.coroutines.flow.Flow

/**
 * The application-level data API. Everything outside this package goes through
 * this class rather than touching DAOs, so a future backend swap (e.g.
 * Firestore) stays contained.
 *
 * Rename and duplicate-name checks are check-then-write without a transaction:
 * this is a single-user, on-device app, and the unique indices still make the
 * race harmless (the write fails) rather than corrupting data.
 */
class ShoppingRepository(private val dao: ShoppingDao) {
  // --- Observation ---

  fun observeItems(): Flow<List<Item>> = dao.observeItems()

  fun observeStores(): Flow<List<Store>> = dao.observeStores()

  fun observeShoppingList(storeId: Long): Flow<List<ShoppingListRow>> =
      dao.observeShoppingList(storeId)

  // --- Items ---

  /**
   * Marks the item with this name as needed, creating it first if it doesn't
   * exist (names match case-insensitively). Returns the item's id.
   */
  suspend fun addNeededItem(name: String): Long {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Item name must not be blank" }
    val id = dao.insertItem(Item(name = trimmed, needed = true))
    if (id != -1L) return id
    val existing = checkNotNull(dao.findItemByName(trimmed))
    dao.setNeeded(existing.id, true)
    return existing.id
  }

  suspend fun setNeeded(itemId: Long, needed: Boolean) = dao.setNeeded(itemId, needed)

  suspend fun setInCart(itemId: Long, inCart: Boolean) = dao.setInCart(itemId, inCart)

  /** Returns false (and changes nothing) if another item already has this name. */
  suspend fun renameItem(itemId: Long, newName: String): Boolean {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) return false
    val existing = dao.findItemByName(trimmed)
    if (existing != null && existing.id != itemId) return false
    dao.renameItem(itemId, trimmed)
    return true
  }

  suspend fun deleteItem(itemId: Long) = dao.deleteItem(itemId)

  /** Ends the shopping trip: in-cart items become bought (not needed, not in cart). */
  suspend fun checkOut() = dao.checkOut()

  // --- Stores ---

  /** Returns the new store's id, or null if the name is blank or already taken. */
  suspend fun addStore(name: String): Long? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null
    val id = dao.insertStore(Store(name = trimmed))
    return if (id == -1L) null else id
  }

  /** Returns false (and changes nothing) if another store already has this name. */
  suspend fun renameStore(storeId: Long, newName: String): Boolean {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) return false
    val existing = dao.findStoreByName(trimmed)
    if (existing != null && existing.id != storeId) return false
    dao.renameStore(storeId, trimmed)
    return true
  }

  /** Deletes the store and all its availability/aisle records; items are untouched. */
  suspend fun deleteStore(storeId: Long) = dao.deleteStore(storeId)

  // --- Per-store availability ---

  /** Marks the item available at the store, setting (or updating) its aisle. */
  suspend fun setAvailable(itemId: Long, storeId: Long, aisle: String = "") =
      dao.upsertStoreItem(StoreItem(itemId = itemId, storeId = storeId, aisle = aisle.trim()))

  /** The item is gone from this store; forgets its aisle too. */
  suspend fun removeFromStore(itemId: Long, storeId: Long) = dao.deleteStoreItem(itemId, storeId)
}
