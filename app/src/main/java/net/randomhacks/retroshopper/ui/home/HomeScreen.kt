package net.randomhacks.retroshopper.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.randomhacks.retroshopper.R
import net.randomhacks.retroshopper.data.Item
import net.randomhacks.retroshopper.theme.RetroShopperTheme
import net.randomhacks.retroshopper.ui.AppViewModelProvider

/** The at-home view: every known item, with a checkbox marking it as needed. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  HomeScreen(
      state = state,
      onQueryChange = viewModel::setQuery,
      onAdd = viewModel::addItem,
      onToggleNeeded = viewModel::setNeeded,
      modifier = modifier,
  )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggleNeeded: (itemId: Long, needed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize()) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_or_add)) },
        trailingIcon = {
          if (state.query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
            }
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
    LazyColumn {
      if (state.canAdd) {
        item(key = "add") { AddRow(name = state.query.trim(), onClick = onAdd) }
      }
      items(state.items, key = { it.id }) { item ->
        ItemRow(item = item, onToggleNeeded = { onToggleNeeded(item.id, it) })
      }
    }
  }
}

@Composable
private fun ItemRow(item: Item, onToggleNeeded: (Boolean) -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .toggleable(
                  value = item.needed,
                  onValueChange = onToggleNeeded,
                  role = Role.Checkbox,
              )
              .padding(horizontal = 16.dp, vertical = 4.dp),
  ) {
    Checkbox(checked = item.needed, onCheckedChange = null)
    Spacer(Modifier.width(16.dp))
    Text(item.name, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun AddRow(name: String, onClick: () -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(16.dp))
    Text(
        stringResource(R.string.add_item, name),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
  RetroShopperTheme {
    HomeScreen(
        state =
            HomeUiState(
                items =
                    listOf(
                        Item(id = 1, name = "Apples", needed = true),
                        Item(id = 2, name = "Bread"),
                        Item(id = 3, name = "Milk", needed = true),
                    )
            ),
        onQueryChange = {},
        onAdd = {},
        onToggleNeeded = { _, _ -> },
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenAddRowPreview() {
  RetroShopperTheme {
    HomeScreen(
        state =
            HomeUiState(
                query = "Cheese",
                items = listOf(Item(id = 1, name = "Cheese crackers")),
                canAdd = true,
            ),
        onQueryChange = {},
        onAdd = {},
        onToggleNeeded = { _, _ -> },
    )
  }
}
