package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationCategory
import com.example.ui.theme.StarGold

import com.example.ui.GroupByMode

@Composable
fun SearchBarWithFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: NotificationCategory?,
    onCategorySelected: (NotificationCategory?) -> Unit,
    selectedPackage: String?,
    onClearSelectedPackage: () -> Unit,
    onlyStarred: Boolean,
    onToggleOnlyStarred: () -> Unit,
    unreadOnly: Boolean,
    onToggleUnreadOnly: () -> Unit,
    groupByMode: GroupByMode = GroupByMode.DATE,
    onGroupByModeChange: (GroupByMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("notification_search_input"),
            placeholder = {
                Text(
                    "Search notifications by app name, title, or message...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal filter chip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active app filter badge if filtered by specific app
            if (selectedPackage != null) {
                InputChip(
                    selected = true,
                    onClick = onClearSelectedPackage,
                    label = {
                        Text(
                            text = "App: ${selectedPackage.substringAfterLast('.')}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear App filter",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // "All" Chip
            FilterChip(
                selected = selectedCategory == null && !onlyStarred && !unreadOnly && selectedPackage == null,
                onClick = {
                    onCategorySelected(null)
                    if (onlyStarred) onToggleOnlyStarred()
                    if (unreadOnly) onToggleUnreadOnly()
                },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(10.dp)
            )

            // "Group by App" Chip
            FilterChip(
                selected = groupByMode == GroupByMode.APP,
                onClick = {
                    onGroupByModeChange(
                        if (groupByMode == GroupByMode.APP) GroupByMode.DATE else GroupByMode.APP
                    )
                },
                label = { Text("Group by App", style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(10.dp)
            )

            // "⭐ Starred" Chip
            FilterChip(
                selected = onlyStarred,
                onClick = onToggleOnlyStarred,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (onlyStarred) StarGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Starred", style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(10.dp)
            )

            // "Unread" Chip
            FilterChip(
                selected = unreadOnly,
                onClick = onToggleUnreadOnly,
                label = { Text("Unread", style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(10.dp)
            )

            // Categories
            NotificationCategory.values().filter { it != NotificationCategory.ALL }.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (selectedCategory == category) category.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(category.displayName, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
