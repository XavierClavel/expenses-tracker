package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryPickerScreen(
    expensesViewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val selectedType = expensesViewModel.selectedType
    val filtered = categories.filter { it.type == selectedType }

    var expandedIds by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_select_category)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            items(filtered) { category ->
                val isExpanded = expandedIds.contains(category.id)
                val defaultSub = category.subcategories.find { it.isDefault }
                val children = category.subcategories.filter { !it.isDefault }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            expensesViewModel.setSelectedSubcategory(defaultSub)
                            navController.popBackStack()
                        },
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
                        if (children.isNotEmpty()) {
                            IconButton(onClick = {
                                expandedIds = if (isExpanded)
                                    expandedIds - category.id
                                else
                                    expandedIds + category.id
                            }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }

                if (isExpanded) {
                    Column {
                        children.forEach { child ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, end = 0.dp, top = 2.dp, bottom = 2.dp)
                                    .clickable {
                                        expensesViewModel.setSelectedSubcategory(child)
                                        navController.popBackStack()
                                    },
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
                                        imageVector = iconByName(child.icon),
                                        contentDescription = null,
                                        tint = colorHexByName(child.color),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = child.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 10.dp),
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
