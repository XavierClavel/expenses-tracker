package com.xavierclavel.bankable.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.bankable.constants.colorByName
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_save
import com.xavierclavel.bankable.resources.cat_delete_category
import com.xavierclavel.bankable.resources.cat_label_color
import com.xavierclavel.bankable.resources.cat_label_icon
import com.xavierclavel.bankable.resources.cat_screen_new_category
import com.xavierclavel.bankable.resources.cat_subcategory_count
import com.xavierclavel.bankable.resources.cd_edit_category
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.dialog_delete_category_message
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_name
import com.xavierclavel.bankable.resources.no_color_selected
import com.xavierclavel.bankable.resources.no_icon_selected
import com.xavierclavel.bankable.ui.DangerButton
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.MyApplicationTheme
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

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
    val color = colorHexByName(viewModel.selectedColor)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_category_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteCategory(
                        onSuccess = { navController.popBackStack("categories", false) },
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
                    if (isEditing) Res.string.cd_edit_category else Res.string.cat_screen_new_category
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
                    viewModel.saveCategory(
                        name = title,
                        type = typeValue,
                        onSuccess = { navController.popBackStack() },
                        onError = { e -> error = e },
                    )
                },
                deleteText = if (isEditing) stringResource(Res.string.cat_delete_category) else null,
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
                caption = viewModel.selectedCategory?.let { cat ->
                    stringResource(Res.string.cat_subcategory_count, cat.subcategories.count { !it.isDefault })
                },
            )

            if (!isEditing) {
                SlidingToggle(
                    options = listOf(
                        "EXPENSE" to stringResource(Res.string.label_expense),
                        "INCOME" to stringResource(Res.string.label_income),
                    ),
                    selected = typeValue,
                    onSelect = { typeValue = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            NightTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(Res.string.label_name),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.titleMedium,
            )

            ColorPickerRow(
                colorName = viewModel.selectedColor,
                onClick = { navController.navigate("color/picker") },
            )

            IconPickerRow(
                iconName = viewModel.selectedIcon,
                tint = color,
                onClick = { navController.navigate("icon/picker") },
            )
        }
    }
}

/** Big centered icon preview with an optional caption, at the top of the edit forms. */
@Composable
internal fun EditPreviewTile(
    icon: @Composable () -> Unit,
    caption: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        if (caption != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Save + optional delete buttons pinned at the bottom of the edit forms. */
@Composable
internal fun EditActions(
    error: String?,
    onSave: () -> Unit,
    deleteText: String?,
    onDelete: () -> Unit,
    saveEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NightBackground)
            .padding(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Danger)
        }
        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = onSave,
            enabled = saveEnabled,
        )
        if (deleteText != null) {
            DangerButton(text = deleteText, onClick = onDelete, icon = Icons.Outlined.Delete)
        }
    }
}

@Composable
internal fun ColorPickerRow(colorName: String?, onClick: () -> Unit) {
    val appColor = colorByName(colorName)
    FormRow(label = stringResource(Res.string.cat_label_color), onClick = onClick) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colorHexByName(colorName)),
        )
        Spacer(Modifier.width(14.dp))
        val colorLabel = appColor?.nameRes?.let { stringResource(it) }
            ?: colorName?.takeIf { it.isNotEmpty() }
            ?: stringResource(Res.string.no_color_selected)
        FormValueText(colorLabel, placeholder = appColor == null && colorName.isNullOrEmpty())
    }
}

@Composable
internal fun IconPickerRow(iconName: String?, tint: Color, onClick: () -> Unit) {
    FormRow(label = stringResource(Res.string.cat_label_icon), onClick = onClick) {
        Icon(
            imageVector = iconByName(iconName),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(14.dp))
        FormValueText(iconName ?: stringResource(Res.string.no_icon_selected), placeholder = iconName == null)
    }
}

@Composable
internal fun FormValueText(text: String, placeholder: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (placeholder) FontWeight.Normal else FontWeight.Medium,
        color = if (placeholder) NightTextTertiary else NightTextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
