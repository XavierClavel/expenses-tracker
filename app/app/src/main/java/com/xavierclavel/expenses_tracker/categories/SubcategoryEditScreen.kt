package com.xavierclavel.expenses_tracker.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
import com.xavierclavel.expenses_tracker.model.CategoryOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryEditScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val isEditing = viewModel.selectedSubcategory != null
    var title by remember { mutableStateOf(viewModel.selectedSubcategory?.name ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()

    // Only show categories of the same type as the subcategory being edited/created
    val typeFilter = viewModel.selectedSubcategory?.type ?: viewModel.pickerCategory?.type
    val filteredCategories = if (typeFilter != null)
        categories.filter { it.type == typeFilter }
    else
        categories

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm delete") },
            text = { Text("Are you sure you want to delete this subcategory?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSubcategory(
                        onSuccess = { navController.popBackStack() },
                        onError = { e -> error = e },
                    )
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Visual category selector
        CategorySelectorRow(
            category = viewModel.pickerCategory,
            onClick  = { showCategoryPicker = true },
        )

        IconPickerRow(
            iconName = viewModel.selectedIcon,
            onClick  = { navController.navigate("icon/picker") },
        )

        Button(
            onClick = {
                viewModel.saveSubcategory(
                    name    = title,
                    onSuccess = { navController.popBackStack() },
                    onError   = { e -> error = e },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        if (isEditing) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    // Category picker modal
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CategoryPickerSheetContent(
                categories = filteredCategories,
                onSelect   = { cat ->
                    viewModel.setPickerCategory(cat)
                    showCategoryPicker = false
                },
            )
        }
    }
}

@Composable
private fun CategorySelectorRow(
    category: CategoryOut?,
    onClick: () -> Unit,
) {
    Surface(
        modifier       = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape          = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = iconByName(category?.icon),
                contentDescription = null,
                tint     = colorHexByName(category?.color),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text     = category?.name ?: "Select parent category",
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color    = if (category == null) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryPickerSheetContent(
    categories: List<CategoryOut>,
    onSelect: (CategoryOut) -> Unit,
) {
    LazyColumn(
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(categories) { cat ->
            Surface(
                modifier       = Modifier.fillMaxWidth().clickable { onSelect(cat) },
                shape          = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = iconByName(cat.icon),
                        contentDescription = null,
                        tint     = colorHexByName(cat.color),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
