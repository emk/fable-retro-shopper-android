package net.randomhacks.retroshopper.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.randomhacks.retroshopper.data.ShoppingListRow
import net.randomhacks.retroshopper.data.ShoppingRepository
import net.randomhacks.retroshopper.data.Store

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(private val repository: ShoppingRepository) : ViewModel() {
  private val selectedStoreId = MutableStateFlow<Long?>(null)

  val uiState: StateFlow<ShopUiState> =
      combine(repository.observeStores(), repository.observeItems(), selectedStoreId) {
            stores,
            items,
            selectedId ->
            // Fall back to the first store when nothing (or a deleted store) is selected.
            val store = stores.find { it.id == selectedId } ?: stores.firstOrNull()
            Triple(stores, items.mapTo(mutableSetOf()) { it.name.lowercase() }, store)
          }
          .flatMapLatest { (stores, names, store) ->
            if (store == null) {
              flowOf(ShopUiState(stores = stores, allItemNames = names))
            } else {
              repository.observeShoppingList(store.id).map { rows ->
                buildState(stores, names, store, rows)
              }
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

  fun selectStore(storeId: Long) {
    selectedStoreId.value = storeId
  }

  /** Creates the store and switches to it; no-op if the name is blank or taken. */
  fun addStore(name: String) {
    viewModelScope.launch {
      repository.addStore(name)?.let { selectedStoreId.value = it }
    }
  }

  fun setInCart(itemId: Long, inCart: Boolean) {
    viewModelScope.launch { repository.setInCart(itemId, inCart) }
  }

  /** Finishes the trip: everything in the cart stops being needed. */
  fun checkOut() {
    viewModelScope.launch { repository.checkOut() }
  }

  fun setNeeded(itemId: Long, needed: Boolean) {
    viewModelScope.launch { repository.setNeeded(itemId, needed) }
  }

  /** The UI validates against existing names first, so a failed rename just no-ops. */
  fun renameItem(itemId: Long, newName: String) {
    viewModelScope.launch { repository.renameItem(itemId, newName) }
  }

  fun deleteItem(itemId: Long) {
    viewModelScope.launch { repository.deleteItem(itemId) }
  }

  fun setAvailable(itemId: Long, storeId: Long, aisle: String) {
    viewModelScope.launch { repository.setAvailable(itemId, storeId, aisle) }
  }

  fun removeFromStore(itemId: Long, storeId: Long) {
    viewModelScope.launch { repository.removeFromStore(itemId, storeId) }
  }

  /** The UI validates against existing names first, so a failed rename just no-ops. */
  fun renameStore(storeId: Long, newName: String) {
    viewModelScope.launch { repository.renameStore(storeId, newName) }
  }

  fun deleteStore(storeId: Long) {
    viewModelScope.launch { repository.deleteStore(storeId) }
  }

  private fun buildState(
      stores: List<Store>,
      allItemNames: Set<String>,
      store: Store,
      rows: List<ShoppingListRow>,
  ): ShopUiState {
    val (carted, notCarted) = rows.partition { it.item.inCart }
    val (available, unknown) = notCarted.partition { it.available }
    return ShopUiState(
        stores = stores,
        selectedStore = store,
        toBuy = available.sortedWith(byAisle),
        inCart = carted.sortedWith(byAisle),
        unknown = unknown, // already name-sorted by the query
        allItemNames = allItemNames,
    )
  }

  private companion object {
    /**
     * Aisle order, the walking order of the store: numeric aisles compare as
     * numbers ("2" before "10"), others alphabetically, blank/unknown last;
     * name breaks ties. Rows are name-sorted by the query, so the tiebreak
     * only needs a stable sort.
     */
    val byAisle: Comparator<ShoppingListRow> =
        compareBy<ShoppingListRow> { it.aisle.isNullOrEmpty() }
            .thenBy { it.aisle?.toLongOrNull() ?: Long.MAX_VALUE }
            .thenBy { it.aisle?.lowercase() ?: "" }
  }
}

data class ShopUiState(
    val stores: List<Store> = emptyList(),
    val selectedStore: Store? = null,
    /** Needed, available at this store, not yet in the cart. Sorted by aisle. */
    val toBuy: List<ShoppingListRow> = emptyList(),
    /** Picked up during this trip; cleared by checkout. */
    val inCart: List<ShoppingListRow> = emptyList(),
    /** Needed, but with no availability record at this store. Shown dimmed. */
    val unknown: List<ShoppingListRow> = emptyList(),
    /** Lowercased names of every item, for rename validation in the details sheet. */
    val allItemNames: Set<String> = emptySet(),
)
