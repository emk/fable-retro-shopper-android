package net.randomhacks.retroshopper.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.randomhacks.retroshopper.data.Item
import net.randomhacks.retroshopper.data.ShoppingRepository

class HomeViewModel(private val repository: ShoppingRepository) : ViewModel() {
  private val query = MutableStateFlow("")

  val uiState: StateFlow<HomeUiState> =
      combine(repository.observeItems(), query) { items, query ->
            val trimmed = query.trim()
            HomeUiState(
                query = query,
                items =
                    if (trimmed.isEmpty()) items
                    else items.filter { it.name.contains(trimmed, ignoreCase = true) },
                allItems = items,
                // Offer "Add" unless the query names an existing item exactly.
                canAdd =
                    trimmed.isNotEmpty() && items.none { it.name.equals(trimmed, ignoreCase = true) },
            )
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

  fun setQuery(value: String) {
    query.value = value
  }

  fun setNeeded(itemId: Long, needed: Boolean) {
    viewModelScope.launch { repository.setNeeded(itemId, needed) }
  }

  /** Adds (or re-marks as needed) the item named by the current query, then clears the query. */
  fun addItem() {
    val name = query.value.trim()
    if (name.isEmpty()) return
    viewModelScope.launch { repository.addNeededItem(name) }
    query.value = ""
  }

  /** The UI validates against existing names first, so a failed rename just no-ops. */
  fun renameItem(itemId: Long, newName: String) {
    viewModelScope.launch { repository.renameItem(itemId, newName) }
  }

  fun deleteItem(itemId: Long) {
    viewModelScope.launch { repository.deleteItem(itemId) }
  }
}

data class HomeUiState(
    val query: String = "",
    /** Items matching the current query — what the list shows. */
    val items: List<Item> = emptyList(),
    /** Every item, ignoring the query; for details-sheet lookup and rename validation. */
    val allItems: List<Item> = emptyList(),
    val canAdd: Boolean = false,
)
