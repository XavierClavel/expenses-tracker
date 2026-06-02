package com.xavierclavel.expenses_tracker.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
import com.xavierclavel.expenses_tracker.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val isEditing = viewModel.selectedCategory != null
    var title by remember { mutableStateOf(viewModel.selectedCategory?.name ?: "") }
    var typeValue by remember { mutableStateOf(viewModel.selectedCategory?.type ?: "EXPENSE") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm delete") },
            text = { Text("Are you sure you want to delete this category?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteCategory(
                        onSuccess = { navController.popBackStack("categories", false) },
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
        if (!isEditing) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("EXPENSE", "INCOME").forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = typeValue == type,
                        onClick = { typeValue = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(if (type == "EXPENSE") "Expense" else "Income") },
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        ColorPickerRow(
            colorName = viewModel.selectedColor,
            onClick = { navController.navigate("color/picker") },
        )

        IconPickerRow(
            iconName = viewModel.selectedIcon,
            onClick = { navController.navigate("icon/picker") },
        )

        if (isEditing) {
            OutlinedButton(
                onClick = {
                    viewModel.prepareNewSubcategory(viewModel.selectedCategory!!)
                    navController.navigate("subcategory/edit")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New subcategory")
            }
        }

        Button(
            onClick = {
                viewModel.saveCategory(
                    name = title,
                    type = typeValue,
                    onSuccess = { navController.popBackStack() },
                    onError = { e -> error = e },
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
}

@Composable
internal fun ColorPickerRow(colorName: String?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colorHexByName(colorName), CircleShape)
            )
            Text(
                text = colorName ?: "No color selected",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
internal fun IconPickerRow(iconName: String?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = iconByName(iconName),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = iconName ?: "No icon selected",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true, name = "Create")
@Composable
private fun CategoryEditScreenCreatePreview() {
    MyApplicationTheme {
        CategoryEditScreen(
            viewModel = CategoriesViewModel(),
            navController = rememberNavController(),
        )
    }
}
