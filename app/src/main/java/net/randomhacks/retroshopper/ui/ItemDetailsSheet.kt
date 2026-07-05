package net.randomhacks.retroshopper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.randomhacks.retroshopper.R
import net.randomhacks.retroshopper.data.Item
import net.randomhacks.retroshopper.data.Store

/** The per-store part of the details sheet; null when opened from the Home tab. */
data class StoreDetails(val store: Store, val available: Boolean, val aisle: String)

/**
 * All low-frequency item actions in one place: rename, needed, delete, and —
 * when opened from a store's list — availability and aisle at that store.
 * Switches and the aisle field write through to the database immediately; only
 * rename and delete sit behind dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsSheet(
    item: Item,
    /** Lowercased names of all *other* items, for live rename validation. */
    otherItemNames: Set<String>,
    storeDetails: StoreDetails?,
    onSetNeeded: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onSetAvailable: (Boolean) -> Unit,
    onSetAisle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
  var showRename by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

  if (showRename) {
    RenameDialog(
        title = stringResource(R.string.rename_item),
        currentName = item.name,
        takenNames = otherItemNames,
        onConfirm = {
          onRename(it)
          showRename = false
        },
        onDismiss = { showRename = false },
    )
  }
  if (showDeleteConfirm) {
    ConfirmDialog(
        title = stringResource(R.string.delete_item),
        text = stringResource(R.string.delete_item_confirm, item.name),
        confirmLabel = stringResource(R.string.delete),
        onConfirm = {
          showDeleteConfirm = false
          onDelete()
        },
        onDismiss = { showDeleteConfirm = false },
    )
  }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(item.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { showRename = true }) {
          Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_item))
        }
      }

      LabeledSwitch(
          label = stringResource(R.string.details_needed),
          checked = item.needed,
          onCheckedChange = onSetNeeded,
      )

      if (storeDetails != null) {
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LabeledSwitch(
            label = stringResource(R.string.available_at, storeDetails.store.name),
            checked = storeDetails.available,
            onCheckedChange = onSetAvailable,
        )
        if (storeDetails.available) {
          // Local echo so typing doesn't fight the database round-trip.
          var aisle by remember(item.id, storeDetails.store.id) {
            mutableStateOf(storeDetails.aisle)
          }
          OutlinedTextField(
              value = aisle,
              onValueChange = {
                aisle = it
                onSetAisle(it)
              },
              label = { Text(stringResource(R.string.aisle_label)) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          )
        }
      }

      HorizontalDivider(Modifier.padding(vertical = 8.dp))
      TextButton(onClick = { showDeleteConfirm = true }) {
        Text(stringResource(R.string.delete_item), color = MaterialTheme.colorScheme.error)
      }
    }
  }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
              .padding(vertical = 8.dp),
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Spacer(Modifier.width(16.dp))
    Switch(checked = checked, onCheckedChange = null)
  }
}

/** Shared by item and store renaming: text field with live taken-name validation. */
@Composable
fun RenameDialog(
    title: String,
    currentName: String,
    takenNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(currentName) }
  val taken = name.trim().lowercase() in takenNames
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title) },
      text = {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            isError = taken,
            supportingText = { if (taken) Text(stringResource(R.string.name_taken)) },
            singleLine = true,
        )
      },
      confirmButton = {
        TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank() && !taken) {
          Text(stringResource(R.string.rename))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
      },
  )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title) },
      text = { Text(text) },
      confirmButton = {
        TextButton(onClick = onConfirm) {
          Text(confirmLabel, color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
      },
  )
}
