package net.randomhacks.retroshopper.ui.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.randomhacks.retroshopper.R
import net.randomhacks.retroshopper.data.Item
import net.randomhacks.retroshopper.data.ShoppingListRow
import net.randomhacks.retroshopper.data.Store
import net.randomhacks.retroshopper.theme.RetroShopperTheme
import net.randomhacks.retroshopper.ui.AppViewModelProvider
import net.randomhacks.retroshopper.ui.ConfirmDialog
import net.randomhacks.retroshopper.ui.ItemDetailsSheet
import net.randomhacks.retroshopper.ui.RenameDialog
import net.randomhacks.retroshopper.ui.StoreDetails

/** The in-store view: what to buy here, what's in the cart, and checkout. */
@Composable
fun ShopScreen(
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ShopScreen(
      state = state,
      onSelectStore = viewModel::selectStore,
      onAddStore = viewModel::addStore,
      onSetInCart = viewModel::setInCart,
      onCheckOut = viewModel::checkOut,
      onSetNeeded = viewModel::setNeeded,
      onRenameItem = viewModel::renameItem,
      onDeleteItem = viewModel::deleteItem,
      onSetAvailable = viewModel::setAvailable,
      onRemoveFromStore = viewModel::removeFromStore,
      onRenameStore = viewModel::renameStore,
      onDeleteStore = viewModel::deleteStore,
      modifier = modifier,
  )
}

@Composable
internal fun ShopScreen(
    state: ShopUiState,
    onSelectStore: (Long) -> Unit,
    onAddStore: (String) -> Unit,
    onSetInCart: (itemId: Long, inCart: Boolean) -> Unit,
    onCheckOut: () -> Unit,
    modifier: Modifier = Modifier,
    onSetNeeded: (itemId: Long, needed: Boolean) -> Unit = { _, _ -> },
    onRenameItem: (itemId: Long, name: String) -> Unit = { _, _ -> },
    onDeleteItem: (itemId: Long) -> Unit = {},
    onSetAvailable: (itemId: Long, storeId: Long, aisle: String) -> Unit = { _, _, _ -> },
    onRemoveFromStore: (itemId: Long, storeId: Long) -> Unit = { _, _ -> },
    onRenameStore: (storeId: Long, name: String) -> Unit = { _, _ -> },
    onDeleteStore: (storeId: Long) -> Unit = {},
) {
  var showAddStore by rememberSaveable { mutableStateOf(false) }
  var showCheckoutConfirm by rememberSaveable { mutableStateOf(false) }
  var detailsItemId by rememberSaveable { mutableStateOf<Long?>(null) }

  if (showAddStore) {
    AddStoreDialog(
        onConfirm = {
          onAddStore(it)
          showAddStore = false
        },
        onDismiss = { showAddStore = false },
    )
  }
  if (showCheckoutConfirm) {
    CheckoutDialog(
        cartSize = state.inCart.size,
        onConfirm = {
          onCheckOut()
          showCheckoutConfirm = false
        },
        onDismiss = { showCheckoutConfirm = false },
    )
  }

  val selectedStore = state.selectedStore
  val detailsRow =
      (state.toBuy + state.inCart + state.unknown).find { it.item.id == detailsItemId }
  if (detailsRow != null && selectedStore != null) {
    val item = detailsRow.item
    ItemDetailsSheet(
        item = item,
        otherItemNames = state.allItemNames - item.name.lowercase(),
        storeDetails =
            StoreDetails(
                store = selectedStore,
                available = detailsRow.available,
                aisle = detailsRow.aisle ?: "",
            ),
        onSetNeeded = { onSetNeeded(item.id, it) },
        onRename = { onRenameItem(item.id, it) },
        onDelete = { onDeleteItem(item.id) },
        onSetAvailable = { available ->
          if (available) onSetAvailable(item.id, selectedStore.id, detailsRow.aisle ?: "")
          else onRemoveFromStore(item.id, selectedStore.id)
        },
        onSetAisle = { onSetAvailable(item.id, selectedStore.id, it) },
        onDismiss = { detailsItemId = null },
    )
  }

  if (state.stores.isEmpty()) {
    NoStoresYet(onAddStore = { showAddStore = true }, modifier = modifier)
    return
  }

  Column(modifier.fillMaxSize()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      StoreSelector(
          stores = state.stores,
          selected = selectedStore,
          onSelectStore = onSelectStore,
          onAddStore = { showAddStore = true },
          modifier = Modifier.weight(1f),
      )
      TextButton(
          onClick = { showCheckoutConfirm = true },
          enabled = state.inCart.isNotEmpty(),
      ) {
        Text(stringResource(R.string.check_out))
      }
      if (selectedStore != null) {
        StoreMenu(
            store = selectedStore,
            otherStoreNames =
                state.stores.mapTo(mutableSetOf()) { it.name.lowercase() } -
                    selectedStore.name.lowercase(),
            onRenameStore = { onRenameStore(selectedStore.id, it) },
            onDeleteStore = { onDeleteStore(selectedStore.id) },
        )
      }
    }
    LazyColumn {
      items(state.toBuy, key = { it.item.id }) { row ->
        ShoppingRow(
            row = row,
            onSetInCart = { onSetInCart(row.item.id, it) },
            onLongPress = { detailsItemId = row.item.id },
        )
      }
      if (state.inCart.isNotEmpty()) {
        item(key = "in-cart") { SectionHeader(stringResource(R.string.section_in_cart)) }
        items(state.inCart, key = { it.item.id }) { row ->
          ShoppingRow(
              row = row,
              onSetInCart = { onSetInCart(row.item.id, it) },
              onLongPress = { detailsItemId = row.item.id },
          )
        }
      }
      if (state.unknown.isNotEmpty()) {
        item(key = "unknown") { SectionHeader(stringResource(R.string.section_not_here)) }
        items(state.unknown, key = { it.item.id }) { row ->
          UnknownRow(row = row, onClick = { detailsItemId = row.item.id })
        }
      }
    }
  }
}

@Composable
private fun StoreSelector(
    stores: List<Store>,
    selected: Store?,
    onSelectStore: (Long) -> Unit,
    onAddStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Box(modifier) {
    TextButton(onClick = { expanded = true }) {
      Text(selected?.name ?: "", style = MaterialTheme.typography.titleMedium)
      Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      stores.forEach { store ->
        DropdownMenuItem(
            text = { Text(store.name) },
            onClick = {
              onSelectStore(store.id)
              expanded = false
            },
        )
      }
      HorizontalDivider()
      DropdownMenuItem(
          text = { Text(stringResource(R.string.add_store)) },
          leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
          onClick = {
            expanded = false
            onAddStore()
          },
      )
    }
  }
}

/** Overflow menu acting on the currently selected store. */
@Composable
private fun StoreMenu(
    store: Store,
    otherStoreNames: Set<String>,
    onRenameStore: (String) -> Unit,
    onDeleteStore: () -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  var showRename by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

  if (showRename) {
    RenameDialog(
        title = stringResource(R.string.rename_store),
        currentName = store.name,
        takenNames = otherStoreNames,
        onConfirm = {
          onRenameStore(it)
          showRename = false
        },
        onDismiss = { showRename = false },
    )
  }
  if (showDeleteConfirm) {
    ConfirmDialog(
        title = stringResource(R.string.delete_store),
        text = stringResource(R.string.delete_store_confirm, store.name),
        confirmLabel = stringResource(R.string.delete),
        onConfirm = {
          showDeleteConfirm = false
          onDeleteStore()
        },
        onDismiss = { showDeleteConfirm = false },
    )
  }

  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.store_menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
          text = { Text(stringResource(R.string.rename_store)) },
          onClick = {
            expanded = false
            showRename = true
          },
      )
      DropdownMenuItem(
          text = { Text(stringResource(R.string.delete_store)) },
          onClick = {
            expanded = false
            showDeleteConfirm = true
          },
      )
    }
  }
}

@Composable
private fun ShoppingRow(
    row: ShoppingListRow,
    onSetInCart: (Boolean) -> Unit,
    onLongPress: () -> Unit,
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .combinedClickable(
                  onClick = { onSetInCart(!row.item.inCart) },
                  onLongClick = onLongPress,
                  role = Role.Checkbox,
              )
              // combinedClickable drops toggleable's state semantics; restore them.
              .semantics { toggleableState = ToggleableState(row.item.inCart) }
              .padding(horizontal = 16.dp, vertical = 4.dp),
  ) {
    Checkbox(checked = row.item.inCart, onCheckedChange = null)
    Spacer(Modifier.width(16.dp))
    Text(
        row.item.name,
        style = MaterialTheme.typography.bodyLarge,
        textDecoration = if (row.item.inCart) TextDecoration.LineThrough else null,
        modifier = Modifier.weight(1f),
    )
    if (!row.aisle.isNullOrEmpty()) {
      Text(
          stringResource(R.string.aisle, row.aisle),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
      )
    }
  }
}

/** A needed item with no availability record here; dimmed. Tap to fill in the details. */
@Composable
private fun UnknownRow(row: ShoppingListRow, onClick: () -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(
        row.item.name,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
      title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
  )
}

@Composable
private fun NoStoresYet(onAddStore: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = modifier.fillMaxSize().padding(32.dp),
  ) {
    Text(stringResource(R.string.no_stores_yet), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.padding(8.dp))
    Button(onClick = onAddStore) { Text(stringResource(R.string.add_store)) }
  }
}

@Composable
private fun AddStoreDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
  var name by rememberSaveable { mutableStateOf("") }
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.add_store)) },
      text = {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(R.string.store_name)) },
            singleLine = true,
        )
      },
      confirmButton = {
        TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
          Text(stringResource(R.string.add))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
      },
  )
}

@Composable
private fun CheckoutDialog(cartSize: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.check_out_title)) },
      text = { Text(pluralStringResource(R.plurals.check_out_message, cartSize, cartSize)) },
      confirmButton = {
        TextButton(onClick = onConfirm) { Text(stringResource(R.string.check_out)) }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
      },
  )
}

@Preview(showBackground = true)
@Composable
private fun ShopScreenPreview() {
  val store = Store(id = 1, name = "Co-op")
  RetroShopperTheme {
    ShopScreen(
        state =
            ShopUiState(
                stores = listOf(store),
                selectedStore = store,
                toBuy =
                    listOf(
                        ShoppingListRow(Item(1, "Apples", needed = true), "produce", true),
                        ShoppingListRow(Item(2, "Milk", needed = true), "dairy", true),
                    ),
                inCart =
                    listOf(
                        ShoppingListRow(Item(3, "Bread", needed = true, inCart = true), "4", true)
                    ),
                unknown = listOf(ShoppingListRow(Item(4, "Batteries", needed = true), null, false)),
            ),
        onSelectStore = {},
        onAddStore = {},
        onSetInCart = { _, _ -> },
        onCheckOut = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ShopScreenNoStoresPreview() {
  RetroShopperTheme {
    ShopScreen(
        state = ShopUiState(),
        onSelectStore = {},
        onAddStore = {},
        onSetInCart = { _, _ -> },
        onCheckOut = {},
    )
  }
}
