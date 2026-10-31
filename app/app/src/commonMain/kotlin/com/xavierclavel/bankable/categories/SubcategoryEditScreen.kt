package com.xavierclavel.bankable.categories

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.cat_delete_subcategory
import com.xavierclavel.bankable.resources.cat_inherits_parent_color
import com.xavierclavel.bankable.resources.cat_label_parent
import com.xavierclavel.bankable.resources.cat_screen_new_subcategory
import com.xavierclavel.bankable.resources.cd_edit_subcategory
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.dialog_delete_subcategory_message
import com.xavierclavel.bankable.resources.label_name
import com.xavierclavel.bankable.resources.label_select_parent_category
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import org.jetbrains.compose.resources.stringResource

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

    // Subcategories take their parent's color.
    val color = colorHexByName(viewModel.pickerCategory?.color)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_subcategory_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSubcategory(
                        onSuccess = { navController.popBackStack() },
                        onError = { e -> error = e },
                    )
                }) {
                    Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(
                    if (isEditing) Res.string.cd_edit_subcategory else Res.string.cat_screen_new_subcategory
                ),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
        bottomBar = {
            EditActions(
                error = error,
                onSave = {
                    viewModel.saveSubcategory(
                        name    = title,
                        onSuccess = { navController.popBackStack() },
                        onError   = { e -> error = e },
                    )
                },
                deleteText = if (isEditing) stringResource(Res.string.cat_delete_subcategory) else null,
                onDelete = { showDeleteDialog = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditPreviewTile(
                icon = { IconTile(icon = iconByName(viewModel.selectedIcon), color = color, size = 108.dp, iconSize = 52.dp) },
                caption = stringResource(Res.string.cat_inherits_parent_color),
            )

            NightTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(Res.string.label_name),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.titleMedium,
            )

            // Visual category selector
            CategorySelectorRow(
                category = viewModel.pickerCategory,
                onClick  = { showCategoryPicker = true },
            )

            IconPickerRow(
                iconName = viewModel.selectedIcon,
                tint     = color,
                onClick  = { navController.navigate("icon/picker") },
            )
        }
    }

    // Category picker modal
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = NightBackground,
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
    FormRow(label = stringResource(Res.string.cat_label_parent), onClick = onClick) {
        if (category != null) {
            Icon(
                imageVector = iconByName(category.icon),
                contentDescription = null,
                tint     = colorHexByName(category.color),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
        }
        FormValueText(
            text = category?.name ?: stringResource(Res.string.label_select_parent_category),
            placeholder = category == null,
        )
    }
}

@Composable
private fun CategoryPickerSheetContent(
    categories: List<CategoryOut>,
    onSelect: (CategoryOut) -> Unit,
) {
    LazyColumn(
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories) { cat ->
            NightCard(
                modifier = Modifier.fillMaxWidth(),
                onClick  = { onSelect(cat) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = iconByName(cat.icon), color = colorHexByName(cat.color), size = 40.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NightTextPrimary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
