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
}

data class HomeUiState(
    val query: String = "",
    val items: List<Item> = emptyList(),
    val canAdd: Boolean = false,
)
