package com.xavierclavel.bankable.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatDecimal
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.cat_count
import com.xavierclavel.bankable.resources.cat_per_month
import com.xavierclavel.bankable.resources.cd_add_category
import com.xavierclavel.bankable.resources.label_expenses
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.nav_categories
import com.xavierclavel.bankable.resources.nav_tags
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.ShareBar
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.MyApplicationTheme
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CategoryListScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val categories by viewModel.categories.collectAsState()
    val typeFilter = viewModel.typeFilter
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }
    val visibleCategories = categories.filter { it.type == typeFilter }
    val meanTotals by viewModel.meanTotals.collectAsState()
    val categoryTotals = remember(visibleCategories, meanTotals) {
        visibleCategories.associate { cat -> cat.id to cat.subcategories.sumOf { meanTotals[it.id] ?: 0.0 } }
    }
    // Null until there is spending to show, so an empty year keeps the plain cards.
    val monthSum = categoryTotals.values.sum().takeIf { it > 0.0 }
    val locale = LocalAppLocale.current
    // Categories are a long-lived list; refresh the means on every visit.
    LaunchedEffect(Unit) { viewModel.loadMeanTotals() }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.nav_categories),
                actions = {
                    CircleIconButton(
                        icon = Icons.Outlined.Sell,
                        contentDescription = stringResource(Res.string.nav_tags),
                        onClick = { navController.navigate("tags") },
                    )
                    CircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.cd_add_category),
                        onClick = {
                            viewModel.prepareNewCategory()
                            navController.navigate("category/edit")
                        },
                    )
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SlidingToggle(
                options = listOf(
                    "EXPENSE" to stringResource(Res.string.label_expenses),
                    "INCOME" to stringResource(Res.string.label_income),
                ),
                selected = typeFilter,
                onSelect = { viewModel.setTypeFilter(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter, vertical = 4.dp),
            )

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Mint)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SectionLabel(
                            text = stringResource(Res.string.cat_count, visibleCategories.size),
                            trailing = monthSum?.let { stringResource(Res.string.cat_per_month, formatEuros(it, locale)) },
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    items(visibleCategories, key = { it.id }) { category ->
                        val isExpanded = expandedIds.contains(category.id)
                        val nonDefaultChildren = category.subcategories.filter { !it.isDefault }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CategoryRow(
                                category = category,
                                total = monthSum?.let { categoryTotals[category.id] ?: 0.0 },
                                share = monthSum?.let { ((categoryTotals[category.id] ?: 0.0) / it).toFloat() } ?: 0f,
                                subcategoryCount = nonDefaultChildren.size,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedIds = if (isExpanded)
                                        expandedIds - category.id
                                    else
                                        expandedIds + category.id
                                },
                                onClick = {
                                    viewModel.prepareViewCategory(category)
                                    navController.navigate("category/view")
                                },
                            )

                            if (isExpanded) {
                                nonDefaultChildren.forEach { child ->
                                    SubcategoryRow(
                                        subcategory = child,
                                        total = monthSum?.let { meanTotals[child.id] ?: 0.0 },
                                        share = monthSum?.let { ((meanTotals[child.id] ?: 0.0) / it).toFloat() } ?: 0f,
                                        onClick = {
                                            viewModel.prepareViewSubcategory(child)
                                            navController.navigate("subcategory/view")
                                        },
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

@Composable
private fun CategoryRow(
    category: CategoryOut,
    total: Double?,
    share: Float,
    subcategoryCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
) {
    val color = colorHexByName(category.color)
    val locale = LocalAppLocale.current
    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = iconByName(category.icon), color = color, size = 40.dp, iconSize = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (total != null) {
                    Spacer(Modifier.height(6.dp))
                    ShareBar(fraction = share, color = color)
                }
            }
            if (total != null) {
                Spacer(Modifier.width(14.dp))
                Text(
                    text = formatEuros(total, locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (total > 0.0) NightTextPrimary else NightTextSecondary,
                )
            }
            if (subcategoryCount > 0) {
                // Tapping the arrow expands the subcategories inline.
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleExpand)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NightTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
internal fun SubcategoryRow(
    subcategory: SubcategoryOut,
    total: Double? = null,
    // Fraction of the list's overall total, on the same scale as the category bars.
    share: Float = 0f,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(start = 28.dp),
    onClick: () -> Unit,
) {
    NightCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = iconByName(subcategory.icon),
                color = colorHexByName(subcategory.color),
                size = 32.dp,
                iconSize = 16.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subcategory.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (total != null) {
                    Spacer(Modifier.height(5.dp))
                    ShareBar(fraction = share, color = colorHexByName(subcategory.color), height = 4.dp)
                }
            }
            if (total != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatEuros(total, LocalAppLocale.current),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (total > 0.0) NightTextPrimary else NightTextSecondary,
                )
                // Lines the amount up with the category amounts, which sit left of the expand arrow.
                Spacer(Modifier.width(36.dp))
            }
        }
    }
}

private fun formatEuros(value: Double, locale: String): String =
    "${platformFormatDecimal(value, locale, 2, 2)} €"

@Preview(showBackground = true)
@Composable
private fun CategoryListScreenPreview() {
    MyApplicationTheme {
        CategoryListScreen(
            viewModel = CategoriesViewModel(),
            navController = rememberNavController(),
        )
    }
}
