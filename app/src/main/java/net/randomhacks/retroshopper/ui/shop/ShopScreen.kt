package net.randomhacks.retroshopper.ui.shop

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
) {
  var showAddStore by rememberSaveable { mutableStateOf(false) }
  var showCheckoutConfirm by rememberSaveable { mutableStateOf(false) }

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

  if (state.stores.isEmpty()) {
    NoStoresYet(onAddStore = { showAddStore = true }, modifier = modifier)
    return
  }

  Column(modifier.fillMaxSize()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      StoreSelector(
          stores = state.stores,
          selected = state.selectedStore,
          onSelectStore = onSelectStore,
          onAddStore = { showAddStore = true },
      )
      TextButton(
          onClick = { showCheckoutConfirm = true },
          enabled = state.inCart.isNotEmpty(),
      ) {
        Text(stringResource(R.string.check_out))
      }
    }
    LazyColumn {
      items(state.toBuy, key = { it.item.id }) { row ->
        ShoppingRow(row = row, onSetInCart = { onSetInCart(row.item.id, it) })
      }
      if (state.inCart.isNotEmpty()) {
        item(key = "in-cart") { SectionHeader(stringResource(R.string.section_in_cart)) }
        items(state.inCart, key = { it.item.id }) { row ->
          ShoppingRow(row = row, onSetInCart = { onSetInCart(row.item.id, it) })
        }
      }
      if (state.unknown.isNotEmpty()) {
        item(key = "unknown") { SectionHeader(stringResource(R.string.section_not_here)) }
        items(state.unknown, key = { it.item.id }) { row -> UnknownRow(row) }
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
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Box {
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

@Composable
private fun ShoppingRow(row: ShoppingListRow, onSetInCart: (Boolean) -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .toggleable(
                  value = row.item.inCart,
                  onValueChange = onSetInCart,
                  role = Role.Checkbox,
              )
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

/** A needed item with no availability record here; dimmed. Details sheet arrives in milestone 5. */
@Composable
private fun UnknownRow(row: ShoppingListRow) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
