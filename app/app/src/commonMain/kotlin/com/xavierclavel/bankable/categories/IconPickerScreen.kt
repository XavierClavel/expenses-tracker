package com.xavierclavel.bankable.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconMap
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cat_icon_count
import com.xavierclavel.bankable.resources.cat_screen_select_icon
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.MintBorder
import com.xavierclavel.bankable.ui.theme.MintContainer
import com.xavierclavel.bankable.ui.theme.MyApplicationTheme
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun IconPickerScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val entries = iconMap.entries.toList()
    // Subcategories are drawn in their parent's color; categories in the color being edited.
    val fromSubcategory = navController.previousBackStackEntry?.destination?.route == "subcategory/edit"
    val tint = colorHexByName(
        if (fromSubcategory) viewModel.pickerCategory?.color else viewModel.selectedColor
    )

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.cat_screen_select_icon),
                subtitle = stringResource(Res.string.cat_icon_count, entries.size),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries) { (name, vector) ->
                IconItem(
                    name = name,
                    vector = vector,
                    tint = tint,
                    selected = viewModel.selectedIcon == name,
                    onClick = {
                        viewModel.setSelectedIcon(name)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun IconItem(
    name: String,
    vector: ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MintContainer else NightCard)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) MintBorder else NightBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = vector,
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NightTextPrimary else NightTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPickerScreenPreview() {
    MyApplicationTheme {
        IconPickerScreen(
            viewModel = CategoriesViewModel(),
            navController = rememberNavController(),
        )
    }
}
