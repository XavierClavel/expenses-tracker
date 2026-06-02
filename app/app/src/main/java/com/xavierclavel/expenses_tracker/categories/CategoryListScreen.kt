package com.xavierclavel.expenses_tracker.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
import com.xavierclavel.expenses_tracker.model.CategoryOut
import com.xavierclavel.expenses_tracker.model.SubcategoryOut
import com.xavierclavel.expenses_tracker.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val categories by viewModel.categories.collectAsState()
    val typeFilter = viewModel.typeFilter
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewCategory()
                navController.navigate("category/edit")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                listOf("EXPENSE", "INCOME").forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = typeFilter == type,
                        onClick = { viewModel.setTypeFilter(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(if (type == "EXPENSE") "Expense" else "Income") },
                    )
                }
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categories.filter { it.type == typeFilter }) { category ->
                        val isExpanded = expandedIds.contains(category.id)
                        val nonDefaultChildren = category.subcategories.filter { !it.isDefault }

                        CategoryRow(
                            category = category,
                            isExpanded = isExpanded,
                            hasChildren = nonDefaultChildren.isNotEmpty(),
                            onToggleExpand = {
                                expandedIds = if (isExpanded)
                                    expandedIds - category.id
                                else
                                    expandedIds + category.id
                            },
                            onClick = {
                                viewModel.prepareEditCategory(category)
                                navController.navigate("category/edit")
                            },
                        )

                        if (isExpanded) {
                            nonDefaultChildren.forEach { child ->
                                SubcategoryRow(
                                    subcategory = child,
                                    onClick = {
                                        viewModel.prepareEditSubcategory(child)
                                        navController.navigate("subcategory/edit")
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

@Composable
private fun CategoryRow(
    category: CategoryOut,
    isExpanded: Boolean,
    hasChildren: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconByName(category.icon),
                contentDescription = null,
                tint = colorHexByName(category.color),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            if (hasChildren) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryRow(
    subcategory: SubcategoryOut,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconByName(subcategory.icon),
                contentDescription = null,
                tint = colorHexByName(subcategory.color),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = subcategory.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

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
