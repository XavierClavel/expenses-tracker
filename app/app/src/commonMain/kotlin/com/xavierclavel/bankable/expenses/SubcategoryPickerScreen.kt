package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.screen_select_category
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

/**
 * Category tree for picking an expense's subcategory. Tapping a category picks its
 * default subcategory; its chevron opens the other subcategories. [selectedId], when
 * given, is the current subcategory: it gets a check and its category starts open.
 */
@Composable
fun SubcategoryPickerScreen(
    selectedType: String,
    onPick: (SubcategoryOut?) -> Unit,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
    selectedId: Int? = null,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val filtered = categories.filter { it.type == selectedType }

    var expandedIds by remember {
        mutableStateOf(
            categories
                .filter { c -> c.subcategories.any { it.id == selectedId && !it.isDefault } }
                .map { it.id }
                .toSet()
        )
    }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.screen_select_category),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.id }) { category ->
                val isExpanded = expandedIds.contains(category.id)
                val defaultSub = category.subcategories.find { it.isDefault }
                val children = category.subcategories.filter { !it.isDefault }
                val accent = colorHexByName(category.color)

                NightCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onPick(defaultSub)
                        navController.popBackStack()
                    },
                    selected = isExpanded,
                    accent = accent,
                    contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(icon = iconByName(category.icon), color = accent, size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NightTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (defaultSub != null && defaultSub.id == selectedId) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        if (children.isNotEmpty()) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isExpanded) NightTextPrimary else NightTextSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        expandedIds = if (isExpanded) expandedIds - category.id
                                        else expandedIds + category.id
                                    }
                                    .padding(8.dp)
                                    .size(22.dp),
                            )
                        }
                    }
                }

                if (isExpanded) {
                    children.forEach { child ->
                        NightCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, top = 6.dp),
                            onClick = {
                                onPick(child)
                                navController.popBackStack()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconTile(
                                    icon = iconByName(child.icon),
                                    color = colorHexByName(child.color),
                                    size = 32.dp,
                                    iconSize = 16.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NightTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (child.id == selectedId) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
