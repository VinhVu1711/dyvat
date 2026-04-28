package com.vinh.dyvat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import kotlinx.coroutines.launch

data class DropdownItem(
    val id: String,
    val label: String,
    val subLabel: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DyvatDropdownSelector(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String,
    itemToId: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Chon...",
    isRequired: Boolean = false,
    errorMessage: String? = null
) where T : Any {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val displayText = selectedItem?.let { itemToString(it) }

    Column(modifier = modifier) {
        Text(
            text = if (isRequired) "$label *" else label,
            color = if (errorMessage != null) MaterialTheme.colorScheme.error else TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidDark)
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText ?: placeholder,
                    color = displayText?.let { TextWhite } ?: TextSilver,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Mo rong",
                    tint = TextSilver,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (expanded) {
        val filteredItems = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                itemToString(item).contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            },
            sheetState = sheetState,
            containerColor = DarkCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = label,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DyvatSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Tim kiem...",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Khong co ket qua",
                            color = TextSilver,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(filteredItems, key = { itemToId(it) }) { item ->
                            val isSelected = selectedItem?.let { itemToId(it) == itemToId(item) } ?: false

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SpotifyGreen.copy(alpha = 0.1f) else MidDark)
                                    .clickable {
                                        onItemSelected(item)
                                        scope.launch {
                                            sheetState.hide()
                                        }
                                        expanded = false
                                        searchQuery = ""
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemToString(item),
                                        color = if (isSelected) SpotifyGreen else TextWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Da chon",
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FormDropdownSelector(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String,
    itemToId: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Chon...",
    isRequired: Boolean = false,
    errorMessage: String? = null
) where T : Any {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = if (isRequired) "$label *" else label,
            color = if (errorMessage != null) MaterialTheme.colorScheme.error else TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (items.isNotEmpty()) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedItem?.let { itemToString(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = if (items.isEmpty()) "Dang tai..." else placeholder,
                        color = if (items.isEmpty()) TextSilver.copy(alpha = 0.5f) else TextSilver
                    )
                },
                trailingIcon = {
                    if (items.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = MidDark,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedContainerColor = MidDark,
                    unfocusedContainerColor = MidDark,
                    cursorColor = SpotifyGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (items.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    items.forEach { item ->
                        val isSelected = selectedItem?.let { itemToId(it) == itemToId(item) } ?: false
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = itemToString(item),
                                    color = if (isSelected) SpotifyGreen else TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Da chon",
                                        tint = SpotifyGreen
                                    )
                                }
                            } else null,
                            colors = MenuDefaults.itemColors(
                                textColor = TextWhite,
                                disabledTextColor = TextSilver
                            )
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SimpleDropdownSelector(
    label: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Chon...",
    isRequired: Boolean = false,
    errorMessage: String? = null
) {
    DyvatDropdownSelector(
        label = label,
        items = items,
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        itemToString = { it },
        itemToId = { it },
        modifier = modifier,
        placeholder = placeholder,
        isRequired = isRequired,
        errorMessage = errorMessage
    )
}
